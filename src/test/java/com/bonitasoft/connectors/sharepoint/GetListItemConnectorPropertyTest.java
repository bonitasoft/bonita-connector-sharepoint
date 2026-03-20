package com.bonitasoft.connectors.sharepoint;

import static org.assertj.core.api.Assertions.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.HashMap;
import java.util.Map;

class GetListItemConnectorPropertyTest {

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n", "  \t  ");
    }

    @Provide
    Arbitrary<String> nonBlankStrings() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100);
    }


    @Property
    void tenantIdRejectsBlank(@ForAll("blankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("tenantId", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void clientIdRejectsBlank(@ForAll("blankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("clientId", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void siteIdRejectsBlank(@ForAll("blankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("siteId", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void listIdRejectsBlank(@ForAll("blankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("listId", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void listItemIdRejectsBlank(@ForAll("blankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("listItemId", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }


    @Property(tries = 1)
    void tenantIdRejectsNull() {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.remove("tenantId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 1)
    void clientIdRejectsNull() {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.remove("clientId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 1)
    void siteIdRejectsNull() {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.remove("siteId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 1)
    void listIdRejectsNull() {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.remove("listId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 1)
    void listItemIdRejectsNull() {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.remove("listItemId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }


    @Property
    void tenantIdAcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("tenantId", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void clientIdAcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("clientId", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void siteIdAcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("siteId", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void listIdAcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("listId", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void listItemIdAcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("listItemId", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void configurationBuildsWithValidInputs(
            @ForAll @net.jqwik.api.constraints.AlphaChars @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @net.jqwik.api.constraints.AlphaChars @StringLength(min = 1, max = 50) String clientId,
            @ForAll @net.jqwik.api.constraints.AlphaChars @StringLength(min = 1, max = 50) String secret,
            @ForAll @net.jqwik.api.constraints.AlphaChars @StringLength(min = 1, max = 50) String siteId) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("tenantId", tenantId);
        inputs.put("clientId", clientId);
        inputs.put("clientSecret", secret);
        inputs.put("siteId", siteId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property(tries = 1)
    void defaultTimeoutsApplied() throws Exception {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }

    @Property
    void customTimeoutsApplied(@ForAll("positiveInts") int connectTimeout,
                                @ForAll("positiveInts") int readTimeout) throws Exception {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("connectTimeout", connectTimeout);
        inputs.put("readTimeout", readTimeout);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getConnectTimeout()).isEqualTo(connectTimeout);
        assertThat(config.getReadTimeout()).isEqualTo(readTimeout);
    }

    @Provide
    Arbitrary<Integer> positiveInts() {
        return Arbitraries.integers().between(1, 300000);
    }

    @Property(tries = 1)
    void defaultSelectFieldsIsNull() throws Exception {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getSelectFields()).isNull();
    }

    @Property(tries = 1)
    void getListItemResultStoresValues() {
        var fields = java.util.Map.<String, Object>of("Title", "Test");
        var result = new GetListItemResult(fields, "li-1", "2024-01-01", "2024-01-02");
        assertThat(result.fields()).containsEntry("Title", "Test");
        assertThat(result.listItemId()).isEqualTo("li-1");
        assertThat(result.createdDateTime()).isEqualTo("2024-01-01");
        assertThat(result.lastModifiedDateTime()).isEqualTo("2024-01-02");
    }

    @Property
    void listItemIdAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String itemId) {
        var connector = new GetListItemConnector();
        var inputs = validInputs();
        inputs.put("listItemId", itemId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("tenantId", "test-tenant");
        inputs.put("clientId", "test-client");
        inputs.put("clientSecret", "test-secret");
        inputs.put("siteId", "test-site");
        inputs.put("listId", "list-123");
        inputs.put("listItemId", "li-123");
        return inputs;
    }
}
