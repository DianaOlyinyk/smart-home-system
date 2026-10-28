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
class UserCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void crud_ShouldPersistEachOperationAndHashPassword() throws Exception {
        String token = bearerToken(SeedData.OWNER_EMAIL);
        String email = "crud-user@example.com";
        String password = "Passw0rd456";
        String createBody = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(email, password, "CRUD User")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(JsonPath.read(createBody, "$.id"));

        assertThat(jdbc.sql("select count(*) from users where id = ? and email = ? and name = ?")
                .param(userId).param(email).param("CRUD User").query(Integer.class).single()).isEqualTo(1);
        String passwordHash = jdbc.sql("select password_hash from users where id = ?")
                .param(userId).query(String.class).single();
        assertThat(passwordHash).isNotEqualTo(password).isNotBlank();

        mockMvc.perform(get("/api/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("CRUD User"));
        assertThat(jdbc.sql("select count(*) from users where id = ? and email = ? and name = ?")
                .param(userId).param(email).param("CRUD User").query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(get("/api/users").queryParam("query", "crud-user@example.com")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].email").value(email));
        assertThat(jdbc.sql("select count(*) from users where id = ? and email = ?")
                .param(userId).param(email).query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed CRUD User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed CRUD User"));
        assertThat(jdbc.sql("select count(*) from users where id = ? and name = ?")
                .param(userId).param("Renamed CRUD User").query(Integer.class).single()).isEqualTo(1);

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.sql("select count(*) from users where id = ?")
                .param(userId).query(Integer.class).single()).isZero();
    }

    @Test
    void registrationFailures_ShouldNotChangeUserCount() throws Exception {
        int initialCount = userCount();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("not-an-email", "Passw0rd456", "Invalid Email")))
                .andExpect(status().isBadRequest());
        assertThat(userCount()).isEqualTo(initialCount);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("short-password@example.com", "short", "Short Password")))
                .andExpect(status().isBadRequest());
        assertThat(userCount()).isEqualTo(initialCount);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(SeedData.OWNER_EMAIL, SeedData.PASSWORD, "Duplicate User")))
                .andExpect(status().isConflict());
        assertThat(userCount()).isEqualTo(initialCount);
    }

    @Test
    void unknownUserOperations_ShouldReturnNotFoundWithoutChangingUserCount() throws Exception {
        String token = bearerToken(SeedData.OWNER_EMAIL);
        UUID unknownId = UUID.randomUUID();
        int initialCount = userCount();

        mockMvc.perform(get("/api/users/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        assertThat(userCount()).isEqualTo(initialCount);

        mockMvc.perform(put("/api/users/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unknown User\"}"))
                .andExpect(status().isNotFound());
        assertThat(userCount()).isEqualTo(initialCount);

        mockMvc.perform(delete("/api/users/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        assertThat(userCount()).isEqualTo(initialCount);
    }

    private String bearerToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SeedData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private int userCount() {
        return jdbc.sql("select count(*) from users").query(Integer.class).single();
    }

    private String userJson(String email, String password, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"" + name + "\"}";
    }
}
