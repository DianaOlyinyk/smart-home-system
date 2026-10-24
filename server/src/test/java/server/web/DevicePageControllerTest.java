package server.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;
import server.commands.Command;
import server.commands.CommandAlreadyExistsException;
import server.commands.CommandService;
import server.commands.RequiredRole;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.User;
import server.users.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DevicePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class DevicePageControllerTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private CommandService commandService;

    @MockitoBean
    private UserService userService;

    @Test
    void guestSeesOnlyGuestCommandsAndNoCreationForm() throws Exception {
        UUID deviceId = UUID.randomUUID();
        User guest = user(UUID.randomUUID());
        when(userService.getUserByEmail(guest.getEmail())).thenReturn(guest);
        when(deviceService.findById(deviceId)).thenReturn(device(deviceId));
        when(deviceService.findAccessRole(deviceId, guest.getId())).thenReturn(Optional.of(AccessRole.GUEST));
        List<Command> commands = List.of(command(deviceId, "guest_command", RequiredRole.GUEST));
        when(commandService.findAllByDevice(deviceId, RequiredRole.GUEST))
                .thenReturn(commands);

        mockMvc.perform(get("/devices/{id}", deviceId)
                        .principal(guest::getEmail))
                .andExpect(status().isOk())
                .andExpect(view().name("devices/details"))
                .andExpect(model().attribute("isOwner", false))
                .andExpect(model().attribute("commands", commands));

        verify(commandService).findAllByDevice(deviceId, RequiredRole.GUEST);
    }

    @Test
    void ownerSeesAllCommandsAndCreationForm() throws Exception {
        UUID deviceId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        when(userService.getUserByEmail(owner.getEmail())).thenReturn(owner);
        when(deviceService.findById(deviceId)).thenReturn(device(deviceId));
        when(deviceService.findAccessRole(deviceId, owner.getId())).thenReturn(Optional.of(AccessRole.OWNER));
        List<Command> commands = List.of(command(deviceId, "owner_command", RequiredRole.OWNER));
        when(commandService.findAllByDevice(deviceId, null))
                .thenReturn(commands);

        mockMvc.perform(get("/devices/{id}", deviceId)
                        .principal(owner::getEmail))
                .andExpect(status().isOk())
                .andExpect(view().name("devices/details"))
                .andExpect(model().attribute("isOwner", true))
                .andExpect(model().attribute("commands", commands));

        verify(commandService).findAllByDevice(deviceId, null);
    }

    @Test
    void missingDeviceReturnsNotFound() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenThrow(new DeviceNotFoundException(deviceId));

        mockMvc.perform(get("/devices/{id}", deviceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void foreignDeviceReturnsNotFound() throws Exception {
        UUID deviceId = UUID.randomUUID();
        User guest = user(UUID.randomUUID());
        when(userService.getUserByEmail(guest.getEmail())).thenReturn(guest);
        when(deviceService.findById(deviceId)).thenReturn(device(deviceId));
        when(deviceService.findAccessRole(deviceId, guest.getId())).thenReturn(Optional.empty());

        mockMvc.perform(get("/devices/{id}", deviceId).principal(guest::getEmail))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCommandRedirectsBackWithFieldErrors() throws Exception {
        UUID deviceId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/devices/{id}/commands", deviceId)
                        .param("name", " ")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices/" + deviceId))
                .andExpect(flash().attributeExists("commandForm"))
                .andExpect(flash().attributeExists(
                        BindingResult.MODEL_KEY_PREFIX + "commandForm"))
                .andReturn();

        BindingResult bindingResult = (BindingResult) result.getFlashMap().get(
                BindingResult.MODEL_KEY_PREFIX + "commandForm");
        assertTrue(bindingResult.hasFieldErrors("name"));
    }

    @Test
    void duplicateCommandAddsFieldErrorAndRedirects() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.create(deviceId, "existing", "{}", RequiredRole.GUEST))
                .thenThrow(new CommandAlreadyExistsException(deviceId, "existing"));

        MvcResult result = mockMvc.perform(post("/devices/{id}/commands", deviceId)
                        .param("name", "existing")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices/" + deviceId))
                .andExpect(flash().attributeExists(
                        BindingResult.MODEL_KEY_PREFIX + "commandForm"))
                .andReturn();

        BindingResult bindingResult = (BindingResult) result.getFlashMap().get(
                BindingResult.MODEL_KEY_PREFIX + "commandForm");
        assertTrue(bindingResult.hasFieldErrors("name"));
    }

    @Test
    void successfulCommandCreationUsesPostRedirectGet() throws Exception {
        UUID deviceId = UUID.randomUUID();

        mockMvc.perform(post("/devices/{id}/commands", deviceId)
                        .param("name", "new_command")
                        .param("argsSchema", "{}")
                        .param("requiredRole", "GUEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices/" + deviceId))
                .andExpect(flash().attribute("successMessage", "Команду додано."));

        verify(commandService).create(deviceId, "new_command", "{}", RequiredRole.GUEST);
    }

    private static Device device(UUID id) {
        Device device = new Device("Test device", DeviceType.LAMP, "not-displayed", CREATED_AT);
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }

    private static Command command(UUID deviceId, String name, RequiredRole role) {
        Command command = new Command(device(deviceId), name, "{}", role, CREATED_AT);
        ReflectionTestUtils.setField(command, "id", UUID.randomUUID());
        return command;
    }

    private static User user(UUID id) {
        User user = new User("user@example.com", "hash", "User", CREATED_AT);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
