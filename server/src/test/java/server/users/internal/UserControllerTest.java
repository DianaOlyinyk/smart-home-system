package server.users.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.users.EmailAlreadyRegisteredException;
import server.users.UserAccount;
import server.users.UserNotFoundException;
import server.users.UserService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createUserReturns201WithLocationAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        when(userService.register(anyString(), anyString(), anyString()))
                .thenReturn(new UserAccount(id, "user@example.com", "Іван", createdAt));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "supersecret",
                                  "name": "Іван"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("Іван"));
    }

    @Test
    void createUserReturns400WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturns404ProblemDetailWhenUserIsMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(any())).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Користувача з id " + id + " не знайдено"));
    }

    @Test
    void getByIdReturnsExistingAccount() throws Exception {
        UUID id = UUID.randomUUID();
        var account = new UserAccount(id, "olena@example.com", "Олена", Instant.parse("2025-11-02T09:15:00Z"));
        when(userService.findById(id)).thenReturn(account);

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("olena@example.com"))
                .andExpect(jsonPath("$.name").value("Олена"));
    }

    @Test
    void duplicateEmailEndsIn409() throws Exception {
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new EmailAlreadyRegisteredException("olena@example.com"));

        String body = """
                {"email": "olena@example.com", "password": "qwerty123", "name": "Олена"}
                """;

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Користувач з такою поштою вже існує"));
    }

    @Test
    void unknownFieldInPayloadIsRejected() throws Exception {
        String body = """
                {"email": "olena@example.com", "password": "qwerty123", "name": "Олена", "isAdmin": true}
                """;

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
