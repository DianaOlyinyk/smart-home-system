package server.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import server.support.IntegrationTest;
import server.devices.Device;
import server.devices.DeviceService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class WebSecurityTest {

    private static final String PASSWORD = "Passw0rd123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceService deviceService;

    @Test
    void anonymousUserIsRedirectedToLoginFromEveryPage() throws Exception {
        List<String> pages = List.of("/devices", "/devices/new", "/devices/" + UUID.randomUUID());

        for (String page : pages) {
            mockMvc.perform(get(page))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void wrongPasswordRedirectsBackToLoginWithError() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "owner@example.com")
                        .param("password", "wrong-password"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "owner@example.com")
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        MockHttpSession owner = loginAs("owner@example.com");

        mockMvc.perform(post("/devices").session(owner)
                        .param("name", "No token lamp")
                        .param("type", "LAMP"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validPostCreatesDeviceAndRedirectsToList() throws Exception {
        MockHttpSession owner = loginAs("owner@example.com");

        mockMvc.perform(post("/devices").session(owner).with(csrf())
                        .param("name", "Security test lamp")
                        .param("type", "LAMP"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices"));

        mockMvc.perform(get("/devices").session(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Security test lamp")))
                .andExpect(content().string(containsString("alert-success")));

        mockMvc.perform(get("/devices").session(owner))
                .andExpect(content().string(not(containsString("alert-success"))));
    }

    @Test
    void invalidPostReturnsFormWithErrors() throws Exception {
        MockHttpSession owner = loginAs("owner@example.com");

        mockMvc.perform(post("/devices").session(owner).with(csrf())
                        .param("name", " ")
                        .param("type", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Вкажіть назву пристрою")))
                .andExpect(content().string(containsString("Оберіть тип пристрою")))
                .andExpect(content().string(containsString("has-error")));
    }

    @Test
    void scriptInDeviceNameIsEscaped() throws Exception {
        MockHttpSession owner = loginAs("owner@example.com");

        mockMvc.perform(post("/devices").session(owner).with(csrf())
                        .param("name", "<script>alert(1)</script>")
                        .param("type", "LAMP"))
                .andExpect(redirectedUrl("/devices"));

        mockMvc.perform(get("/devices").session(owner))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)"))));
    }

    @Test
    void guestSeesNoAddCommandFormAndCannotPostCommand() throws Exception {
        Device lamp = seedLamp();
        MockHttpSession guest = loginAs("guest@example.com");

        mockMvc.perform(get("/devices/{id}", lamp.getId()).session(guest))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Додати команду"))));

        mockMvc.perform(post("/devices/{id}/commands", lamp.getId()).session(guest).with(csrf())
                        .param("name", "guest_command")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerSeesAddCommandForm() throws Exception {
        Device lamp = seedLamp();
        MockHttpSession owner = loginAs("owner@example.com");

        mockMvc.perform(get("/devices/{id}", lamp.getId()).session(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Додати команду")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void userWithoutAccessGetsNotFoundForForeignDevice() throws Exception {
        Device lamp = seedLamp();
        MockHttpSession stranger = loginAs("stranger@example.com");

        mockMvc.perform(get("/devices/{id}", lamp.getId()).session(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagesAreSentWithSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; img-src 'self' data:; script-src 'self'; style-src 'self'; frame-ancestors 'none'"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void restApiUsesJwtIndependentlyOfWebSession() throws Exception {
        String token = accessToken("owner@example.com");
        MockHttpSession webSession = loginAs("owner@example.com");

        mockMvc.perform(get("/api/devices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/devices").session(webSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/devices").header("Authorization", "Bearer " + token))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void swaggerDocumentationStaysPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("email", email)
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/devices"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Device seedLamp() {
        return deviceService.findAll(null).stream()
                .filter(device -> "Seed Lamp".equals(device.getName()))
                .findFirst()
                .orElseThrow();
    }
}
