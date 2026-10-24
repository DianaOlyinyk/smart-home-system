package server.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.devices.UserDevice;
import server.users.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DeviceListController.class)
class DeviceListControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void listShowsCurrentUsersDevicesWithRoles() throws Exception {
        User olena = account(UUID.randomUUID());
        UUID lampId = UUID.randomUUID();
        Device lamp = device(lampId, "Лампа", DeviceType.LAMP);
        Device kettle = device(UUID.randomUUID(), "Чайник", DeviceType.KETTLE);
        List<UserDevice> devices = List.of(
                new UserDevice(lamp, AccessRole.OWNER),
                new UserDevice(kettle, AccessRole.GUEST));
        when(deviceService.findAllForUser(olena.getId())).thenReturn(devices);

        mockMvc.perform(get("/devices").with(user(olena)))
                .andExpect(status().isOk())
                .andExpect(view().name("devices/list"))
                .andExpect(model().attribute("devices", devices))
                .andExpect(content().string(containsString("Лампа")))
                .andExpect(content().string(containsString("Чайник")))
                .andExpect(content().string(containsString("badge-owner")))
                .andExpect(content().string(containsString("badge-guest")))
                .andExpect(content().string(containsString("01.01.2026")))
                .andExpect(content().string(containsString("/devices/" + lampId)))
                .andExpect(content().string(not(containsString("token-123"))));
    }

    @Test
    void listShowsEmptyStateWhenUserHasNoDevices() throws Exception {
        User olena = account(UUID.randomUUID());
        when(deviceService.findAllForUser(olena.getId())).thenReturn(List.of());

        mockMvc.perform(get("/devices").with(user(olena)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("У вас ще немає пристроїв.")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void listShowsFlashMessage() throws Exception {
        User olena = account(UUID.randomUUID());
        when(deviceService.findAllForUser(olena.getId())).thenReturn(List.of());

        mockMvc.perform(get("/devices").with(user(olena)).flashAttr("success", "Пристрій «Лампа» додано"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-success")))
                .andExpect(content().string(containsString("Пристрій «Лампа» додано")));
    }

    @Test
    void listHasNoFlashMessageByDefault() throws Exception {
        User olena = account(UUID.randomUUID());
        when(deviceService.findAllForUser(olena.getId())).thenReturn(List.of());

        mockMvc.perform(get("/devices").with(user(olena)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("alert-success"))));
    }

    private static User account(UUID id) {
        User user = new User("olena@example.com", "hash", "Олена", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Device device(UUID id, String name, DeviceType type) {
        Device device = new Device(name, type, "token-123", Instant.parse("2026-01-01T12:00:00Z"));
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }
}
