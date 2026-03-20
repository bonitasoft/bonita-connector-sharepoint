import os

BASE = "C:/Bonita/PROJECTS/Connector_repositories/bonita-connector-sharepoint"
TEST_DIR = os.path.join(BASE, "src/test/java/com/bonitasoft/connectors/sharepoint")
os.makedirs(TEST_DIR, exist_ok=True)

def w(name, content):
    path = os.path.join(TEST_DIR, name)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print(f"  OK: {name}")

# ================================================================
# UNIT TEST GENERATION
# ================================================================

def gen_unit_test(op, client_method, extra_inputs, mock_success, assert_outputs,
                  extra_validation_tests, assert_defaults, assert_all_outputs):
    return f'''package com.bonitasoft.connectors.sharepoint;

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
class {op}ConnectorTest {{

    @Mock
    private SharePointClient mockClient;

    private {op}Connector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {{
        connector = new {op}Connector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("siteId", "test-site-id");
{extra_inputs}
    }}

    private void injectMockClient() throws Exception {{
        var clientField = AbstractSharePointConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }}

    @Test
    void shouldExecuteSuccessfully() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

{mock_success}

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(true);
{assert_outputs}
    }}

    @Test
    void shouldFailValidationWhenTenantIdMissing() {{
        inputs.remove("tenantId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tenantId");
    }}

    @Test
    void shouldFailValidationWhenClientIdMissing() {{
        inputs.remove("clientId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("clientId");
    }}

    @Test
    void shouldFailValidationWhenNoCredentialProvided() {{
        inputs.remove("clientSecret");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("clientSecret");
    }}

    @Test
    void shouldFailValidationWhenSiteIdMissing() {{
        inputs.remove("siteId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("siteId");
    }}

{extra_validation_tests}

    @Test
    void shouldAcceptCertificateInsteadOfSecret() throws Exception {{
        inputs.remove("clientSecret");
        inputs.put("clientCertificatePem", "-----BEGIN CERTIFICATE-----\\ntest\\n-----END CERTIFICATE-----");
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }}

    @Test
    void shouldRetryOnRateLimit() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.{client_method}(any()))
                .thenThrow(new SharePointException("Rate limited", 429, true));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(false);
        assertThat(connector.getOutputParameters().get("errorMessage")).asString().contains("Rate limited");
    }}

    @Test
    void shouldFailImmediatelyOnAuthError() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.{client_method}(any()))
                .thenThrow(new SharePointException("Unauthorized", 401, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(false);
        assertThat(connector.getOutputParameters().get("errorMessage")).asString().contains("Unauthorized");
    }}

    @Test
    void shouldHandleNetworkTimeout() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.{client_method}(any()))
                .thenThrow(new SharePointException("Connection timed out",
                        new java.net.SocketTimeoutException("timeout")));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(false);
        assertThat(connector.getOutputParameters().get("errorMessage")).asString().contains("timed out");
    }}

    @Test
    void shouldApplyDefaultsForNullOptionalInputs() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
{assert_defaults}
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }}

    @Test
    void shouldPopulateAllOutputFields() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

{mock_success}

        connector.executeBusinessLogic();

        Map<String, Object> outputs = connector.getOutputParameters();
        assertThat(outputs.get("success")).isNotNull();
{assert_all_outputs}
    }}

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.{client_method}(any()))
                .thenThrow(new SharePointException("Server error", 500, true));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(false);
        assertThat(connector.getOutputParameters().get("errorMessage")).asString().contains("Server error");
    }}

    @Test
    void shouldFailValidationWhenTenantIdBlank() {{
        inputs.put("tenantId", "   ");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tenantId");
    }}

    @Test
    void shouldHandleUnexpectedException() throws Exception {{
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.{client_method}(any()))
                .thenThrow(new RuntimeException("Unexpected NPE"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(false);
        assertThat(connector.getOutputParameters().get("errorMessage")).asString().contains("Unexpected");
    }}
}}
'''

print("=== Generating Unit Tests ===")

# 1. UploadFile
w("UploadFileConnectorTest.java", gen_unit_test(
    "UploadFile", "uploadFile",
    '        inputs.put("fileName", "test.pdf");\n        inputs.put("fileContentBase64", "dGVzdA==");',
    '        when(mockClient.uploadFile(any())).thenReturn(\n                new UploadFileResult("item-123", "https://sp.com/file", "etag-1"));',
    '        assertThat(connector.getOutputParameters().get("itemId")).isEqualTo("item-123");\n        assertThat(connector.getOutputParameters().get("itemWebUrl")).isEqualTo("https://sp.com/file");\n        assertThat(connector.getOutputParameters().get("eTag")).isEqualTo("etag-1");',
    '''    @Test
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
    }''',
    '        assertThat(config.getParentItemId()).isNull();\n        assertThat(config.getDriveId()).isNull();\n        assertThat(config.getConflictBehavior()).isNull();',
    '        assertThat(outputs.get("itemId")).isNotNull();\n        assertThat(outputs.get("itemWebUrl")).isNotNull();\n        assertThat(outputs.get("eTag")).isNotNull();',
))

