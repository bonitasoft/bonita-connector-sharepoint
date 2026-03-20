package com.bonitasoft.connectors.sharepoint;

import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.drives.item.items.item.createuploadsession.CreateUploadSessionPostRequestBody;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.FieldValueSet;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.models.ListItem;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.models.odataerrors.ODataError;

import com.microsoft.kiota.serialization.UntypedArray;
import com.microsoft.kiota.serialization.UntypedBoolean;
import com.microsoft.kiota.serialization.UntypedDouble;
import com.microsoft.kiota.serialization.UntypedFloat;
import com.microsoft.kiota.serialization.UntypedInteger;
import com.microsoft.kiota.serialization.UntypedLong;
import com.microsoft.kiota.serialization.UntypedNode;
import com.microsoft.kiota.serialization.UntypedNull;
import com.microsoft.kiota.serialization.UntypedObject;
import com.microsoft.kiota.serialization.UntypedString;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API client facade for SharePoint connector using Microsoft Graph Java SDK v6.
 * Handles authentication via Azure Identity (Client Secret or Certificate credentials)
 * and provides one public method per connector operation.
 */
@Slf4j
public class SharePointClient {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final int SIMPLE_UPLOAD_MAX_BYTES = 4 * 1024 * 1024; // 4 MB
    private static final int UPLOAD_CHUNK_SIZE = 320 * 1024 * 10; // 3.2 MB chunks (must be multiple of 320 KiB)

    private final SharePointConfiguration configuration;
    private final GraphServiceClient graphClient;
    private final RetryPolicy retryPolicy;

    /** Lazily resolved default drive ID for the site. */
    private volatile String defaultDriveId;

