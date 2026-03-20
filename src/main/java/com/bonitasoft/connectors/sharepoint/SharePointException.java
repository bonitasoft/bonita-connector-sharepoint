package com.bonitasoft.connectors.sharepoint;

/**
 * Typed exception for SharePoint connector operations.
 * Carries HTTP status code and retryable flag for retry policy integration.
 */
public class SharePointException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public SharePointException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public SharePointException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public SharePointException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public SharePointException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