# 2. DownloadFile
w("DownloadFileConnectorTest.java", gen_unit_test(
    "DownloadFile", "downloadFile",
    '        inputs.put("itemId", "item-123");',
    '        when(mockClient.downloadFile(any())).thenReturn(\n                new DownloadFileResult("dGVzdA==", "report.pdf", "application/pdf", 1024L));',
    '        assertThat(connector.getOutputParameters().get("fileContentBase64")).isEqualTo("dGVzdA==");\n        assertThat(connector.getOutputParameters().get("fileName")).isEqualTo("report.pdf");\n        assertThat(connector.getOutputParameters().get("mimeType")).isEqualTo("application/pdf");\n        assertThat(connector.getOutputParameters().get("fileSizeBytes")).isEqualTo(1024L);',
    '''    @Test
    void shouldFailValidationWhenItemIdMissing() {
        inputs.remove("itemId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("itemId");
    }''',
    '        assertThat(config.getDriveId()).isNull();',
    '        assertThat(outputs.get("fileContentBase64")).isNotNull();\n        assertThat(outputs.get("fileName")).isNotNull();\n        assertThat(outputs.get("mimeType")).isNotNull();\n        assertThat(outputs.get("fileSizeBytes")).isNotNull();',
))

# 3. CreateFolder
w("CreateFolderConnectorTest.java", gen_unit_test(
    "CreateFolder", "createFolder",
    '        inputs.put("folderName", "New Folder");',
    '        when(mockClient.createFolder(any())).thenReturn(\n                new CreateFolderResult("folder-123", "https://sp.com/folder"));',
    '        assertThat(connector.getOutputParameters().get("folderId")).isEqualTo("folder-123");\n        assertThat(connector.getOutputParameters().get("folderWebUrl")).isEqualTo("https://sp.com/folder");',
    '''    @Test
    void shouldFailValidationWhenFolderNameMissing() {
        inputs.remove("folderName");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("folderName");
    }''',
    '        assertThat(config.getParentItemId()).isNull();\n        assertThat(config.getDriveId()).isNull();\n        assertThat(config.getConflictBehavior()).isNull();',
    '        assertThat(outputs.get("folderId")).isNotNull();\n        assertThat(outputs.get("folderWebUrl")).isNotNull();',
))

# 4. ListChildren
w("ListChildrenConnectorTest.java", gen_unit_test(
    "ListChildren", "listChildren",
    '',
    '        when(mockClient.listChildren(any())).thenReturn(\n                new ListChildrenResult(List.of(Map.of("id", "item-1", "name", "file.txt")), 1, null));',
    '        assertThat(connector.getOutputParameters().get("items")).isNotNull();\n        assertThat(connector.getOutputParameters().get("totalCount")).isEqualTo(1);',
    '''    @Test
    void shouldFailValidationWhenMaxResultsNegative() {
        inputs.put("maxResults", -1);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("maxResults");
    }''',
    '        assertThat(config.getParentItemId()).isNull();\n        assertThat(config.getDriveId()).isNull();\n        assertThat(config.isIncludeFiles()).isTrue();\n        assertThat(config.isIncludeFolders()).isTrue();',
    '        assertThat(outputs.get("items")).isNotNull();\n        assertThat(outputs.get("totalCount")).isNotNull();',
))

# 5. DeleteItem
w("DeleteItemConnectorTest.java", gen_unit_test(
    "DeleteItem", "deleteItem",
    '        inputs.put("itemId", "item-to-delete");',
    '        when(mockClient.deleteItem(any())).thenReturn(\n                new DeleteItemResult("item-to-delete", false));',
    '        assertThat(connector.getOutputParameters().get("deletedItemId")).isEqualTo("item-to-delete");\n        assertThat(connector.getOutputParameters().get("permanent")).isEqualTo(false);',
    '''    @Test
    void shouldFailValidationWhenItemIdMissing() {
        inputs.remove("itemId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("itemId");
    }''',
    '        assertThat(config.getDriveId()).isNull();\n        assertThat(config.isPermanent()).isFalse();',
    '        assertThat(outputs.get("deletedItemId")).isNotNull();\n        assertThat(outputs.get("permanent")).isNotNull();',
))

# 6. CreateListItem
w("CreateListItemConnectorTest.java", gen_unit_test(
    "CreateListItem", "createListItem",
    '        inputs.put("listId", "list-123");\n        inputs.put("fields", "{\\"Title\\": \\"Test Item\\"}");',
    '        when(mockClient.createListItem(any())).thenReturn(\n                new CreateListItemResult("li-123", "https://sp.com/list/item", "2024-01-01T00:00:00Z"));',
    '        assertThat(connector.getOutputParameters().get("listItemId")).isEqualTo("li-123");\n        assertThat(connector.getOutputParameters().get("listItemWebUrl")).isEqualTo("https://sp.com/list/item");\n        assertThat(connector.getOutputParameters().get("createdDateTime")).isEqualTo("2024-01-01T00:00:00Z");',
    '''    @Test
    void shouldFailValidationWhenListIdMissing() {
        inputs.remove("listId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("listId");
    }

    @Test
    void shouldFailValidationWhenFieldsMissing() {
        inputs.remove("fields");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("fields");
    }''',
    '        assertThat(config.getConnectTimeout()).isEqualTo(30000);',
    '        assertThat(outputs.get("listItemId")).isNotNull();\n        assertThat(outputs.get("listItemWebUrl")).isNotNull();\n        assertThat(outputs.get("createdDateTime")).isNotNull();',
))

