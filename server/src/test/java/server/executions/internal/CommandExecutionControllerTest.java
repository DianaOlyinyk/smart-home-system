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
import server.devices.DeviceNotFoundException;
import server.devices.DeviceType;
import server.executions.CommandExecution;
import server.executions.CommandExecutionNotFoundException;
import server.executions.CommandExecutionService;
import server.executions.ExecutionStatus;
import server.executions.InvalidStateTransitionException;
import server.users.User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommandExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommandExecutionControllerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandExecutionService commandExecutionService;

    private final UUID deviceId = UUID.randomUUID();
    private final UUID commandId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private CommandExecution execution(Map<String, Object> args, User executedBy) {
        Device device = new Device("Лампа", DeviceType.LAMP, "token", REQUESTED_AT);
        ReflectionTestUtils.setField(device, "id", deviceId);
        Command command = new Command(device, "set_brightness", "{}", RequiredRole.GUEST, REQUESTED_AT);
        ReflectionTestUtils.setField(command, "id", commandId);
        CommandExecution execution = new CommandExecution(command, device, executedBy, args, REQUESTED_AT);
        ReflectionTestUtils.setField(execution, "id", executionId);
        return execution;
    }

    private User user() {
        User user = new User("olena@example.com", "hash", "Олена", REQUESTED_AT);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    @Test
    void executeCommandReturns202WithLocationAndBody() throws Exception {
        when(commandExecutionService.execute(eq(deviceId), eq(commandId), eq(userId), anyMap()))
                .thenReturn(execution(Map.of("brightness", 80), user()));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": { "brightness": 80 }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/devices/" + deviceId + "/executions/" + executionId))
                .andExpect(jsonPath("$.id").value(executionId.toString()))
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.commandId").value(commandId.toString()))
                .andExpect(jsonPath("$.commandName").value("set_brightness"))
                .andExpect(jsonPath("$.executedById").value(userId.toString()))
                .andExpect(jsonPath("$.executedByEmail").value("olena@example.com"))
                .andExpect(jsonPath("$.args.brightness").value(80))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void executeCommandWithoutUserHeaderPassesNullUser() throws Exception {
        when(commandExecutionService.execute(eq(deviceId), eq(commandId), isNull(), anyMap()))
                .thenReturn(execution(Map.of(), null));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": {}
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executedById").doesNotExist())
                .andExpect(jsonPath("$.executedByEmail").doesNotExist());
    }

    @Test
    void executeCommandReturns400WhenUserHeaderIsNotUuid() throws Exception {
        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "args": {}
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commandExecutionService);
    }

    @Test
    void executeCommandReturns404ProblemDetailWhenCommandDoesNotBelongToDevice() throws Exception {
        when(commandExecutionService.execute(eq(deviceId), eq(commandId), eq(userId), anyMap()))
                .thenThrow(new CommandNotFoundException(deviceId, commandId));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .header("X-User-Id", userId)
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
    void executeCommandReturns400WhenArgsFieldIsMissing() throws Exception {
        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownFieldInPayloadIsRejected() throws Exception {
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

    @Test
    void journalReturnsExecutionsOfDevice() throws Exception {
        when(commandExecutionService.findJournal(deviceId, null))
                .thenReturn(List.of(execution(Map.of("brightness", 80), user())));

        mockMvc.perform(get("/devices/{deviceId}/executions", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(executionId.toString()))
                .andExpect(jsonPath("$[0].commandName").value("set_brightness"))
                .andExpect(jsonPath("$[0].executedByEmail").value("olena@example.com"));
    }

    @Test
    void journalFiltersByUser() throws Exception {
        when(commandExecutionService.findJournal(deviceId, userId)).thenReturn(List.of());

        mockMvc.perform(get("/devices/{deviceId}/executions", deviceId).param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commandExecutionService).findJournal(deviceId, userId);
    }

    @Test
    void journalReturns404WhenDeviceNotFound() throws Exception {
        when(commandExecutionService.findJournal(deviceId, null)).thenThrow(new DeviceNotFoundException(deviceId));

        mockMvc.perform(get("/devices/{deviceId}/executions", deviceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsExecution() throws Exception {
        when(commandExecutionService.findById(deviceId, executionId)).thenReturn(execution(Map.of(), user()));

        mockMvc.perform(get("/devices/{deviceId}/executions/{executionId}", deviceId, executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(executionId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getByIdReturns404WhenExecutionNotFound() throws Exception {
        when(commandExecutionService.findById(deviceId, executionId))
                .thenThrow(new CommandExecutionNotFoundException(deviceId, executionId));

        mockMvc.perform(get("/devices/{deviceId}/executions/{executionId}", deviceId, executionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatusReturnsUpdatedExecution() throws Exception {
        CommandExecution execution = execution(Map.of(), user());
        execution.transitionTo(ExecutionStatus.RUNNING).transitionTo(ExecutionStatus.TIMEOUT);
        when(commandExecutionService.changeStatus(deviceId, executionId, ExecutionStatus.TIMEOUT)).thenReturn(execution);

        mockMvc.perform(patch("/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "TIMEOUT" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TIMEOUT"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void changeStatusReturns422OnForbiddenTransition() throws Exception {
        when(commandExecutionService.changeStatus(deviceId, executionId, ExecutionStatus.RUNNING))
                .thenThrow(new InvalidStateTransitionException(ExecutionStatus.SUCCESS, ExecutionStatus.RUNNING));

        mockMvc.perform(patch("/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "RUNNING" }
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void changeStatusReturns400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commandExecutionService);
    }

    @Test
    void changeStatusReturns400WhenStatusIsUnknown() throws Exception {
        mockMvc.perform(patch("/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "DONE" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatusReturns404WhenExecutionNotFound() throws Exception {
        when(commandExecutionService.changeStatus(deviceId, executionId, ExecutionStatus.TIMEOUT))
                .thenThrow(new CommandExecutionNotFoundException(deviceId, executionId));

        mockMvc.perform(patch("/devices/{deviceId}/executions/{executionId}", deviceId, executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "TIMEOUT" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/devices/{deviceId}/executions/{executionId}", deviceId, executionId))
                .andExpect(status().isNoContent());

        verify(commandExecutionService).delete(deviceId, executionId);
    }

    @Test
    void deleteReturns404WhenExecutionNotFound() throws Exception {
        doThrow(new CommandExecutionNotFoundException(deviceId, executionId))
                .when(commandExecutionService).delete(deviceId, executionId);

        mockMvc.perform(delete("/devices/{deviceId}/executions/{executionId}", deviceId, executionId))
                .andExpect(status().isNotFound());
    }
}
