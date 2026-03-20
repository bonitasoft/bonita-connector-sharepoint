package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates a new list item in a SharePoint list via Microsoft Graph API.
 * Connector ID: sharepoint-create-list-item
 */
@Slf4j
public class CreateListItemConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_LIST_ID = "listId";
    static final String INPUT_FIELDS = "fields";

    // Output constants
    static final String OUTPUT_LIST_ITEM_ID = "listItemId";
    static final String OUTPUT_LIST_ITEM_WEB_URL = "listItemWebUrl";
    static final String OUTPUT_CREATED_DATE_TIME = "createdDateTime";

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
                .fields(getStringInput(INPUT_FIELDS))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        if (isNullOrEmpty(getStringInput(INPUT_LIST_ID))) {
            errors.add("listId is required");
        }
        if (isNullOrEmpty(getStringInput(INPUT_FIELDS))) {
            errors.add("fields is required");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing CreateListItem connector");
        CreateListItemResult result = client.createListItem(configuration);
        setOutputParameter(OUTPUT_LIST_ITEM_ID, result.listItemId());
        setOutputParameter(OUTPUT_LIST_ITEM_WEB_URL, result.listItemWebUrl());
        setOutputParameter(OUTPUT_CREATED_DATE_TIME, result.createdDateTime());
        log.info("CreateListItem completed: listItemId={}", result.listItemId());
    }
}