# 7. GetListItem
w("GetListItemConnectorTest.java", gen_unit_test(
    "GetListItem", "getListItem",
    '        inputs.put("listId", "list-123");\n        inputs.put("listItemId", "li-123");',
    '        when(mockClient.getListItem(any())).thenReturn(\n                new GetListItemResult(Map.of("Title", "Test"), "li-123", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z"));',
    '        assertThat(connector.getOutputParameters().get("listItemId")).isEqualTo("li-123");\n        assertThat(connector.getOutputParameters().get("fields")).isNotNull();\n        assertThat(connector.getOutputParameters().get("createdDateTime")).isEqualTo("2024-01-01T00:00:00Z");\n        assertThat(connector.getOutputParameters().get("lastModifiedDateTime")).isEqualTo("2024-01-02T00:00:00Z");',
    '''    @Test
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
    }''',
    '        assertThat(config.getSelectFields()).isNull();',
    '        assertThat(outputs.get("fields")).isNotNull();\n        assertThat(outputs.get("listItemId")).isNotNull();\n        assertThat(outputs.get("createdDateTime")).isNotNull();\n        assertThat(outputs.get("lastModifiedDateTime")).isNotNull();',
))

# 8. UpdateListItem
w("UpdateListItemConnectorTest.java", gen_unit_test(
    "UpdateListItem", "updateListItem",
    '        inputs.put("listId", "list-123");\n        inputs.put("listItemId", "li-123");\n        inputs.put("fields", "{\\"Title\\": \\"Updated\\"}");',
    '        when(mockClient.updateListItem(any())).thenReturn(\n                new UpdateListItemResult("li-123", "2024-01-02T12:00:00Z"));',
    '        assertThat(connector.getOutputParameters().get("listItemId")).isEqualTo("li-123");\n        assertThat(connector.getOutputParameters().get("lastModifiedDateTime")).isEqualTo("2024-01-02T12:00:00Z");',
    '''    @Test
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
    void shouldFailValidationWhenFieldsMissing() {
        inputs.remove("fields");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("fields");
    }''',
    '        assertThat(config.getConnectTimeout()).isEqualTo(30000);',
    '        assertThat(outputs.get("listItemId")).isNotNull();\n        assertThat(outputs.get("lastModifiedDateTime")).isNotNull();',
))

# ================================================================
# PROPERTY TEST GENERATION
# ================================================================

print("\n=== Generating Property Tests ===")

def gen_property_test(op, mandatory_inputs, optional_defaults, extra_properties):
    """Generate property test for a connector operation."""
    valid_inputs_method = "    private Map<String, Object> validInputs() {\n        var inputs = new HashMap<String, Object>();\n"
    valid_inputs_method += '        inputs.put("tenantId", "test-tenant");\n'
    valid_inputs_method += '        inputs.put("clientId", "test-client");\n'
    valid_inputs_method += '        inputs.put("clientSecret", "test-secret");\n'
    valid_inputs_method += '        inputs.put("siteId", "test-site");\n'
    for k, v in mandatory_inputs.items():
        valid_inputs_method += f'        inputs.put("{k}", {v});\n'
    valid_inputs_method += "        return inputs;\n    }"

    # Generate mandatory param rejection tests
    all_mandatory = ["tenantId", "clientId", "siteId"] + list(mandatory_inputs.keys())
    reject_blank_tests = ""
    reject_null_tests = ""
    for param in all_mandatory:
        reject_blank_tests += f'''
    @Property
    void {param}RejectsBlank(@ForAll("blankStrings") String value) {{
        var connector = new {op}Connector();
        var inputs = validInputs();
        inputs.put("{param}", value);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }}
'''
        reject_null_tests += f'''
    @Property(tries = 1)
    void {param}RejectsNull() {{
        var connector = new {op}Connector();
        var inputs = validInputs();
        inputs.remove("{param}");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }}
'''

    accept_valid = ""
    for param in all_mandatory:
        accept_valid += f'''
    @Property
    void {param}AcceptsValidNonBlank(@ForAll("nonBlankStrings") String value) {{
        var connector = new {op}Connector();
        var inputs = validInputs();
        inputs.put("{param}", value);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }}
'''

    return f'''package com.bonitasoft.connectors.sharepoint;

import static org.assertj.core.api.Assertions.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.HashMap;
import java.util.Map;

class {op}ConnectorPropertyTest {{

    @Provide
    Arbitrary<String> blankStrings() {{
        return Arbitraries.of("", " ", "\\t", "\\n", "  \\t  ");
    }}

    @Provide
    Arbitrary<String> nonBlankStrings() {{
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100);
    }}

{reject_blank_tests}
{reject_null_tests}
{accept_valid}
    @Property
    void configurationBuildsWithValidInputs(
            @ForAll @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @StringLength(min = 1, max = 50) String clientId,
            @ForAll @StringLength(min = 1, max = 50) String secret,
            @ForAll @StringLength(min = 1, max = 50) String siteId) {{
        var connector = new {op}Connector();
        var inputs = validInputs();
        inputs.put("tenantId", tenantId);
        inputs.put("clientId", clientId);
        inputs.put("clientSecret", secret);
        inputs.put("siteId", siteId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }}

    @Property(tries = 1)
    void defaultTimeoutsApplied() throws Exception {{
        var connector = new {op}Connector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }}

    @Property
    void customTimeoutsApplied(@ForAll("positiveInts") int connectTimeout,
                                @ForAll("positiveInts") int readTimeout) throws Exception {{
        var connector = new {op}Connector();
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
    }}

    @Provide
    Arbitrary<Integer> positiveInts() {{
        return Arbitraries.integers().between(1, 300000);
    }}

{extra_properties}

{valid_inputs_method}
}}
'''

