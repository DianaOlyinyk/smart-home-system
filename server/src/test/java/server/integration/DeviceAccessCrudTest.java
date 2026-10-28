package server.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import server.support.IntegrationTest;
import server.support.SeedData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DeviceAccessCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void grantListAndRevoke_ShouldPersistEachOperation() throws Exception {
        String ownerToken = bearerToken(SeedData.OWNER_EMAIL);
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID guestId = userId(SeedData.GUEST_EMAIL);
        UUID deviceId = createOwnedDevice(ownerToken, ownerId);
        assertThat(jdbc.sql("select count(*) from devices where id = ?")
                .param(deviceId).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from device_accesses where device_id = ? and user_id = ? and role = 'OWNER'")
                .param(deviceId).param(ownerId).query(Integer.class).single()).isEqualTo(1);

        String grantBody = grant(ownerToken, deviceId, SeedData.GUEST_EMAIL, ownerId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.userId").value(guestId.toString()))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andReturn().getResponse().getContentAsString();
        UUID accessId = UUID.fromString(JsonPath.read(grantBody, "$.id"));
        assertThat(jdbc.sql("select count(*) from device_accesses where id = ? and device_id = ? and user_id = ? and role = ? and granted_by = ?")
                .param(accessId).param(deviceId).param(guestId).param("GUEST").param(ownerId)
                .query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(get("/api/devices/{id}/accesses", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + accessId + "')].userId")
                        .value(org.hamcrest.Matchers.contains(guestId.toString())));
        assertThat(jdbc.sql("select count(*) from device_accesses where id = ? and device_id = ? and user_id = ? and role = 'GUEST'")
                .param(accessId).param(deviceId).param(guestId)
                .query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(delete("/api/devices/{id}/accesses/{userId}", deviceId, guestId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from device_accesses where id = ?")
                .param(accessId).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from device_accesses where device_id = ? and user_id = ? and role = 'OWNER'")
                .param(deviceId).param(ownerId).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void accessGrantFailures_ShouldNotChangeAccessCount() throws Exception {
        String ownerToken = bearerToken(SeedData.OWNER_EMAIL);
        String guestToken = bearerToken(SeedData.GUEST_EMAIL);
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        UUID deviceId = createOwnedDevice(ownerToken, ownerId);

        int initialCount = accessCount();
        grant(ownerToken, deviceId, SeedData.GUEST_EMAIL, ownerId)
                .andExpect(status().isCreated());
        assertThat(accessCount()).isEqualTo(initialCount + 1);

        int countAfterGrant = accessCount();
        grant(ownerToken, deviceId, SeedData.GUEST_EMAIL, ownerId)
                .andExpect(status().isConflict());
        assertThat(accessCount()).isEqualTo(countAfterGrant);

        mockMvc.perform(post("/api/devices/{id}/accesses", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantJson("missing-access-user@example.com", ownerId)))
                .andExpect(status().isNotFound());
        assertThat(accessCount()).isEqualTo(countAfterGrant);

        mockMvc.perform(post("/api/devices/{id}/accesses", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantJson(SeedData.STRANGER_EMAIL, ownerId)))
                .andExpect(status().isForbidden());
        assertThat(accessCount()).isEqualTo(countAfterGrant);
    }

    private ResultActions grant(String token, UUID deviceId, String email, UUID grantedById) throws Exception {
        return mockMvc.perform(post("/api/devices/{id}/accesses", deviceId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantJson(email, grantedById)));
    }

    private String grantJson(String email, UUID grantedById) {
        return "{\"email\":\"" + email + "\",\"role\":\"GUEST\",\"grantedById\":\"" + grantedById + "\"}";
    }

    private UUID createOwnedDevice(String token, UUID ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Access CRUD Lamp\",\"type\":\"LAMP\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
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

    private int accessCount() {
        return jdbc.sql("select count(*) from device_accesses").query(Integer.class).single();
    }
}
