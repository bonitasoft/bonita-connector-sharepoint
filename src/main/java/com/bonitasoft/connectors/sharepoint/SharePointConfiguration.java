package com.bonitasoft.connectors.sharepoint;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for SharePoint connector.
 * Holds connection/auth parameters and operation-specific parameters.
 */
@Data
@Builder
public class SharePointConfiguration {

    // === Connection / Auth parameters ===
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String clientCertificatePem;
    private String siteId;

    @Builder.Default
    private int connectTimeout = 30000;

    @Builder.Default
    private int readTimeout = 60000;

    @Builder.Default
    private int maxRetries = 5;

    // === Upload File parameters ===
    private String fileName;
    private String fileContentBase64;
    private String mimeType;
    private String parentItemId;
    private String driveId;
    private String conflictBehavior;

    // === Download File / Delete Item parameters ===
    private String itemId;

    // === Create Folder parameters ===
    private String folderName;

    // === List Children parameters ===
    @Builder.Default
    private int maxResults = 200;
    @Builder.Default
    private boolean includeFiles = true;
    @Builder.Default
    private boolean includeFolders = true;

    // === Delete Item parameters ===
    @Builder.Default
    private boolean permanent = false;

    // === List operations parameters ===
    private String listId;
    private String fields;
    private String listItemId;
    private String selectFields;
}
