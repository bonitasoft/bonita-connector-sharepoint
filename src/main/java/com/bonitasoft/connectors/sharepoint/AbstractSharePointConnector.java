package com.bonitasoft.connectors.sharepoint;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base connector for SharePoint operations.
 * Validates connection parameters, builds configuration, and provides
 * template method pattern for operation execution with error handling.
 */
@Slf4j
public abstract class AbstractSharePointConnector extends AbstractConnector {

    // Connection/auth input parameter constants
    protected static final String INPUT_TENANT_ID = "tenantId";
    protected static final String INPUT_CLIENT_ID = "clientId";
    protected static final String INPUT_CLIENT_SECRET = "clientSecret";
    protected static final String INPUT_CLIENT_CERTIFICATE_PEM = "clientCertificatePem";
    protected static final String INPUT_SITE_ID = "siteId";
    protected static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    protected static final String INPUT_READ_TIMEOUT = "readTimeout";

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected SharePointConfiguration configuration;
    protected SharePointClient client;

    /**
     * Validates all input parameters and builds the configuration object.
     * Called by Bonita engine before connect().
     */
    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        List<String> errors = new ArrayList<>();

        // Validate mandatory connection parameters
        if (isNullOrEmpty(getStringInput(INPUT_TENANT_ID))) {
            errors.add("tenantId is mandatory");
        }
        if (isNullOrEmpty(getStringInput(INPUT_CLIENT_ID))) {
            errors.add("clientId is mandatory");
        }
        boolean hasSecret = !isNullOrEmpty(getStringInput(INPUT_CLIENT_SECRET));
        boolean hasCert = !isNullOrEmpty(getStringInput(INPUT_CLIENT_CERTIFICATE_PEM));
        if (!hasSecret && !hasCert) {
            errors.add("At least one of clientSecret or clientCertificatePem is mandatory");
        }
        if (isNullOrEmpty(getStringInput(INPUT_SITE_ID))) {
            errors.add("siteId is mandatory");
        }

        // Validate operation-specific parameters
        validateOperationParameters(errors);

        if (!errors.isEmpty()) {
            throw new ConnectorValidationException(this, String.join("; ", errors));
        }

        // Build configuration after validation passes
        this.configuration = buildConfiguration();
    }

    /**
     * Creates and authenticates the SharePoint client via Microsoft Graph SDK.
     */
    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new SharePointClient(this.configuration);
            log.info("SharePoint connector connected successfully");
        } catch (SharePointException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    /**
     * Stateless HTTP -- null the client reference.
     */
    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    /**
     * Template method: wraps doExecute() with standard error handling.
     * Always sets success and errorMessage outputs.
     */
    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (SharePointException e) {
            log.error("SharePoint connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in SharePoint connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Subclasses implement this to perform their specific operation.
     */
    protected abstract void doExecute() throws SharePointException;

    /**
     * Subclasses implement this to build their full configuration from input parameters.
     */
    protected abstract SharePointConfiguration buildConfiguration();

    /**
     * Subclasses implement this to validate operation-specific parameters.
     * Add error messages to the errors list.
     */
    protected abstract void validateOperationParameters(List<String> errors);

    // ========================= Input reading helpers =========================

    /** Read a String input, returning null if not set. */
    protected String getStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Read a String input with a default value. */
    protected String getStringInputOrDefault(String name, String defaultValue) {
        String value = getStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Read a Boolean input, returning null if not set. */
    protected Boolean getBooleanInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : null;
    }

    /** Read a Boolean input with a default value. */
    protected boolean getBooleanInputOrDefault(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /** Read an Integer input, returning null if not set. */
    protected Integer getIntegerInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : null;
    }

    /** Read an Integer input with a default value. */
    protected int getIntegerInputOrDefault(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /** Read a Long input with a default value. */
    protected long getLongInputOrDefault(String name, long defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).longValue() : defaultValue;
    }

    /** Check if a string is null or empty/blank. */
    protected static boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }
}
