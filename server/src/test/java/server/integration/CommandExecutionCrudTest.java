package server.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import server.support.IntegrationTest;
import server.support.SeedData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CommandExecutionCrudTest {

    private static final String EMPTY_SCHEMA = "{\"type\":\"object\",\"additionalProperties\":false}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void executeAndManageExecution_ShouldPersistSuccessAndExecutor() throws Exception {
        String ownerToken = bearerToken();
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createDevice(ownerToken, ownerId, "Execution CRUD Lamp");
        UUID commandId = createCommand(ownerToken, deviceId, "turn_on", EMPTY_SCHEMA);

        String executionBody = mockMvc.perform(post(executionPath(deviceId, commandId))
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"args\":{}}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.executedById").value(ownerId.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID executionId = UUID.fromString(JsonPath.read(executionBody, "$.id"));
        assertExecutionRow(executionId, deviceId, commandId, ownerId, "SUCCESS");

        mockMvc.perform(get("/api/devices/{deviceId}/executions", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + executionId + "')].status")
                        .value(org.hamcrest.Matchers.contains("SUCCESS")));
        assertThat(executionCount()).isEqualTo(1);

        mockMvc.perform(get("/api/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(executionId.toString()))
                .andExpect(jsonPath("$.commandId").value(commandId.toString()))
                .andExpect(jsonPath("$.executedById").value(ownerId.toString()));
        assertExecutionRow(executionId, deviceId, commandId, ownerId, "SUCCESS");

        int initialCount = executionCount();
        mockMvc.perform(patch("/api/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(executionCount()).isEqualTo(initialCount);
        assertThat(jdbc.sql("select status from command_executions where id = ?")
                .param(executionId).query(String.class).single()).isEqualTo("SUCCESS");

        mockMvc.perform(delete("/api/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from command_executions where id = ?")
                .param(executionId).query(Integer.class).single()).isZero();
    }

    @Test
    void executionFailures_ShouldNotCreateRows() throws Exception {
        String ownerToken = bearerToken();
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createDevice(ownerToken, ownerId, "Execution Failure Lamp");
        UUID brightnessCommandId = createCommand(ownerToken, deviceId, "set_brightness",
                "{\"type\":\"object\",\"properties\":{\"brightness\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":100}},\"required\":[\"brightness\"],\"additionalProperties\":false}");
        UUID unsupportedCommandId = createCommand(ownerToken, deviceId, "unsupported_command", EMPTY_SCHEMA);
        int initialCount = executionCount();

        mockMvc.perform(post(executionPath(deviceId, brightnessCommandId))
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"args\":{\"brightness\":101}}"))
                .andExpect(status().isBadRequest());
        assertThat(executionCount()).isEqualTo(initialCount);

        mockMvc.perform(post(executionPath(deviceId, unsupportedCommandId))
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"args\":{}}"))
                .andExpect(status().isBadRequest());
        assertThat(executionCount()).isEqualTo(initialCount);
    }

    @Test
    void unknownExecution_ShouldReturnNotFoundWithoutChangingExecutionCount() throws Exception {
        String ownerToken = bearerToken();
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createDevice(ownerToken, ownerId, "Unknown Execution Lamp");
        int initialCount = executionCount();

        mockMvc.perform(get("/api/devices/{deviceId}/executions/{executionId}", deviceId, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNotFound());
        assertThat(executionCount()).isEqualTo(initialCount);
    }

    private UUID createDevice(String token, UUID ownerId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"LAMP\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID createCommand(String token, UUID deviceId, String name, String schema) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"argsSchema\":\""
                + schema.replace("\"", "\\\"") + "\",\"requiredRole\":\"OWNER\"}";
        MvcResult result = mockMvc.perform(post("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String executionPath(UUID deviceId, UUID commandId) {
        return "/api/devices/" + deviceId + "/commands/" + commandId + "/executions";
    }

    private String bearerToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SeedData.OWNER_EMAIL
                                + "\",\"password\":\"" + SeedData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private UUID userId(String email) {
        return jdbc.sql("select id from users where email = ?")
                .param(email).query(UUID.class).single();
    }

    private int executionCount() {
        return jdbc.sql("select count(*) from command_executions").query(Integer.class).single();
    }

    private void assertExecutionRow(UUID executionId, UUID deviceId, UUID commandId, UUID ownerId, String status) {
        assertThat(jdbc.sql("select count(*) from command_executions where id = ? and device_id = ? and command_id = ? and executed_by_id = ? and status = ?")
                .param(executionId).param(deviceId).param(commandId).param(ownerId).param(status)
                .query(Integer.class).single()).isEqualTo(1);
    }
}
