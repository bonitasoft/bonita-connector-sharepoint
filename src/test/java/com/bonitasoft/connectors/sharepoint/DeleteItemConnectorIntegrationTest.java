package com.bonitasoft.connectors.sharepoint;

import static com.bonitasoft.connectors.sharepoint.ConnectorTestToolkit.getOutput;
import static com.bonitasoft.connectors.sharepoint.ConnectorTestToolkit.getOutputs;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;

@EnabledIfEnvironmentVariable(named = "SHAREPOINT_TENANT_ID", matches = ".+")
class DeleteItemConnectorIntegrationTest {

    @Test
    void shouldExecuteAgainstRealApi() throws Exception {
        var connector = new DeleteItemConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("tenantId", System.getenv("SHAREPOINT_TENANT_ID"));
        inputs.put("clientId", System.getenv("SHAREPOINT_CLIENT_ID"));
        inputs.put("clientSecret", System.getenv("SHAREPOINT_CLIENT_SECRET"));
        inputs.put("siteId", System.getenv("SHAREPOINT_SITE_ID"));
        inputs.put("itemId", System.getenv("SHAREPOINT_TEST_DELETE_ITEM_ID"));
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "deletedItemId")).isNotNull();
        connector.disconnect();
    }
}
