package server.executions.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.commands.Command;
import server.commands.CommandNotFoundException;
import server.commands.CommandService;
import server.executions.CommandExecution;
import server.executions.CommandExecutionService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommandExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CommandArgsValidator.class)
class CommandExecutionControllerTest {

    private static final String BRIGHTNESS_SCHEMA = """
            {
              "type": "object",
              "properties": { "brightness": { "type": "integer", "minimum": 0, "maximum": 100 } },
              "required": ["brightness"],
              "additionalProperties": false
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandService commandService;

    @MockitoBean
    private CommandExecutionService commandExecutionService;

    @Test
    void executeCommandReturns202WithLocationAndBody() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-01-01T00:00:00Z");

        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenReturn(new Command(commandId, deviceId, "setBrightness", BRIGHTNESS_SCHEMA, "OWNER", requestedAt));
        when(commandExecutionService.execute(eq(deviceId), eq(commandId), anyString()))
                .thenReturn(new CommandExecution(executionId, deviceId, commandId, "{\"brightness\":80}", "PENDING", requestedAt));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": "{\\"brightness\\":80}"
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

        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenThrow(new CommandNotFoundException(deviceId, commandId));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": "{}"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Команду з id " + commandId + " для пристрою " + deviceId + " не знайдено"));
    }

    @Test
    void executeCommandReturns400WhenArgsJsonIsBlank() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": ""
                                }
                                """))
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
                                  "argsJson": "{}",
                                  "priority": "high"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeCommandReturns400WhenRequiredArgIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenReturn(new Command(commandId, deviceId, "setBrightness", BRIGHTNESS_SCHEMA, "OWNER", Instant.now()));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": "{}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors.brightness").exists());
    }

    @Test
    void executeCommandReturns400WhenArgIsNotDefinedByCommandSchema() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenReturn(new Command(commandId, deviceId, "setBrightness", BRIGHTNESS_SCHEMA, "OWNER", Instant.now()));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": "{\\"brightness\\": 80, \\"color\\": \\"red\\"}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors.color").exists());
    }

    @Test
    void executeCommandReturns400WhenArgIsOutOfRange() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenReturn(new Command(commandId, deviceId, "setBrightness", BRIGHTNESS_SCHEMA, "OWNER", Instant.now()));

        mockMvc.perform(post("/devices/{deviceId}/commands/{commandId}/executions", deviceId, commandId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "argsJson": "{\\"brightness\\": 150}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.errors.brightness").exists());
    }
}
