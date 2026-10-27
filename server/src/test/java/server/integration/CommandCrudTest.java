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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CommandCrudTest {

    private static final String EMPTY_SCHEMA = "{\"type\":\"object\",\"additionalProperties\":false}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void crud_ShouldPersistCommandsAndSupportSortedRoleFilteredListing() throws Exception {
        String ownerToken = bearerToken(SeedData.OWNER_EMAIL);
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createDevice(ownerToken, ownerId, "Command CRUD Lamp");

        UUID secondCommandId = createCommand(ownerToken, deviceId, "turn_on", EMPTY_SCHEMA, "OWNER");
        UUID firstCommandId = createCommand(ownerToken, deviceId, "turn_off", EMPTY_SCHEMA, "GUEST");
        assertThat(commandCount()).isEqualTo(2);
        assertCommandRow(firstCommandId, deviceId, "turn_off", EMPTY_SCHEMA, "GUEST");
        assertCommandRow(secondCommandId, deviceId, "turn_on", EMPTY_SCHEMA, "OWNER");

        MvcResult listResult = mockMvc.perform(get("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JsonPath.<String>read(listResult.getResponse().getContentAsString(), "$[0].name"))
                .isEqualTo("turn_off");
        assertThat(JsonPath.<String>read(listResult.getResponse().getContentAsString(), "$[1].name"))
                .isEqualTo("turn_on");
        assertThat(jdbc.sql("select count(*) from command where device_id = ?")
                .param(deviceId).query(Integer.class).single()).isEqualTo(2);

        mockMvc.perform(get("/api/devices/{deviceId}/commands", deviceId)
                        .queryParam("requiredRole", "GUEST")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstCommandId.toString()));
        assertThat(jdbc.sql("select count(*) from command where device_id = ? and required_role = 'GUEST'")
                .param(deviceId).query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(put("/api/devices/{deviceId}/commands/{commandId}", deviceId, secondCommandId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandUpdateJson(
                                "{\"type\":\"object\",\"required\":[\"enabled\"],\"properties\":{\"enabled\":{\"type\":\"boolean\"}},\"additionalProperties\":false}",
                                "GUEST")))
                .andExpect(status().isNoContent());
        String updatedSchema = "{\"type\":\"object\",\"required\":[\"enabled\"],\"properties\":{\"enabled\":{\"type\":\"boolean\"}},\"additionalProperties\":false}";
        assertCommandRow(secondCommandId, deviceId, "turn_on", updatedSchema, "GUEST");

        mockMvc.perform(delete("/api/devices/{deviceId}/commands/{commandId}", deviceId, firstCommandId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from command where id = ?")
                .param(firstCommandId).query(Integer.class).single()).isZero();
        assertThat(commandCount()).isEqualTo(1);
    }

    @Test
    void commandFailures_ShouldReturnErrorsWithoutChangingCommandCount() throws Exception {
        String ownerToken = bearerToken(SeedData.OWNER_EMAIL);
        String guestToken = bearerToken(SeedData.GUEST_EMAIL);
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createDevice(ownerToken, ownerId, "Command Failure Lamp");
        UUID foreignDeviceId = createDevice(ownerToken, ownerId, "Foreign Command Lamp");
        UUID commandId = createCommand(ownerToken, deviceId, "turn_on", EMPTY_SCHEMA, "OWNER");
        int initialCount = commandCount();

        mockMvc.perform(post("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandJson("turn_on", EMPTY_SCHEMA, "OWNER")))
                .andExpect(status().isConflict());
        assertThat(commandCount()).isEqualTo(initialCount);

        mockMvc.perform(post("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"missing_schema\",\"requiredRole\":\"OWNER\"}"))
                .andExpect(status().isBadRequest());
        assertThat(commandCount()).isEqualTo(initialCount);

        mockMvc.perform(post("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandJson("guest_command", EMPTY_SCHEMA, "OWNER")))
                .andExpect(status().isForbidden());
        assertThat(commandCount()).isEqualTo(initialCount);

        mockMvc.perform(put("/api/devices/{deviceId}/commands/{commandId}", foreignDeviceId, commandId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandUpdateJson(EMPTY_SCHEMA, "OWNER")))
                .andExpect(status().isNotFound());
        assertThat(commandCount()).isEqualTo(initialCount);
        assertThat(jdbc.sql("select count(*) from command where id = ? and device_id = ?")
                .param(commandId).param(deviceId).query(Integer.class).single()).isEqualTo(1);
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

    private UUID createCommand(String token, UUID deviceId, String name, String argsSchema, String role)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/devices/{deviceId}/commands", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commandJson(name, argsSchema, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String commandJson(String name, String argsSchema, String role) {
        return "{\"name\":\"" + name + "\",\"argsSchema\":\""
                + argsSchema.replace("\"", "\\\"") + "\",\"requiredRole\":\"" + role + "\"}";
    }

    private String commandUpdateJson(String argsSchema, String role) {
        return "{\"argsSchema\":\"" + argsSchema.replace("\"", "\\\"")
                + "\",\"requiredRole\":\"" + role + "\"}";
    }

    private String bearerToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SeedData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private UUID userId(String email) {
        return jdbc.sql("select id from users where email = ?")
                .param(email).query(UUID.class).single();
    }

    private int commandCount() {
        return jdbc.sql("select count(*) from command").query(Integer.class).single();
    }

    private void assertCommandRow(UUID commandId, UUID deviceId, String name, String schema, String role) {
        assertThat(jdbc.sql("select count(*) from command where id = ? and device_id = ? and name = ? and args_schema = ? and required_role = ?")
                .param(commandId).param(deviceId).param(name).param(schema).param(role)
                .query(Integer.class).single()).isEqualTo(1);
    }
}