    public SharePointClient(SharePointConfiguration configuration) throws SharePointException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());

        try {
            this.graphClient = createGraphClient(configuration);
            log.debug("SharePointClient initialized for site {}", configuration.getSiteId());
        } catch (Exception e) {
            throw new SharePointException("Failed to initialize Graph client: " + e.getMessage(), e);
        }
    }

    // ========================= Public Operation Methods =========================

    /**
     * Upload a file to SharePoint document library.
     * Uses simple PUT for files <= 4MB, resumable upload session for larger files.
     */
    public UploadFileResult uploadFile(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String driveId = resolveDriveId(config.getDriveId());
                byte[] fileBytes = Base64.getDecoder().decode(config.getFileContentBase64());
                String fileName = config.getFileName();
                String parentId = config.getParentItemId() != null ? config.getParentItemId() : "root";

                DriveItem result;
                if (fileBytes.length <= SIMPLE_UPLOAD_MAX_BYTES) {
                    result = simpleUpload(driveId, parentId, fileName, fileBytes, config.getConflictBehavior());
                } else {
                    result = resumableUpload(driveId, parentId, fileName, fileBytes, config.getConflictBehavior());
                }

                log.info("Uploaded file '{}' (id={}, size={})", fileName, result.getId(), fileBytes.length);
                return new UploadFileResult(result.getId(), result.getWebUrl(), result.getETag());
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("uploadFile", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to upload file: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Download a file from SharePoint by item ID.
     * Returns the file content as Base64, plus metadata.
     */
    public DownloadFileResult downloadFile(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String driveId = resolveDriveId(config.getDriveId());
                String itemId = config.getItemId();

                // Get item metadata first
                DriveItem item = graphClient.drives().byDriveId(driveId)
                        .items().byDriveItemId(itemId)
                        .get();

                // Download content
                InputStream contentStream = graphClient.drives().byDriveId(driveId)
                        .items().byDriveItemId(itemId)
                        .content()
                        .get();

                byte[] fileBytes = readAllBytes(contentStream);
                String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                String fileName = item.getName();
                String mimeType = item.getFile() != null ? item.getFile().getMimeType() : "application/octet-stream";
                Long fileSize = item.getSize();

                log.info("Downloaded file '{}' (id={}, size={})", fileName, itemId, fileSize);
                return new DownloadFileResult(base64Content, fileName, mimeType, fileSize);
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("downloadFile", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to download file: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create a folder in the SharePoint document library.
     */
    public CreateFolderResult createFolder(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String driveId = resolveDriveId(config.getDriveId());
                String parentId = config.getParentItemId() != null ? config.getParentItemId() : "root";

                DriveItem folderItem = new DriveItem();
                folderItem.setName(config.getFolderName());
                folderItem.setFolder(new Folder());

                // Set conflict behavior if specified
                if (config.getConflictBehavior() != null) {
                    Map<String, Object> additionalData = new HashMap<>();
                    additionalData.put("@microsoft.graph.conflictBehavior", config.getConflictBehavior());
                    folderItem.setAdditionalData(additionalData);
                }

                DriveItem result = graphClient.drives().byDriveId(driveId)
                        .items().byDriveItemId(parentId)
                        .children()
                        .post(folderItem);

                log.info("Created folder '{}' (id={})", config.getFolderName(), result.getId());
                return new CreateFolderResult(result.getId(), result.getWebUrl());
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("createFolder", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to create folder: " + e.getMessage(), e);
            }
        });
    }

    /**
     * List children (files and/or folders) of a drive item.
     */
    public ListChildrenResult listChildren(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String driveId = resolveDriveId(config.getDriveId());
                String parentId = config.getParentItemId() != null ? config.getParentItemId() : "root";

                DriveItemCollectionResponse response = graphClient.drives().byDriveId(driveId)
                        .items().byDriveItemId(parentId)
                        .children()
                        .get(requestConfig -> {
                            requestConfig.queryParameters.top = config.getMaxResults();
                        });

                List<Map<String, Object>> items = new ArrayList<>();
                if (response != null && response.getValue() != null) {
                    for (DriveItem item : response.getValue()) {
                        boolean isFolder = item.getFolder() != null;
                        boolean isFile = item.getFile() != null;

                        // Filter based on includeFiles/includeFolders
                        if (isFile && !config.isIncludeFiles()) continue;
                        if (isFolder && !config.isIncludeFolders()) continue;

                        Map<String, Object> itemMap = new LinkedHashMap<>();
                        itemMap.put("id", item.getId());
                        itemMap.put("name", item.getName());
                        itemMap.put("webUrl", item.getWebUrl());
                        itemMap.put("size", item.getSize());
                        itemMap.put("isFolder", isFolder);
                        itemMap.put("isFile", isFile);
                        if (item.getLastModifiedDateTime() != null) {
                            itemMap.put("lastModifiedDateTime", item.getLastModifiedDateTime().toString());
                        }
                        if (item.getCreatedDateTime() != null) {
                            itemMap.put("createdDateTime", item.getCreatedDateTime().toString());
                        }
                        if (isFile && item.getFile() != null) {
                            itemMap.put("mimeType", item.getFile().getMimeType());
                        }
                        if (isFolder && item.getFolder() != null) {
                            itemMap.put("childCount", item.getFolder().getChildCount());
                        }
                        items.add(itemMap);
                    }
                }

                String nextPageToken = (response != null) ? response.getOdataNextLink() : null;
                log.info("Listed {} children under item '{}'", items.size(), parentId);
                return new ListChildrenResult(items, items.size(), nextPageToken);
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("listChildren", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to list children: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Delete a drive item (file or folder).
     * 404 on delete is treated as success (idempotent).
     */
    public DeleteItemResult deleteItem(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String driveId = resolveDriveId(config.getDriveId());
                String itemId = config.getItemId();

                graphClient.drives().byDriveId(driveId)
                        .items().byDriveItemId(itemId)
                        .delete();

                log.info("Deleted item '{}' (permanent={})", itemId, config.isPermanent());
                return new DeleteItemResult(itemId, config.isPermanent());
            } catch (ODataError e) {
                // 404 on delete = item already gone, treat as success (idempotent)
                int statusCode = extractStatusCode(e);
                if (statusCode == 404) {
                    log.info("Item '{}' not found (already deleted), treating as success", config.getItemId());
                    return new DeleteItemResult(config.getItemId(), config.isPermanent());
                }
                throw translateODataError("deleteItem", e);
            } catch (SharePointException e) {
                throw e;
            } catch (Exception e) {
                throw new SharePointException("Failed to delete item: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create a new list item in a SharePoint list.
     */
    public CreateListItemResult createListItem(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String siteId = config.getSiteId();
                String listId = config.getListId();

                ListItem listItem = new ListItem();
                FieldValueSet fieldValueSet = new FieldValueSet();

                // Parse JSON fields and set them
                if (config.getFields() != null && !config.getFields().isBlank()) {
                    Map<String, Object> fieldsMap = parseJsonFields(config.getFields());
                    fieldValueSet.setAdditionalData(fieldsMap);
                }
                listItem.setFields(fieldValueSet);

                ListItem result = graphClient.sites().bySiteId(siteId)
                        .lists().byListId(listId)
                        .items()
                        .post(listItem);

                String itemId = result.getId();
                String webUrl = result.getWebUrl();
                String createdDateTime = result.getCreatedDateTime() != null
                        ? result.getCreatedDateTime().toString() : null;

                log.info("Created list item '{}' in list '{}'", itemId, listId);
                return new CreateListItemResult(itemId, webUrl, createdDateTime);
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("createListItem", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to create list item: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get a list item by ID from a SharePoint list.
     */
    public GetListItemResult getListItem(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String siteId = config.getSiteId();
                String listId = config.getListId();
                String listItemId = config.getListItemId();

                ListItem result = graphClient.sites().bySiteId(siteId)
                        .lists().byListId(listId)
                        .items().byListItemId(listItemId)
                        .get(requestConfig -> {
                            requestConfig.queryParameters.expand = new String[]{"fields"};
                            if (config.getSelectFields() != null && !config.getSelectFields().isBlank()) {
                                requestConfig.queryParameters.select = new String[]{config.getSelectFields()};
                            }
                        });

                Map<String, Object> fieldsMap = new LinkedHashMap<>();
                if (result.getFields() != null && result.getFields().getAdditionalData() != null) {
                    fieldsMap = convertUntypedNodes(result.getFields().getAdditionalData());
                }

                String createdDateTime = result.getCreatedDateTime() != null
                        ? result.getCreatedDateTime().toString() : null;
                String lastModifiedDateTime = result.getLastModifiedDateTime() != null
                        ? result.getLastModifiedDateTime().toString() : null;

                log.info("Retrieved list item '{}' from list '{}'", listItemId, listId);
                return new GetListItemResult(fieldsMap, result.getId(), createdDateTime, lastModifiedDateTime);
            } catch (ODataError e) {
                throw translateODataError("getListItem", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to get list item: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update an existing list item in a SharePoint list.
     */
    public UpdateListItemResult updateListItem(SharePointConfiguration config) throws SharePointException {
        return retryPolicy.execute(() -> {
            try {
                String siteId = config.getSiteId();
                String listId = config.getListId();
                String listItemId = config.getListItemId();

                FieldValueSet fieldValueSet = new FieldValueSet();
                if (config.getFields() != null && !config.getFields().isBlank()) {
                    Map<String, Object> fieldsMap = parseJsonFields(config.getFields());
                    fieldValueSet.setAdditionalData(fieldsMap);
                }

                FieldValueSet result = graphClient.sites().bySiteId(siteId)
                        .lists().byListId(listId)
                        .items().byListItemId(listItemId)
                        .fields()
                        .patch(fieldValueSet);

                // Re-fetch to get lastModifiedDateTime
                ListItem updatedItem = graphClient.sites().bySiteId(siteId)
                        .lists().byListId(listId)
                        .items().byListItemId(listItemId)
                        .get();

                String lastModifiedDateTime = updatedItem.getLastModifiedDateTime() != null
                        ? updatedItem.getLastModifiedDateTime().toString() : null;

                log.info("Updated list item '{}' in list '{}'", listItemId, listId);
                return new UpdateListItemResult(listItemId, lastModifiedDateTime);
            } catch (SharePointException e) {
                throw e;
            } catch (ODataError e) {
                throw translateODataError("updateListItem", e);
            } catch (Exception e) {
                throw new SharePointException("Failed to update list item: " + e.getMessage(), e);
            }
        });
    }

    // ========================= Private Helpers =========================

    /**
     * Create the GraphServiceClient using either client secret or certificate credentials.
     */
    private GraphServiceClient createGraphClient(SharePointConfiguration config) {
        String tenantId = config.getTenantId();
        String clientId = config.getClientId();

        if (config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
            var credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(config.getClientSecret())
                    .build();
            return new GraphServiceClient(credential, GRAPH_SCOPE);
        } else {
            // Certificate-based auth: write PEM to temp file for Azure Identity SDK
            try {
                Path tempPem = Files.createTempFile("sharepoint-cert-", ".pem");
                Files.writeString(tempPem, config.getClientCertificatePem(), StandardCharsets.UTF_8);
                tempPem.toFile().deleteOnExit();

                var credential = new ClientCertificateCredentialBuilder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .pemCertificate(tempPem.toString())
                        .build();
                return new GraphServiceClient(credential, GRAPH_SCOPE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write certificate PEM to temp file", e);
            }
        }
    }

    /**
     * Resolve the drive ID to use. If a specific driveId is given, use it.
     * Otherwise, lazily resolve the default drive for the site.
     */
    private String resolveDriveId(String configDriveId) throws SharePointException {
        if (configDriveId != null && !configDriveId.isBlank()) {
            return configDriveId;
        }
        if (defaultDriveId == null) {
            synchronized (this) {
                if (defaultDriveId == null) {
                    try {
                        var drive = graphClient.sites()
                                .bySiteId(configuration.getSiteId())
                                .drive()
                                .get();
                        defaultDriveId = drive.getId();
                        log.debug("Resolved default drive ID: {}", defaultDriveId);
                    } catch (ODataError e) {
                        throw translateODataError("resolveDriveId", e);
                    } catch (Exception e) {
                        throw new SharePointException("Failed to resolve default drive: " + e.getMessage(), e);
                    }
                }
            }
        }
        return defaultDriveId;
    }

    /**
     * Simple upload (PUT) for files <= 4MB.
     */
    private DriveItem simpleUpload(String driveId, String parentId, String fileName,
                                    byte[] fileBytes, String conflictBehavior) {
        String itemPath = fileName;
        if (conflictBehavior != null) {
            itemPath = fileName + "?@microsoft.graph.conflictBehavior=" + conflictBehavior;
        }

        // Use content PUT via parent:/filename:/content
        return graphClient.drives().byDriveId(driveId)
                .items().byDriveItemId(parentId + ":/" + fileName + ":")
                .content()
                .put(new ByteArrayInputStream(fileBytes));
    }

    /**
     * Resumable upload session for files > 4MB.
     * Uses the Microsoft Graph SDK's upload session support.
     */
    private DriveItem resumableUpload(String driveId, String parentId, String fileName,
                                       byte[] fileBytes, String conflictBehavior)
            throws SharePointException {
        try {
            // Create upload session
            CreateUploadSessionPostRequestBody sessionBody = new CreateUploadSessionPostRequestBody();
            DriveItemUploadableProperties props = new DriveItemUploadableProperties();
            if (conflictBehavior != null) {
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("@microsoft.graph.conflictBehavior", conflictBehavior);
                props.setAdditionalData(additionalData);
            }
            sessionBody.setItem(props);

            UploadSession uploadSession = graphClient.drives().byDriveId(driveId)
                    .items().byDriveItemId(parentId + ":/" + fileName + ":")
                    .createUploadSession()
                    .post(sessionBody);

            // Upload in chunks
            String uploadUrl = uploadSession.getUploadUrl();
            long fileSize = fileBytes.length;
            int offset = 0;

            DriveItem result = null;
            while (offset < fileSize) {
                int chunkEnd = (int) Math.min(offset + UPLOAD_CHUNK_SIZE, fileSize);
                int chunkLength = chunkEnd - offset;
                byte[] chunk = new byte[chunkLength];
                System.arraycopy(fileBytes, offset, chunk, 0, chunkLength);

                // PUT chunk to upload URL with Content-Range header
                // For the last chunk, the Graph SDK returns the completed DriveItem
                var httpClient = java.net.http.HttpClient.newBuilder().build();
                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(uploadUrl))
                        .header("Content-Length", String.valueOf(chunkLength))
                        .header("Content-Range",
                                String.format("bytes %d-%d/%d", offset, chunkEnd - 1, fileSize))
                        .PUT(java.net.http.HttpRequest.BodyPublishers.ofByteArray(chunk))
                        .build();

                var response = httpClient.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    // Upload complete - parse DriveItem from response
                    // Re-fetch the item to get full metadata
                    String itemPath = parentId + ":/" + fileName + ":";
                    result = graphClient.drives().byDriveId(driveId)
                            .items().byDriveItemId(itemPath)
                            .get();
                    break;
                } else if (response.statusCode() == 202) {
                    // More chunks needed
                    log.debug("Uploaded chunk {}-{}/{}", offset, chunkEnd - 1, fileSize);
                } else {
                    throw new SharePointException(
                            "Upload chunk failed with status " + response.statusCode()
                                    + ": " + response.body(),
                            response.statusCode(),
                            RetryPolicy.isRetryableStatusCode(response.statusCode()));
                }

                offset = chunkEnd;
            }

            if (result == null) {
                throw new SharePointException("Resumable upload completed but no DriveItem returned");
            }
            return result;
        } catch (SharePointException e) {
            throw e;
        } catch (Exception e) {
            throw new SharePointException("Failed during resumable upload: " + e.getMessage(), e);
        }
    }

    /**
     * Translate an ODataError to a SharePointException with status code and retryable flag.
     */
    private SharePointException translateODataError(String operation, ODataError error) {
        int statusCode = extractStatusCode(error);
        String errorCode = error.getError() != null ? error.getError().getCode() : "unknown";
        String errorMessage = error.getError() != null ? error.getError().getMessage() : error.getMessage();

        boolean retryable = RetryPolicy.isRetryableStatusCode(statusCode);

        // Special handling for 423 resourceLocked
        if (statusCode == 423 || "resourceLocked".equals(errorCode)) {
            retryable = true;
        }

        String message = String.format("%s failed [%d %s]: %s", operation, statusCode, errorCode, errorMessage);
        return new SharePointException(message, statusCode, retryable, error);
    }

    /**
     * Extract HTTP status code from an ODataError.
     */
    private int extractStatusCode(ODataError error) {
        return error.getResponseStatusCode();
    }

    /**
     * Parse a JSON string into a Map of field name to value.
     * Uses a simple recursive JSON parser to avoid external dependencies.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonFields(String json) throws SharePointException {
        try {
            // Use a minimal JSON parser: the Graph SDK already has com.google.gson or
            // we can use the built-in Kiota JSON support. For simplicity, we parse manually.
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new IllegalArgumentException("Fields must be a JSON object");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            // Remove outer braces
            String inner = json.substring(1, json.length() - 1).trim();
            if (inner.isEmpty()) {
                return result;
            }

            // Simple tokenizer for flat JSON objects (handles nested values as strings)
            int pos = 0;
            while (pos < inner.length()) {
                // Skip whitespace and commas
                while (pos < inner.length() && (inner.charAt(pos) == ',' || Character.isWhitespace(inner.charAt(pos)))) {
                    pos++;
                }
                if (pos >= inner.length()) break;

                // Parse key
                String key = parseJsonString(inner, pos);
                pos += key.length() + 2; // +2 for quotes
                // Skip to colon
                while (pos < inner.length() && inner.charAt(pos) != ':') pos++;
                pos++; // skip colon
                while (pos < inner.length() && Character.isWhitespace(inner.charAt(pos))) pos++;

                // Parse value
                Object value;
                if (inner.charAt(pos) == '"') {
                    String strVal = parseJsonString(inner, pos);
                    pos += strVal.length() + 2;
                    value = strVal;
                } else if (inner.charAt(pos) == 't' || inner.charAt(pos) == 'f') {
                    boolean boolVal = inner.charAt(pos) == 't';
                    pos += boolVal ? 4 : 5;
                    value = boolVal;
                } else if (inner.charAt(pos) == 'n') {
                    pos += 4;
                    value = null;
                } else {
                    // Number
                    int start = pos;
                    while (pos < inner.length() && inner.charAt(pos) != ',' && inner.charAt(pos) != '}' && !Character.isWhitespace(inner.charAt(pos))) {
                        pos++;
                    }
                    String numStr = inner.substring(start, pos);
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                }

                result.put(key, value);
            }
            return result;
        } catch (Exception e) {
            throw new SharePointException("Failed to parse fields JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a JSON string starting at pos (expects opening quote at pos).
     */
    private String parseJsonString(String json, int pos) {
        if (json.charAt(pos) != '"') {
            throw new IllegalArgumentException("Expected '\"' at position " + pos);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = pos + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"' || next == '\\' || next == '/') {
                    sb.append(next);
                    i++;
                } else if (next == 'n') {
                    sb.append('\n');
                    i++;
                } else if (next == 't') {
                    sb.append('\t');
                    i++;
                } else {
                    sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Convert Graph SDK UntypedNode values to plain Java objects.
     */
    private Map<String, Object> convertUntypedNodes(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result.put(entry.getKey(), convertUntypedNode(entry.getValue()));
        }
        return result;
    }

    /**
     * Convert a single UntypedNode or regular value to a plain Java object.
     */
    private Object convertUntypedNode(Object value) {
        if (value instanceof UntypedString u) {
            return u.getValue();
        } else if (value instanceof UntypedInteger u) {
            return u.getValue();
        } else if (value instanceof UntypedLong u) {
            return u.getValue();
        } else if (value instanceof UntypedDouble u) {
            return u.getValue();
        } else if (value instanceof UntypedFloat u) {
            return u.getValue();
        } else if (value instanceof UntypedBoolean u) {
            return u.getValue();
        } else if (value instanceof UntypedNull) {
            return null;
        } else if (value instanceof UntypedObject u) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, UntypedNode> entry : u.getValue().entrySet()) {
                map.put(entry.getKey(), convertUntypedNode(entry.getValue()));
            }
            return map;
        } else if (value instanceof UntypedArray u) {
            List<Object> list = new ArrayList<>();
            for (UntypedNode node : u.getValue()) {
                list.add(convertUntypedNode(node));
            }
            return list;
        }
        // Already a plain Java type
        return value;
    }

    /**
     * Read all bytes from an InputStream.
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }
}
