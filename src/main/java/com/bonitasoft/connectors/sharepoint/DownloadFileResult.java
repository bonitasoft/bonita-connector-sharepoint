package com.bonitasoft.connectors.sharepoint;

public record DownloadFileResult(String fileContentBase64, String fileName, String mimeType, Long fileSizeBytes) {}
