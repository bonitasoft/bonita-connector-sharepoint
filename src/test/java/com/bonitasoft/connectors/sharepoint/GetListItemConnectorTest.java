package com.bonitasoft.connectors.sharepoint;

import static com.bonitasoft.connectors.sharepoint.ConnectorTestToolkit.getOutput;
import static com.bonitasoft.connectors.sharepoint.ConnectorTestToolkit.getOutputs;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class GetListItemConnectorTest {

    @Mock
    private SharePointClient mockClient;

    private GetListItemConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new GetListItemConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("siteId", "test-site-id");
        inputs.put("listId", "list-123");
        inputs.put("listItemId", "li-123");
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractSharePointConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any())).thenReturn(
                new GetListItemResult(Map.of("Title", "Test"), "li-123", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "listItemId")).isEqualTo("li-123");
        assertThat(getOutput(connector, "fields")).isNotNull();
        assertThat(getOutput(connector, "createdDateTime")).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(getOutput(connector, "lastModifiedDateTime")).isEqualTo("2024-01-02T00:00:00Z");
    }

    @Test
    void shouldFailValidationWhenTenantIdMissing() {
        inputs.remove("tenantId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldFailValidationWhenClientIdMissing() {
        inputs.remove("clientId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("clientId");
    }

    @Test
    void shouldFailValidationWhenNoCredentialProvided() {
        inputs.remove("clientSecret");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("clientSecret");
    }

    @Test
    void shouldFailValidationWhenSiteIdMissing() {
        inputs.remove("siteId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("siteId");
    }

    @Test
    void shouldFailValidationWhenListIdMissing() {
        inputs.remove("listId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("listId");
    }

    @Test
    void shouldFailValidationWhenListItemIdMissing() {
        inputs.remove("listItemId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("listItemId");
    }

    @Test
    void shouldAcceptCertificateInsteadOfSecret() throws Exception {
        inputs.remove("clientSecret");
        inputs.put("clientCertificatePem", "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----");
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Test
    void shouldRetryOnRateLimit() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any()))
                .thenThrow(new SharePointException("Rate limited", 429, true));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("Rate limited");
    }

    @Test
    void shouldFailImmediatelyOnAuthError() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any()))
                .thenThrow(new SharePointException("Unauthorized", 401, false));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("Unauthorized");
    }

    @Test
    void shouldHandleNetworkTimeout() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any()))
                .thenThrow(new SharePointException("Connection timed out",
                        new java.net.SocketTimeoutException("timeout")));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("timed out");
    }

    @Test
    void shouldApplyDefaultsForNullOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getSelectFields()).isNull();
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any())).thenReturn(
                new GetListItemResult(Map.of("Title", "Test"), "li-123", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = getOutputs(connector);
        assertThat(outputs.get("success")).isNotNull();
        assertThat(outputs.get("fields")).isNotNull();
        assertThat(outputs.get("listItemId")).isNotNull();
        assertThat(outputs.get("createdDateTime")).isNotNull();
        assertThat(outputs.get("lastModifiedDateTime")).isNotNull();
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any()))
                .thenThrow(new SharePointException("Server error", 500, true));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("Server error");
    }

    @Test
    void shouldFailValidationWhenTenantIdBlank() {
        inputs.put("tenantId", "   ");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldHandleUnexpectedException() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getListItem(any()))
                .thenThrow(new RuntimeException("Unexpected NPE"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("Unexpected");
    }
}
