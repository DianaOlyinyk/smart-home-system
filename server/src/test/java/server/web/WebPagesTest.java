package server.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import server.support.IntegrationTest;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.UserService;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class WebPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private UserService userService;

    @Test
    void loginPageIsAvailableAndShowsLoginError() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString(
                        "Неправильна адреса електронної пошти або пароль.")));
    }

    @Test
    void loginPageShowsLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Ви успішно вийшли з облікового запису.")));
    }

    @Test
    void notFoundErrorUsesGenericErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(
                        "Сторінку або пристрій не знайдено.")));
    }

    @Test
    void forbiddenErrorUsesGenericErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(
                        "У вас немає прав для виконання цієї дії.")));
    }

    @Test
    void serverErrorUsesGenericErrorPage() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 503))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString(
                        "Не вдалося виконати запит. Спробуйте ще раз пізніше.")))
                .andExpect(content().string(containsString("error-page__code\">503<")));
    }

    @Test
    void serverErrorPageShowsFiveHundredByDefault() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("error-page__code\">500<")));

        mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("error-page__code\">500<")));
    }

    @Test
    void ownerCanViewDeviceAndCommandForm() throws Exception {
        Device device = seedDevice();

        mockMvc.perform(get("/devices/{id}", device.getId())
                        .with(user(userService.getUserByEmail("owner@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(device.getName())))
                .andExpect(content().string(containsString("Додати команду")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString(device.getConnectionToken()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("not-displayed"))));
    }

    @Test
    void guestCanViewDeviceButNotCommandForm() throws Exception {
        Device device = seedDevice();

        mockMvc.perform(get("/devices/{id}", device.getId())
                        .with(user(userService.getUserByEmail("guest@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(device.getName())))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Add command"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Додати команду"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString(device.getConnectionToken()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("not-displayed"))));
    }

    @Test
    void guestCannotPostCommandDirectly() throws Exception {
        Device device = seedDevice();
        var guest = userService.getUserByEmail("guest@example.com");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/devices/{id}/commands", device.getId())
                        .with(user(guest))
                        .with(csrf())
                        .param("name", "")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/devices/{id}/commands", device.getId())
                        .with(user(guest))
                        .with(csrf())
                        .param("name", "guest_command")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void foreignAndMissingDevicesReturnNotFound() throws Exception {
        Device foreignDevice = deviceService.create(
                "Foreign device",
                DeviceType.LAMP,
                userService.getUserByEmail("stranger@example.com").getId());

        mockMvc.perform(get("/devices/{id}", foreignDevice.getId())
                        .with(user(userService.getUserByEmail("guest@example.com"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/devices/{id}", UUID.randomUUID())
                        .with(user(userService.getUserByEmail("guest@example.com"))))
                .andExpect(status().isNotFound());
    }

    private Device seedDevice() {
        return deviceService.findAll(null).stream()
                .filter(device -> "Seed Lamp".equals(device.getName()))
                .findFirst()
                .orElseThrow();
    }
}
