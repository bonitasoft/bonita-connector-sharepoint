package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates a folder in a SharePoint document library via Microsoft Graph API.
 * Connector ID: sharepoint-create-folder
 */
@Slf4j
public class CreateFolderConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_FOLDER_NAME = "folderName";
    static final String INPUT_PARENT_ITEM_ID = "parentItemId";
    static final String INPUT_DRIVE_ID = "driveId";
    static final String INPUT_CONFLICT_BEHAVIOR = "conflictBehavior";

    // Output constants
    static final String OUTPUT_FOLDER_ID = "folderId";
    static final String OUTPUT_FOLDER_WEB_URL = "folderWebUrl";

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
                .folderName(getStringInput(INPUT_FOLDER_NAME))
                .parentItemId(getStringInput(INPUT_PARENT_ITEM_ID))
                .driveId(getStringInput(INPUT_DRIVE_ID))
                .conflictBehavior(getStringInput(INPUT_CONFLICT_BEHAVIOR))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        if (isNullOrEmpty(getStringInput(INPUT_FOLDER_NAME))) {
            errors.add("folderName is required");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing CreateFolder connector");
        CreateFolderResult result = client.createFolder(configuration);
        setOutputParameter(OUTPUT_FOLDER_ID, result.folderId());
        setOutputParameter(OUTPUT_FOLDER_WEB_URL, result.folderWebUrl());
        log.info("CreateFolder completed: folderId={}", result.folderId());
    }
}
