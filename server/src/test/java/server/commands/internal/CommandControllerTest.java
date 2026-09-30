package server.commands.internal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.commands.Command;
import server.commands.CommandAlreadyExistsException;
import server.commands.CommandNotFoundException;
import server.commands.CommandService;
import server.commands.RequiredRole;
import server.devices.DeviceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(CommandController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommandControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    CommandService commandService;
    @Test
    void createCommandReturns201WithLocationAndBody() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Command command = new Command(deviceId, "set_brightness", "{}", RequiredRole.GUEST, now);
        ReflectionTestUtils.setField(command, "id", commandId);
        when(commandService.create(eq(deviceId), eq("set_brightness"), eq("{}"), eq(RequiredRole.GUEST)))
            .thenReturn(command);
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"name": "set_brightness", "argsSchema": "{}", "requiredRole": "GUEST"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/devices/" + deviceId + "/commands/" + commandId))
            .andExpect(jsonPath("$.name").value("set_brightness"))
            .andExpect(jsonPath("$.requiredRole").value("GUEST"));
}
    @Test
    void createCommandReturns404WhenDeviceIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.create(eq(deviceId), anyString(), anyString(), any(RequiredRole.class)))
        .thenThrow(new DeviceNotFoundException(deviceId));
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"name": "turn_on", "argsSchema": "{}", "requiredRole": "GUEST"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
}
    @Test
    void createCommandReturns400WhenNameIsBlank() throws Exception {
        UUID deviceId = UUID.randomUUID();
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"name": "", "argsSchema": "{}", "requiredRole": "GUEST"}
                    """))
            .andExpect(status().isBadRequest());
}
    @Test
    void createCommandReturns400WhenRequiredRoleIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"name": "turn_on", "argsSchema": "{}"}
                    """))
            .andExpect(status().isBadRequest());
    }
    @Test
    void createCommandReturns409WhenNameAlreadyExists() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.create(eq(deviceId), eq("turn_on"), anyString(), any(RequiredRole.class)))
            .thenThrow(new CommandAlreadyExistsException(deviceId, "turn_on"));
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"name": "turn_on", "argsSchema": "{}", "requiredRole": "GUEST"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }
    @Test
    void listCommandsReturns200WithAllCommands() throws Exception {
        UUID deviceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(commandService.findAllByDevice(deviceId, null)).thenReturn(List.of(
            new Command(deviceId, "set_brightness", "{}", RequiredRole.OWNER, now),
            new Command(deviceId, "turn_on", "{}", RequiredRole.GUEST, now)));
        mockMvc.perform(get("/devices/{deviceId}/commands", deviceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("set_brightness"))
            .andExpect(jsonPath("$[1].name").value("turn_on"));
    }
    @Test
    void listCommandsPassesRequiredRoleFilter() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.findAllByDevice(deviceId, RequiredRole.OWNER)).thenReturn(List.of(
            new Command(deviceId, "set_mode", "{}", RequiredRole.OWNER, Instant.now())));
        mockMvc.perform(get("/devices/{deviceId}/commands", deviceId).param("requiredRole", "OWNER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].requiredRole").value("OWNER"));
    }
    @Test
    void listCommandsReturns404WhenDeviceIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.findAllByDevice(deviceId, null)).thenThrow(new DeviceNotFoundException(deviceId));
        mockMvc.perform(get("/devices/{deviceId}/commands", deviceId))
            .andExpect(status().isNotFound());
    }
    @Test
    void updateCommandReturns204() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        mockMvc.perform(put("/devices/{deviceId}/commands/{commandId}", deviceId, commandId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"argsSchema": "{}", "requiredRole": "OWNER"}
                    """))
            .andExpect(status().isNoContent());
        verify(commandService).update(deviceId, commandId, "{}", RequiredRole.OWNER);
    }
    @Test
    void updateCommandReturns404WhenCommandIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        doThrow(new CommandNotFoundException(deviceId, commandId))
            .when(commandService).update(deviceId, commandId, "{}", RequiredRole.OWNER);
        mockMvc.perform(put("/devices/{deviceId}/commands/{commandId}", deviceId, commandId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"argsSchema": "{}", "requiredRole": "OWNER"}
                    """))
            .andExpect(status().isNotFound());
    }
    @Test
    void updateCommandReturns400WhenRequiredRoleIsMissing() throws Exception {
        mockMvc.perform(put("/devices/{deviceId}/commands/{commandId}", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"argsSchema": "{}"}
                    """))
            .andExpect(status().isBadRequest());
    }
    @Test
    void deleteCommandReturns204() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        mockMvc.perform(delete("/devices/{deviceId}/commands/{commandId}", deviceId, commandId))
            .andExpect(status().isNoContent());
        verify(commandService).delete(deviceId, commandId);
    }
    @Test
    void deleteCommandReturns404WhenCommandIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        doThrow(new CommandNotFoundException(deviceId, commandId))
            .when(commandService).delete(deviceId, commandId);
        mockMvc.perform(delete("/devices/{deviceId}/commands/{commandId}", deviceId, commandId))
            .andExpect(status().isNotFound());
    }
}