# UploadFile property test
w("UploadFileConnectorPropertyTest.java", gen_property_test(
    "UploadFile",
    {"fileName": '"test.pdf"', "fileContentBase64": '"dGVzdA=="'},
    {},
    '''    @Property(tries = 1)
    void defaultConflictBehaviorIsNull() throws Exception {
        var connector = new UploadFileConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getConflictBehavior()).isNull();
    }

    @Property(tries = 1)
    void conflictBehaviorAcceptsValidValues() throws Exception {
        for (String behavior : new String[]{"rename", "replace", "fail"}) {
            var connector = new UploadFileConnector();
            var inputs = validInputs();
            inputs.put("conflictBehavior", behavior);
            connector.setInputParameters(inputs);
            connector.validateInputParameters();
            var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
            configField.setAccessible(true);
            SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
            assertThat(config.getConflictBehavior()).isEqualTo(behavior);
        }
    }

    @Property(tries = 1)
    void uploadFileResultStoresValues() {
        var result = new UploadFileResult("id-1", "https://example.com", "etag-1");
        assertThat(result.itemId()).isEqualTo("id-1");
        assertThat(result.itemWebUrl()).isEqualTo("https://example.com");
        assertThat(result.eTag()).isEqualTo("etag-1");
    }'''
))

# DownloadFile property test
w("DownloadFileConnectorPropertyTest.java", gen_property_test(
    "DownloadFile",
    {"itemId": '"item-123"'},
    {},
    '''    @Property(tries = 1)
    void defaultDriveIdIsNull() throws Exception {
        var connector = new DownloadFileConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getDriveId()).isNull();
    }

    @Property(tries = 1)
    void downloadFileResultStoresValues() {
        var result = new DownloadFileResult("base64data", "file.txt", "text/plain", 512L);
        assertThat(result.fileContentBase64()).isEqualTo("base64data");
        assertThat(result.fileName()).isEqualTo("file.txt");
        assertThat(result.mimeType()).isEqualTo("text/plain");
        assertThat(result.fileSizeBytes()).isEqualTo(512L);
    }

    @Property
    void itemIdAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String itemId) {
        var connector = new DownloadFileConnector();
        var inputs = validInputs();
        inputs.put("itemId", itemId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }'''
))

# CreateFolder property test
w("CreateFolderConnectorPropertyTest.java", gen_property_test(
    "CreateFolder",
    {"folderName": '"TestFolder"'},
    {},
    '''    @Property(tries = 1)
    void defaultParentItemIdIsNull() throws Exception {
        var connector = new CreateFolderConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.getParentItemId()).isNull();
    }

    @Property(tries = 1)
    void createFolderResultStoresValues() {
        var result = new CreateFolderResult("folder-id", "https://example.com/folder");
        assertThat(result.folderId()).isEqualTo("folder-id");
        assertThat(result.folderWebUrl()).isEqualTo("https://example.com/folder");
    }

    @Property
    void folderNameAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String name) {
        var connector = new CreateFolderConnector();
        var inputs = validInputs();
        inputs.put("folderName", name);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }'''
))

# ListChildren property test
w("ListChildrenConnectorPropertyTest.java", gen_property_test(
    "ListChildren",
    {},
    {},
    '''    @Property(tries = 1)
    void defaultIncludeFilesAndFolders() throws Exception {
        var connector = new ListChildrenConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.isIncludeFiles()).isTrue();
        assertThat(config.isIncludeFolders()).isTrue();
    }

    @Property
    void maxResultsAcceptsPositive(@ForAll("positiveInts") int maxResults) throws Exception {
        var connector = new ListChildrenConnector();
        var inputs = validInputs();
        inputs.put("maxResults", maxResults);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property(tries = 1)
    void listChildrenResultStoresValues() {
        var items = java.util.List.of(java.util.Map.<String, Object>of("id", "i1"));
        var result = new ListChildrenResult(items, 1, "nextToken");
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.nextPageToken()).isEqualTo("nextToken");
    }'''
))

