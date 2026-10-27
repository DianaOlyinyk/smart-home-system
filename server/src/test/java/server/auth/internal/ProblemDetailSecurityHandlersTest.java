package server.auth.internal;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import server.support.IntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@IntegrationTest
class ProblemDetailSecurityHandlersTest {

    @Autowired
    private ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticationFailureWritesTopLevelProblemDetails() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new BadCredentialsException("Authentication required"));

        assertProblemResponse(response, 401, "Помилка автентифікації",
                "Для доступу до ресурсу потрібна валідна автентифікація.");
    }

    @Test
    void accessDeniedWritesTopLevelProblemDetails() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));

        assertProblemResponse(response, 403, "Доступ заборонено",
                "Недостатньо прав для виконання цієї операції.");
    }

    private void assertProblemResponse(MockHttpServletResponse response, int status, String title, String detail)
            throws IOException {
        assertEquals(status, response.getStatus());
        assertEquals("application/problem+json", response.getContentType().split(";", 2)[0]);
        assertEquals("UTF-8", response.getCharacterEncoding());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertNotNull(body.get("status"), "Поле status має бути на верхньому рівні");
        assertNotNull(body.get("title"), "Поле title має бути на верхньому рівні");
        assertNotNull(body.get("detail"), "Поле detail має бути на верхньому рівні");
        assertNotNull(body.get("type"), "Поле type має бути на верхньому рівні");
        assertNotNull(body.get("timestamp"), "Поле timestamp має бути на верхньому рівні");
        assertNotNull(body.get("path"), "Поле path має бути на верхньому рівні");
        assertEquals(status, body.get("status").asInt());
        assertEquals(title, body.get("title").asString());
        assertEquals(detail, body.get("detail").asString());
        assertEquals("/api/devices", body.get("path").asString());
        Instant.parse(body.get("timestamp").asString());
    }
}
