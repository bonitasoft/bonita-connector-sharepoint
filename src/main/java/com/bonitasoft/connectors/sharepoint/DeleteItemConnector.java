package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Deletes a file or folder from a SharePoint document library via Microsoft Graph API.
 * Connector ID: sharepoint-delete-item
 */
@Slf4j
public class DeleteItemConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_ITEM_ID = "itemId";
    static final String INPUT_DRIVE_ID = "driveId";
    static final String INPUT_PERMANENT = "permanent";

    // Output constants
    static final String OUTPUT_DELETED_ITEM_ID = "deletedItemId";
    static final String OUTPUT_PERMANENT = "permanent";

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
                .permanent(getBooleanInputOrDefault(INPUT_PERMANENT, false))
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
        log.info("Executing DeleteItem connector");
        DeleteItemResult result = client.deleteItem(configuration);
        setOutputParameter(OUTPUT_DELETED_ITEM_ID, result.deletedItemId());
        setOutputParameter(OUTPUT_PERMANENT, result.permanent());
        log.info("DeleteItem completed: deletedItemId={}, permanent={}", result.deletedItemId(), result.permanent());
    }
}