# DeleteItem property test
w("DeleteItemConnectorPropertyTest.java", gen_property_test(
    "DeleteItem",
    {"itemId": '"item-to-delete"'},
    {},
    '''    @Property(tries = 1)
    void defaultPermanentIsFalse() throws Exception {
        var connector = new DeleteItemConnector();
        var inputs = validInputs();
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
        assertThat(config.isPermanent()).isFalse();
    }

    @Property(tries = 1)
    void permanentAcceptsBooleans() throws Exception {
        for (boolean perm : new boolean[]{true, false}) {
            var connector = new DeleteItemConnector();
            var inputs = validInputs();
            inputs.put("permanent", perm);
            connector.setInputParameters(inputs);
            connector.validateInputParameters();
            var configField = AbstractSharePointConnector.class.getDeclaredField("configuration");
            configField.setAccessible(true);
            SharePointConfiguration config = (SharePointConfiguration) configField.get(connector);
            assertThat(config.isPermanent()).isEqualTo(perm);
        }
    }

    @Property(tries = 1)
    void deleteItemResultStoresValues() {
        var result = new DeleteItemResult("del-id", true);
        assertThat(result.deletedItemId()).isEqualTo("del-id");
        assertThat(result.permanent()).isTrue();
    }'''
))

# CreateListItem property test
w("CreateListItemConnectorPropertyTest.java", gen_property_test(
    "CreateListItem",
    {"listId": '"list-123"', "fields": '"{\\"Title\\": \\"Test\\"}"'},
    {},
    '''    @Property(tries = 1)
    void createListItemResultStoresValues() {
        var result = new CreateListItemResult("li-1", "https://sp.com/item", "2024-01-01T00:00:00Z");
        assertThat(result.listItemId()).isEqualTo("li-1");
        assertThat(result.listItemWebUrl()).isEqualTo("https://sp.com/item");
        assertThat(result.createdDateTime()).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Property
    void listIdAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String listId) {
        var connector = new CreateListItemConnector();
        var inputs = validInputs();
        inputs.put("listId", listId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void fieldsAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String fields) {
        var connector = new CreateListItemConnector();
        var inputs = validInputs();
        inputs.put("fields", fields);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }'''
))

# GetListItem property test
w("GetListItemConnectorPropertyTest.java", gen_property_test(
    "GetListItem",
    {"listId": '"list-123"', "listItemId": '"li-123"'},
    {},
    '''    @Property(tries = 1)
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
    }'''
))

# UpdateListItem property test
w("UpdateListItemConnectorPropertyTest.java", gen_property_test(
    "UpdateListItem",
    {"listId": '"list-123"', "listItemId": '"li-123"', "fields": '"{\\"Title\\": \\"Updated\\"}"'},
    {},
    '''    @Property(tries = 1)
    void updateListItemResultStoresValues() {
        var result = new UpdateListItemResult("li-1", "2024-01-02T12:00:00Z");
        assertThat(result.listItemId()).isEqualTo("li-1");
        assertThat(result.lastModifiedDateTime()).isEqualTo("2024-01-02T12:00:00Z");
    }

    @Property
    void listIdAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String listId) {
        var connector = new UpdateListItemConnector();
        var inputs = validInputs();
        inputs.put("listId", listId);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }

    @Property
    void fieldsAcceptsAnyNonBlank(@ForAll("nonBlankStrings") String fields) {
        var connector = new UpdateListItemConnector();
        var inputs = validInputs();
        inputs.put("fields", fields);
        connector.setInputParameters(inputs);
        assertThatCode(() -> connector.validateInputParameters()).doesNotThrowAnyException();
    }'''
))

# ================================================================
# INTEGRATION TEST GENERATION
# ================================================================

print("\n=== Generating Integration Tests ===")

def gen_integration_test(op, extra_env_inputs, extra_assertions):
    return f'''package com.bonitasoft.connectors.sharepoint;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;

@EnabledIfEnvironmentVariable(named = "SHAREPOINT_TENANT_ID", matches = ".+")
class {op}ConnectorIntegrationTest {{

    @Test
    void shouldExecuteAgainstRealApi() throws Exception {{
        var connector = new {op}Connector();
        var inputs = new HashMap<String, Object>();
        inputs.put("tenantId", System.getenv("SHAREPOINT_TENANT_ID"));
        inputs.put("clientId", System.getenv("SHAREPOINT_CLIENT_ID"));
        inputs.put("clientSecret", System.getenv("SHAREPOINT_CLIENT_SECRET"));
        inputs.put("siteId", System.getenv("SHAREPOINT_SITE_ID"));
{extra_env_inputs}
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();

        assertThat(connector.getOutputParameters().get("success")).isEqualTo(true);
{extra_assertions}
        connector.disconnect();
    }}
}}
'''

w("UploadFileConnectorIntegrationTest.java", gen_integration_test(
    "UploadFile",
    '        inputs.put("fileName", "integration-test-" + System.currentTimeMillis() + ".txt");\n        inputs.put("fileContentBase64", java.util.Base64.getEncoder().encodeToString("Integration test content".getBytes()));\n        inputs.put("conflictBehavior", "rename");',
    '        assertThat(connector.getOutputParameters().get("itemId")).isNotNull();\n        assertThat(connector.getOutputParameters().get("itemWebUrl")).isNotNull();',
))

