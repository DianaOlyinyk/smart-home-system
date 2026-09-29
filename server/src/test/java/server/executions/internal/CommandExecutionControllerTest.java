package server.executions.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import server.commands.Command;
import server.commands.CommandNotFoundException;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceType;
import server.executions.CommandExecution;
import server.executions.CommandExecutionService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommandExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommandExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandExecutionService commandExecutionService;

    private static CommandExecution pendingExecution(
            UUID executionId, UUID deviceId, UUID commandId, Map<String, Object> args, Instant requestedAt) {
        Device device = new Device("Лампа", DeviceType.LAMP, "token", requestedAt);
        ReflectionTestUtils.setField(device, "id", deviceId);
        Command command = new Command(device, "set_brightness", "{}", RequiredRole.GUEST, requestedAt);
        ReflectionTestUtils.setField(command, "id", commandId);
        CommandExecution execution = new CommandExecution(command, device, null, args, requestedAt);
        ReflectionTestUtils.setField(execution, "id", executionId);
        return execution;
    }

    @Test
    void executeCommandReturns202WithLocationAndBody() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-01-01T00:00:00Z");

        when(commandExecutionService.execute(eq(deviceId), eq(commandId), anyMap()))
                .thenReturn(pendingExecution(executionId, deviceId, commandId, Map.of("brightness", 80), requestedAt));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": { "brightness": 80 }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location",
                        "/devices/" + deviceId + "/commands/" + commandId + "/executions/" + executionId))
                .andExpect(jsonPath("$.id").value(executionId.toString()))
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.commandId").value(commandId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void executeCommandReturns404ProblemDetailWhenCommandDoesNotBelongToDevice() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        when(commandExecutionService.execute(eq(deviceId), eq(commandId), anyMap()))
                .thenThrow(new CommandNotFoundException(deviceId, commandId));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": { "brightness": 80 }
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Команду з id " + commandId + " для пристрою " + deviceId + " не знайдено"));
    }

    @Test
    void executeCommandAcceptsEmptyArgsForArgumentlessCommand() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-01-01T00:00:00Z");

        when(commandExecutionService.execute(eq(deviceId), eq(commandId), eq(Map.of())))
                .thenReturn(pendingExecution(executionId, deviceId, commandId, Map.of(), requestedAt));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": {}
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void executeCommandReturns400WhenArgsFieldIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownFieldInPayloadIsRejected() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": { "brightness": 80 },
                                  "priority": "high"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
