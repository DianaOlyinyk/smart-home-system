package server.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.User;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DeviceFormController.class)
class DeviceFormControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void formShowsFieldsWithCsrfToken() throws Exception {
        mockMvc.perform(get("/devices/new").with(user(account(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(view().name("devices/new"))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("value=\"COFFEE_MACHINE\"")));
    }

    @Test
    void createMakesCurrentUserOwnerAndRedirectsWithFlash() throws Exception {
        User olena = account(UUID.randomUUID());
        when(deviceService.create("Лампа", DeviceType.LAMP, olena.getId()))
                .thenReturn(new Device("Лампа", DeviceType.LAMP, "token-123", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/devices").with(user(olena)).with(csrf())
                        .param("name", " Лампа ")
                        .param("type", "LAMP"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices"))
                .andExpect(flash().attribute("success", "Пристрій «Лампа» додано"));

        verify(deviceService).create("Лампа", DeviceType.LAMP, olena.getId());
    }

    @Test
    void createWithBlankNameShowsFormWithError() throws Exception {
        mockMvc.perform(post("/devices").with(user(account(UUID.randomUUID()))).with(csrf())
                        .param("name", " ")
                        .param("type", "LAMP"))
                .andExpect(status().isOk())
                .andExpect(view().name("devices/new"))
                .andExpect(model().attributeHasFieldErrors("form", "name"))
                .andExpect(content().string(containsString("Вкажіть назву пристрою")))
                .andExpect(content().string(containsString("has-error")));

        verify(deviceService, never()).create(any(), any(), any());
    }

    @Test
    void createWithoutTypeShowsFormWithErrorAndKeepsName() throws Exception {
        mockMvc.perform(post("/devices").with(user(account(UUID.randomUUID()))).with(csrf())
                        .param("name", "Лампа")
                        .param("type", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "type"))
                .andExpect(content().string(containsString("Оберіть тип пристрою")))
                .andExpect(content().string(containsString("value=\"Лампа\"")));

        verify(deviceService, never()).create(any(), any(), any());
    }

    @Test
    void createWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/devices").with(user(account(UUID.randomUUID())))
                        .param("name", "Лампа")
                        .param("type", "LAMP"))
                .andExpect(status().isForbidden());

        verify(deviceService, never()).create(any(), any(), any());
    }

    private static User account(UUID id) {
        User user = new User("olena@example.com", "hash", "Олена", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
