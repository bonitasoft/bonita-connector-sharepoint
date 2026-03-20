package com.bonitasoft.connectors.sharepoint;

import java.util.Map;

public record GetListItemResult(Map<String, Object> fields, String listItemId, String createdDateTime, String lastModifiedDateTime) {}
