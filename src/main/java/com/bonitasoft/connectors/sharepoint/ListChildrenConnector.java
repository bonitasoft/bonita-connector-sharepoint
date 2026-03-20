package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Lists children (files and/or folders) of a SharePoint drive item via Microsoft Graph API.
 * Connector ID: sharepoint-list-children
 */
@Slf4j
public class ListChildrenConnector extends AbstractSharePointConnector {

    // Operation-specific input constants
    static final String INPUT_PARENT_ITEM_ID = "parentItemId";
    static final String INPUT_DRIVE_ID = "driveId";
    static final String INPUT_MAX_RESULTS = "maxResults";
    static final String INPUT_INCLUDE_FILES = "includeFiles";
    static final String INPUT_INCLUDE_FOLDERS = "includeFolders";

    // Output constants
    static final String OUTPUT_ITEMS = "items";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_NEXT_PAGE_TOKEN = "nextPageToken";

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
                .parentItemId(getStringInput(INPUT_PARENT_ITEM_ID))
                .driveId(getStringInput(INPUT_DRIVE_ID))
                .maxResults(getIntegerInputOrDefault(INPUT_MAX_RESULTS, 100))
                .includeFiles(getBooleanInputOrDefault(INPUT_INCLUDE_FILES, true))
                .includeFolders(getBooleanInputOrDefault(INPUT_INCLUDE_FOLDERS, true))
                .build();
    }

    @Override
    protected void validateOperationParameters(java.util.List<String> errors) {
        Integer maxResults = getIntegerInput(INPUT_MAX_RESULTS);
        if (maxResults != null && maxResults < 1) {
            errors.add("maxResults must be a positive number");
        }
    }

    @Override
    protected void doExecute() throws SharePointException {
        log.info("Executing ListChildren connector");
        ListChildrenResult result = client.listChildren(configuration);
        setOutputParameter(OUTPUT_ITEMS, result.items());
        setOutputParameter(OUTPUT_TOTAL_COUNT, result.totalCount());
        setOutputParameter(OUTPUT_NEXT_PAGE_TOKEN, result.nextPageToken());
        log.info("ListChildren completed: totalCount={}", result.totalCount());
    }
}