w("DownloadFileConnectorIntegrationTest.java", gen_integration_test(
    "DownloadFile",
    '        inputs.put("itemId", System.getenv("SHAREPOINT_TEST_ITEM_ID"));',
    '        assertThat(connector.getOutputParameters().get("fileContentBase64")).isNotNull();\n        assertThat(connector.getOutputParameters().get("fileName")).isNotNull();',
))

w("CreateFolderConnectorIntegrationTest.java", gen_integration_test(
    "CreateFolder",
    '        inputs.put("folderName", "integration-test-" + System.currentTimeMillis());\n        inputs.put("conflictBehavior", "rename");',
    '        assertThat(connector.getOutputParameters().get("folderId")).isNotNull();\n        assertThat(connector.getOutputParameters().get("folderWebUrl")).isNotNull();',
))

w("ListChildrenConnectorIntegrationTest.java", gen_integration_test(
    "ListChildren",
    '        inputs.put("maxResults", 10);',
    '        assertThat(connector.getOutputParameters().get("items")).isNotNull();\n        assertThat(connector.getOutputParameters().get("totalCount")).isNotNull();',
))

w("DeleteItemConnectorIntegrationTest.java", gen_integration_test(
    "DeleteItem",
    '        inputs.put("itemId", System.getenv("SHAREPOINT_TEST_DELETE_ITEM_ID"));',
    '        assertThat(connector.getOutputParameters().get("deletedItemId")).isNotNull();',
))

w("CreateListItemConnectorIntegrationTest.java", gen_integration_test(
    "CreateListItem",
    '        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));\n        inputs.put("fields", "{\\"Title\\": \\"Integration Test " + System.currentTimeMillis() + "\\"}");',
    '        assertThat(connector.getOutputParameters().get("listItemId")).isNotNull();',
))

w("GetListItemConnectorIntegrationTest.java", gen_integration_test(
    "GetListItem",
    '        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));\n        inputs.put("listItemId", System.getenv("SHAREPOINT_TEST_LIST_ITEM_ID"));',
    '        assertThat(connector.getOutputParameters().get("fields")).isNotNull();\n        assertThat(connector.getOutputParameters().get("listItemId")).isNotNull();',
))

w("UpdateListItemConnectorIntegrationTest.java", gen_integration_test(
    "UpdateListItem",
    '        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));\n        inputs.put("listItemId", System.getenv("SHAREPOINT_TEST_LIST_ITEM_ID"));\n        inputs.put("fields", "{\\"Title\\": \\"Updated " + System.currentTimeMillis() + "\\"}");',
    '        assertThat(connector.getOutputParameters().get("listItemId")).isNotNull();\n        assertThat(connector.getOutputParameters().get("lastModifiedDateTime")).isNotNull();',
))

# ================================================================
# ConnectorTestToolkit.java
# ================================================================

print("\n=== Generating ConnectorTestToolkit ===")

