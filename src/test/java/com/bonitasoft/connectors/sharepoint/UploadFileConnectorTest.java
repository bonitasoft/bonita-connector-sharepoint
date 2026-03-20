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
class UploadFileConnectorTest {

    @Mock
    private SharePointClient mockClient;

    private UploadFileConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new UploadFileConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("siteId", "test-site-id");
        inputs.put("fileName", "test.pdf");
        inputs.put("fileContentBase64", "dGVzdA==");
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

        when(mockClient.uploadFile(any())).thenReturn(
                new UploadFileResult("item-123", "https://sp.com/file", "etag-1"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "itemId")).isEqualTo("item-123");
        assertThat(getOutput(connector, "itemWebUrl")).isEqualTo("https://sp.com/file");
        assertThat(getOutput(connector, "eTag")).isEqualTo("etag-1");
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
    void shouldFailValidationWhenFileNameMissing() {
        inputs.remove("fileName");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("fileName");
    }

    @Test
    void shouldFailValidationWhenFileContentBase64Missing() {
        inputs.remove("fileContentBase64");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("fileContentBase64");
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

        when(mockClient.uploadFile(any()))
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

        when(mockClient.uploadFile(any()))
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

        when(mockClient.uploadFile(any()))
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
        assertThat(config.getParentItemId()).isNull();
        assertThat(config.getDriveId()).isNull();
        assertThat(config.getConflictBehavior()).isNull();
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.uploadFile(any())).thenReturn(
                new UploadFileResult("item-123", "https://sp.com/file", "etag-1"));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = getOutputs(connector);
        assertThat(outputs.get("success")).isNotNull();
        assertThat(outputs.get("itemId")).isNotNull();
        assertThat(outputs.get("itemWebUrl")).isNotNull();
        assertThat(outputs.get("eTag")).isNotNull();
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.uploadFile(any()))
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

        when(mockClient.uploadFile(any()))
                .thenThrow(new RuntimeException("Unexpected NPE"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat(getOutput(connector, "errorMessage")).asString().contains("Unexpected");
    }
}
