package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Uploads a file to a SharePoint document library via Microsoft Graph API.
 * Connector ID: sharepoint-upload-file
 */
@Slf4j
public class UploadFileConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_FILE_NAME = "fileName";
    static final String INPUT_FILE_CONTENT_BASE64 = "fileContentBase64";
    static final String INPUT_MIME_TYPE = "mimeType";
    static final String INPUT_PARENT_ITEM_ID = "parentItemId";
    static final String INPUT_DRIVE_ID = "driveId";
    static final String INPUT_CONFLICT_BEHAVIOR = "conflictBehavior";

    // Output constants
    static final String OUTPUT_ITEM_ID = "itemId";
    static final String OUTPUT_ITEM_WEB_URL = "itemWebUrl";
    static final String OUTPUT_ETAG = "eTag";

    @Override
    protected SharePointConfiguration buildConfiguration() {
        return SharePointConfiguration.builder()
                .tenantId(getStringInput(INPUT_TENANT_ID))
                .clientId(getStringInput(INPUT_CLIENT_ID))
                .clientSecret(getStringInput(INPUT_CLIENT_SECRET))
                .clientCertificatePem(getStringInput(INPUT_CLIENT_CERTIFICATE_PEM))
                .siteId(getStringInput(INPUT_SITE_ID))
                .connectTimeout(getIntegerInputOrDefault(INPUT_CONNECT_TIMEOUT, 30000))
                .readTimeout(getIntegerInputOrDefault(INPUT_READ_TIMEOUT, 60000))
                .fileName(getStringInput(INPUT_FILE_NAME))
                .fileContentBase64(getStringInput(INPUT_FILE_CONTENT_BASE64))
                .mimeType(getStringInput(INPUT_MIME_TYPE))
                .parentItemId(getStringInput(INPUT_PARENT_ITEM_ID))
                .driveId(getStringInput(INPUT_DRIVE_ID))
                .conflictBehavior(getStringInput(INPUT_CONFLICT_BEHAVIOR))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        if (isNullOrEmpty(getStringInput(INPUT_FILE_NAME))) {
            errors.add("fileName is required");
        }
        if (isNullOrEmpty(getStringInput(INPUT_FILE_CONTENT_BASE64))) {
            errors.add("fileContentBase64 is required");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing UploadFile connector");
        UploadFileResult result = client.uploadFile(configuration);
        setOutputParameter(OUTPUT_ITEM_ID, result.itemId());
        setOutputParameter(OUTPUT_ITEM_WEB_URL, result.itemWebUrl());
        setOutputParameter(OUTPUT_ETAG, result.eTag());
        log.info("UploadFile completed: itemId={}", result.itemId());
    }
}
