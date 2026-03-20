package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloads a file from a SharePoint document library via Microsoft Graph API.
 * Connector ID: sharepoint-download-file
 */
@Slf4j
public class DownloadFileConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_ITEM_ID = "itemId";
    static final String INPUT_DRIVE_ID = "driveId";

    // Output constants
    static final String OUTPUT_FILE_CONTENT_BASE64 = "fileContentBase64";
    static final String OUTPUT_FILE_NAME = "fileName";
    static final String OUTPUT_MIME_TYPE = "mimeType";
    static final String OUTPUT_FILE_SIZE_BYTES = "fileSizeBytes";

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
                .itemId(getStringInput(INPUT_ITEM_ID))
                .driveId(getStringInput(INPUT_DRIVE_ID))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        if (isNullOrEmpty(getStringInput(INPUT_ITEM_ID))) {
            errors.add("itemId is required");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing DownloadFile connector");
        DownloadFileResult result = client.downloadFile(configuration);
        setOutputParameter(OUTPUT_FILE_CONTENT_BASE64, result.fileContentBase64());
        setOutputParameter(OUTPUT_FILE_NAME, result.fileName());
        setOutputParameter(OUTPUT_MIME_TYPE, result.mimeType());
        setOutputParameter(OUTPUT_FILE_SIZE_BYTES, result.fileSizeBytes());
        log.info("DownloadFile completed: fileName={}, size={} bytes", result.fileName(), result.fileSizeBytes());
    }
}
