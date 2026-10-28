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
class DeviceCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void crud_ShouldPersistDevicesWithAndWithoutOwnerAndSupportFiltering() throws Exception {
        String token = bearerToken();
        UUID ownerId = userId(SeedData.OWNER_EMAIL);

        UUID unownedDeviceId = createDevice(token, null, "Unowned CRUD Lamp", "LAMP");
        assertThat(jdbc.sql("select count(*) from devices where id = ? and name = ? and type = ?")
                .param(unownedDeviceId).param("Unowned CRUD Lamp").param("LAMP")
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(accessCount(unownedDeviceId)).isZero();

        UUID ownedDeviceId = createDevice(token, ownerId, "Owned CRUD Kettle", "KETTLE");
        assertThat(jdbc.sql("select count(*) from devices where id = ? and name = ? and type = ?")
                .param(ownedDeviceId).param("Owned CRUD Kettle").param("KETTLE")
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from device_accesses where device_id = ? and user_id = ? and role = ?")
                .param(ownedDeviceId).param(ownerId).param("OWNER")
                .query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(get("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(unownedDeviceId.toString())))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(ownedDeviceId.toString())));
        assertThat(jdbc.sql("select count(*) from devices where id in (?, ?)")
                .param(unownedDeviceId).param(ownedDeviceId).query(Integer.class).single()).isEqualTo(2);

        mockMvc.perform(get("/api/devices").queryParam("type", "LAMP")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(unownedDeviceId.toString())))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(ownedDeviceId.toString()))));
        assertThat(jdbc.sql("select count(*) from devices where id = ? and type = 'LAMP'")
                .param(unownedDeviceId).query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(get("/api/devices/{id}", ownedDeviceId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Owned CRUD Kettle"))
                .andExpect(jsonPath("$.type").value("KETTLE"));
        assertThat(jdbc.sql("select count(*) from devices where id = ? and name = ? and type = ?")
                .param(ownedDeviceId).param("Owned CRUD Kettle").param("KETTLE")
                .query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(put("/api/devices/{id}", ownedDeviceId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed CRUD Kettle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed CRUD Kettle"));
        assertThat(jdbc.sql("select count(*) from devices where id = ? and name = ?")
                .param(ownedDeviceId).param("Renamed CRUD Kettle")
                .query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(delete("/api/devices/{id}", ownedDeviceId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from devices where id = ?")
                .param(ownedDeviceId).query(Integer.class).single()).isZero();
        assertThat(accessCount(ownedDeviceId)).isZero();

        mockMvc.perform(delete("/api/devices/{id}", unownedDeviceId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from devices where id = ?")
                .param(unownedDeviceId).query(Integer.class).single()).isZero();
        assertThat(accessCount(unownedDeviceId)).isZero();
    }

    @Test
    void invalidDeviceCreation_ShouldReturnBadRequestWithoutChangingRowCounts() throws Exception {
        String token = bearerToken();
        UUID ownerId = userId(SeedData.OWNER_EMAIL);
        int initialDeviceCount = deviceCount();
        int initialAccessCount = accessCount();

        mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"LAMP\"}"))
                .andExpect(status().isBadRequest());
        assertRowCounts(initialDeviceCount, initialAccessCount);

        mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing Type Device\"}"))
                .andExpect(status().isBadRequest());
        assertRowCounts(initialDeviceCount, initialAccessCount);
    }

    @Test
    void unknownDeviceOperations_ShouldReturnNotFoundWithoutChangingRowCounts() throws Exception {
        String token = bearerToken();
        UUID unknownId = UUID.randomUUID();
        int initialDeviceCount = deviceCount();
        int initialAccessCount = accessCount();

        mockMvc.perform(get("/api/devices/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        assertRowCounts(initialDeviceCount, initialAccessCount);

        mockMvc.perform(put("/api/devices/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unknown Device\"}"))
                .andExpect(status().isNotFound());
        assertRowCounts(initialDeviceCount, initialAccessCount);

        mockMvc.perform(delete("/api/devices/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        assertRowCounts(initialDeviceCount, initialAccessCount);
    }

    private UUID createDevice(String token, UUID ownerId, String name, String type) throws Exception {
        var request = post("/api/devices")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}");
        if (ownerId != null) {
            request.header("X-User-Id", ownerId);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
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

    private int deviceCount() {
        return jdbc.sql("select count(*) from devices").query(Integer.class).single();
    }

    private int accessCount() {
        return jdbc.sql("select count(*) from device_accesses").query(Integer.class).single();
    }

    private int accessCount(UUID deviceId) {
        return jdbc.sql("select count(*) from device_accesses where device_id = ?")
                .param(deviceId).query(Integer.class).single();
    }

    private void assertRowCounts(int expectedDeviceCount, int expectedAccessCount) {
        assertThat(deviceCount()).isEqualTo(expectedDeviceCount);
        assertThat(accessCount()).isEqualTo(expectedAccessCount);
    }
}
