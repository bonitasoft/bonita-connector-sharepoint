package com.bonitasoft.connectors.sharepoint;

import java.util.List;
import java.util.Map;

public record ListChildrenResult(List<Map<String, Object>> items, int totalCount, String nextPageToken) {}