w("ConnectorTestToolkit.java", '''package com.bonitasoft.connectors.sharepoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.actorMapping.Actor;
import org.bonitasoft.engine.bpm.bar.actorMapping.ActorMapping;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.web.client.BonitaClient;
import org.bonitasoft.web.client.api.ProcessInstanceVariableApi;
import org.bonitasoft.web.client.model.ProcessInstantiationResponse;
import org.bonitasoft.web.client.services.policies.ProcessImportPolicy;

/**
 * Helper for testing connectors in a Docker Bonita instance.
 */
public class ConnectorTestToolkit {

    private static final String ARTIFACT_ID = "bonita-connector-sharepoint";

    /**
     * Build a connector and install it into a dummy process with input and output process variables.
     */
    public static BusinessArchive buildConnectorToTest(String connectorId, String versionId,
            Map<String, String> inputs, Map<String, Output> outputs) throws Exception {
        var process = buildConnectorInProcess(connectorId, versionId, inputs, outputs);
        return buildBusinessArchive(process, connectorId, ARTIFACT_ID);
    }

    private static BusinessArchive buildBusinessArchive(DesignProcessDefinition process, String connectorId,
            String artifactId) throws Exception {
        var barBuilder = new BusinessArchiveBuilder();
        barBuilder.createNewBusinessArchive();
        barBuilder.setProcessDefinition(process);

        var foundFiles = new File("").getAbsoluteFile().toPath()
                .resolve("target")
                .toFile()
                .listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return Pattern.matches(artifactId + "-.*.jar", name)
                                && !name.endsWith("-sources.jar")
                                && !name.endsWith("-javadoc.jar");
                    }
                });

        assertThat(foundFiles).hasSize(1);
        var connectorJar = foundFiles[0];
        assertThat(connectorJar).exists();

        List<JarEntry> jarEntries = findJarEntries(connectorJar,
                entry -> entry.getName().equals(connectorId + ".impl"));
        assertThat(jarEntries).hasSize(1);
        var implEntry = jarEntries.get(0);

        byte[] content;
        try (JarFile jarFile = new JarFile(connectorJar)) {
            InputStream inputStream = jarFile.getInputStream(implEntry);
            content = inputStream.readAllBytes();
        }

        barBuilder.addConnectorImplementation(
                new BarResource(connectorId + ".impl", content));
        barBuilder.addClasspathResource(
                new BarResource(connectorJar.getName(), Files.readAllBytes(connectorJar.toPath())));

        ActorMapping actorMapping = new ActorMapping();
        var systemActor = new Actor("system");
        systemActor.addRole("member");
        actorMapping.addActor(systemActor);
        barBuilder.setActorMapping(actorMapping);

        return barBuilder.done();
    }

    private static DesignProcessDefinition buildConnectorInProcess(String connectorId, String versionId,
            Map<String, String> inputs, Map<String, Output> outputs) throws Exception {
        var processBuilder = new ProcessDefinitionBuilder();
        var expBuilder = new ExpressionBuilder();
        processBuilder.createNewInstance("PROCESS_UNDER_TEST", "1.0");
        processBuilder.addActor("system");
        var connectorBuilder = processBuilder.addConnector("connector-under-test", connectorId, versionId,
                ConnectorEvent.ON_ENTER);

        inputs.forEach((name, value) -> {
            try {
                connectorBuilder.addInput(name, expBuilder.createConstantStringExpression(value));
            } catch (InvalidExpressionException e) {
                throw new RuntimeException(e);
            }
        });

        if (outputs != null) {
            outputs.forEach((name, output) -> {
                try {
                    processBuilder.addData(name, output.getType(), null);
                    connectorBuilder.addOutput(new OperationBuilder().createSetDataOperation(name,
                            new ExpressionBuilder().createDataExpression(output.getName(), output.getType())));
                } catch (InvalidExpressionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        processBuilder.addUserTask("waiting task", "system");

        return processBuilder.done();
    }

    /**
     * Import the BusinessArchive and launch the process containing the connector.
     */
    public static ProcessInstantiationResponse importAndLaunchProcess(BusinessArchive barArchive, BonitaClient client)
            throws IOException {
        var process = barArchive.getProcessDefinition();
        File processFile = null;
        try {
            processFile = Files.createTempFile("process", ".bar").toFile();
            processFile.delete();
            BusinessArchiveFactory.writeBusinessArchiveToFile(barArchive, processFile);
            client.login("install", "install");
            client.processes().importProcess(processFile, ProcessImportPolicy.REPLACE_DUPLICATES);
        } finally {
            if (processFile != null) {
                processFile.delete();
            }
        }

        var processId = client.processes().getProcess(process.getName(), process.getVersion()).getId();
        return client.processes().startProcess(processId, Map.of());
    }

    /**
     * Get the value of a process variable.
     */
    public static String getProcessVariableValue(BonitaClient client, String caseId, String variableProcessName) {
        return client.get(ProcessInstanceVariableApi.class)
                .getVariableByProcessInstanceId(caseId, variableProcessName)
                .getValue();
    }

    private static List<JarEntry> findJarEntries(File file, Predicate<? super JarEntry> entryPredicate)
            throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.stream()
                    .filter(entryPredicate)
                    .collect(Collectors.toList());
        }
    }

    static class Output {
        private final String name;
        private final String type;

        public static Output create(String name, String type) {
            return new Output(name, type);
        }

        private Output(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}
''')

# ================================================================
# SharePointConnectorIT.java
# ================================================================

print("\n=== Generating SharePointConnectorIT ===")

