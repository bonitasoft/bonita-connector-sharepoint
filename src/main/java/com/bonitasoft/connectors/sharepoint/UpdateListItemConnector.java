package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Updates an existing list item in a SharePoint list via Microsoft Graph API.
 * Connector ID: sharepoint-update-list-item
 */
@Slf4j
public class UpdateListItemConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_LIST_ID = "listId";
    static final String INPUT_LIST_ITEM_ID = "listItemId";
    static final String INPUT_FIELDS = "fields";

    // Output constants
    static final String OUTPUT_LIST_ITEM_ID = "listItemId";
    static final String OUTPUT_LAST_MODIFIED_DATE_TIME = "lastModifiedDateTime";

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
                .listId(getStringInput(INPUT_LIST_ID))
                .listItemId(getStringInput(INPUT_LIST_ITEM_ID))
                .fields(getStringInput(INPUT_FIELDS))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        if (isNullOrEmpty(getStringInput(INPUT_LIST_ID))) {
            errors.add("listId is required");
        }
        if (isNullOrEmpty(getStringInput(INPUT_LIST_ITEM_ID))) {
            errors.add("listItemId is required");
        }
        if (isNullOrEmpty(getStringInput(INPUT_FIELDS))) {
            errors.add("fields is required");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing UpdateListItem connector");
        UpdateListItemResult result = client.updateListItem(configuration);
        setOutputParameter(OUTPUT_LIST_ITEM_ID, result.listItemId());
        setOutputParameter(OUTPUT_LAST_MODIFIED_DATE_TIME, result.lastModifiedDateTime());
        log.info("UpdateListItem completed: listItemId={}", result.listItemId());
    }
}
