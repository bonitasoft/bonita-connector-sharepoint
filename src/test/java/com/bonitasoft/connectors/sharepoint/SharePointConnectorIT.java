package com.bonitasoft.connectors.sharepoint;

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
        inputs.put("fields", "{\"Title\": \"Process Test " + System.currentTimeMillis() + "\"}");

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
        inputs.put("fields", "{\"Title\": \"Updated " + System.currentTimeMillis() + "\"}");

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
