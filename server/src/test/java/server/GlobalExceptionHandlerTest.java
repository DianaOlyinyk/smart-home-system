package server;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import server.support.IntegrationTest;
import server.support.SeedData;
import server.users.UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void validationErrorUsesProjectFormatWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"LAMP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Помилка валідації вхідних даних"))
                .andExpect(jsonPath("$.errors.name").value("Назва пристрою є обов'язковою"));
    }

    @Test
    void unreadableBodyUsesProjectFormat() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Некоректне тіло запиту"));
    }

    @Test
    void typeMismatchNamesTheParameter() throws Exception {
        mockMvc.perform(get("/api/devices").param("type", "SPACESHIP")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Некоректний параметр запиту"))
                .andExpect(jsonPath("$.detail").value("Параметр 'type' має неправильний формат"));
    }

    @Test
    void unknownRouteUsesProjectFormat() throws Exception {
        mockMvc.perform(get("/no-such-route")
                        .with(user(userService.getUserByEmail(SeedData.OWNER_EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ресурс не знайдено"));
    }

    @Test
    void unsupportedMethodUsesProjectFormatAndKeepsAllowHeader() throws Exception {
        mockMvc.perform(delete("/api/devices")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.title").value("Метод не підтримується"));
    }

    @Test
    void accessDeniedUsesForbiddenProblemDetail() {
        ProblemDetail problemDetail = new GlobalExceptionHandler()
                .handleAccessDenied(new AccessDeniedException("Access is denied"));

        assertEquals(403, problemDetail.getStatus());
        assertEquals("Доступ заборонено", problemDetail.getTitle());
        assertEquals("Недостатньо прав для виконання цієї операції.", problemDetail.getDetail());
    }

    private String bearerToken() throws Exception {
        String body = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SeedData.OWNER_EMAIL + "\",\"password\":\""
                                + SeedData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