w("SharePointConnectorIT.java", '''package com.bonitasoft.connectors.sharepoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bonitasoft.web.client.BonitaClient;
import org.bonitasoft.web.client.api.ArchivedProcessInstanceApi;
import org.bonitasoft.web.client.api.ProcessInstanceApi;
import org.bonitasoft.web.client.exception.NotFoundException;
import org.bonitasoft.web.client.model.ArchivedProcessInstance;
import org.bonitasoft.web.client.services.policies.OrganizationImportPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Process-based integration tests for SharePoint connectors.
 * Deploys connectors to a Docker Bonita instance and verifies execution.
 */
@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "SHAREPOINT_TENANT_ID", matches = ".+")
class SharePointConnectorIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharePointConnectorIT.class);

    // Connector definition IDs and versions
    private static final String UPLOAD_FILE_DEF_ID = "sharepoint-upload-file";
    private static final String UPLOAD_FILE_DEF_VERSION = "1.0.0";
    private static final String DOWNLOAD_FILE_DEF_ID = "sharepoint-download-file";
    private static final String DOWNLOAD_FILE_DEF_VERSION = "1.0.0";
    private static final String CREATE_FOLDER_DEF_ID = "sharepoint-create-folder";
    private static final String CREATE_FOLDER_DEF_VERSION = "1.0.0";
    private static final String LIST_CHILDREN_DEF_ID = "sharepoint-list-children";
    private static final String LIST_CHILDREN_DEF_VERSION = "1.0.0";
    private static final String DELETE_ITEM_DEF_ID = "sharepoint-delete-item";
    private static final String DELETE_ITEM_DEF_VERSION = "1.0.0";
    private static final String CREATE_LIST_ITEM_DEF_ID = "sharepoint-create-list-item";
    private static final String CREATE_LIST_ITEM_DEF_VERSION = "1.0.0";
    private static final String GET_LIST_ITEM_DEF_ID = "sharepoint-get-list-item";
    private static final String GET_LIST_ITEM_DEF_VERSION = "1.0.0";
    private static final String UPDATE_LIST_ITEM_DEF_ID = "sharepoint-update-list-item";
    private static final String UPDATE_LIST_ITEM_DEF_VERSION = "1.0.0";

    @Container
    static GenericContainer<?> BONITA_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("bonita:10.2.0"))
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/bonita"))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    private BonitaClient client;

    @BeforeAll
    static void installOrganization() {
        var client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
        client.users().importOrganization(
                new File(SharePointConnectorIT.class.getResource("/ACME.xml").getFile()),
                OrganizationImportPolicy.IGNORE_DUPLICATES);
        client.logout();
    }

    @BeforeEach
    void login() {
        client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
    }

    @AfterEach
    void logout() {
        client.logout();
    }

    @Test
    void testUploadFileConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("fileName", "process-test-" + System.currentTimeMillis() + ".txt");
        inputs.put("fileContentBase64", java.util.Base64.getEncoder().encodeToString("test content".getBytes()));

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultItemId", ConnectorTestToolkit.Output.create("itemId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                UPLOAD_FILE_DEF_ID, UPLOAD_FILE_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testDownloadFileConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("itemId", System.getenv("SHAREPOINT_TEST_ITEM_ID"));

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultFileName", ConnectorTestToolkit.Output.create("fileName", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                DOWNLOAD_FILE_DEF_ID, DOWNLOAD_FILE_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testCreateFolderConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("folderName", "process-test-" + System.currentTimeMillis());
        inputs.put("conflictBehavior", "rename");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultFolderId", ConnectorTestToolkit.Output.create("folderId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                CREATE_FOLDER_DEF_ID, CREATE_FOLDER_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testListChildrenConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("maxResults", "10");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultTotalCount", ConnectorTestToolkit.Output.create("totalCount", Integer.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                LIST_CHILDREN_DEF_ID, LIST_CHILDREN_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testDeleteItemConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("itemId", System.getenv("SHAREPOINT_TEST_DELETE_ITEM_ID"));

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultDeletedItemId", ConnectorTestToolkit.Output.create("deletedItemId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                DELETE_ITEM_DEF_ID, DELETE_ITEM_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testCreateListItemConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));
        inputs.put("fields", "{\\"Title\\": \\"Process Test " + System.currentTimeMillis() + "\\"}");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultListItemId", ConnectorTestToolkit.Output.create("listItemId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                CREATE_LIST_ITEM_DEF_ID, CREATE_LIST_ITEM_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testGetListItemConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));
        inputs.put("listItemId", System.getenv("SHAREPOINT_TEST_LIST_ITEM_ID"));

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultListItemId", ConnectorTestToolkit.Output.create("listItemId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                GET_LIST_ITEM_DEF_ID, GET_LIST_ITEM_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void testUpdateListItemConnector() throws Exception {
        var inputs = commonInputs();
        inputs.put("listId", System.getenv("SHAREPOINT_TEST_LIST_ID"));
        inputs.put("listItemId", System.getenv("SHAREPOINT_TEST_LIST_ITEM_ID"));
        inputs.put("fields", "{\\"Title\\": \\"Updated " + System.currentTimeMillis() + "\\"}");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultListItemId", ConnectorTestToolkit.Output.create("listItemId", String.class.getName()));

        var barFile = ConnectorTestToolkit.buildConnectorToTest(
                UPDATE_LIST_ITEM_DEF_ID, UPDATE_LIST_ITEM_DEF_VERSION, inputs, outputs);
        var processResponse = ConnectorTestToolkit.importAndLaunchProcess(barFile, client);

        await().until(pollInstanceState(processResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client,
                processResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    private Map<String, String> commonInputs() {
        var inputs = new HashMap<String, String>();
        inputs.put("tenantId", System.getenv("SHAREPOINT_TENANT_ID"));
        inputs.put("clientId", System.getenv("SHAREPOINT_CLIENT_ID"));
        inputs.put("clientSecret", System.getenv("SHAREPOINT_CLIENT_SECRET"));
        inputs.put("siteId", System.getenv("SHAREPOINT_SITE_ID"));
        return inputs;
    }

    private Callable<String> pollInstanceState(String id) {
        return () -> {
            try {
                var instance = client.get(ProcessInstanceApi.class)
                        .getProcessInstanceById(id, (String) null);
                return instance.getState().name().toLowerCase();
            } catch (NotFoundException e) {
                var archived = getCompletedProcess(id);
                return archived != null ? archived.getState().name().toLowerCase() : "unknown";
            }
        };
    }

    private ArchivedProcessInstance getCompletedProcess(String id) {
        var archivedInstances = client.get(ArchivedProcessInstanceApi.class)
                .searchArchivedProcessInstances(
                        new ArchivedProcessInstanceApi.SearchArchivedProcessInstancesQueryParams()
                                .c(1)
                                .p(0)
                                .f(List.of("caller=any", "sourceObjectId=" + id)));
        if (!archivedInstances.isEmpty()) {
            return archivedInstances.get(0);
        }
        return null;
    }
}
''')

print("\n=== ALL FILES GENERATED ===")
