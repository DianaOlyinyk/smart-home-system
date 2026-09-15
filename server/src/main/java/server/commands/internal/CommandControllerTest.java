package server.commands.internal;
import org.testng.annotations.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.commands.Command;
import server.commands.CommandService;
import server.commands.RequiredRole;
import server.devices.DeviceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        when(commandService.create(eq(deviceId), eq("set_brightness"), anyMap(), eq(RequiredRole.GUEST)))
        .thenReturn(new Command(commandId, deviceId, "set_brightness",
        Map.of("brightness", "int:0-100"), RequiredRole.GUEST, now));
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name": "set_brightness", "argsSchema": {"brightness": "int:0-100"}, "requiredRole": "GUEST"}
                            """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/devices/" + deviceId + "/commands/" + commandId))
        .andExpect(jsonPath("$.name").value("set_brightness"))
        .andExpect(jsonPath("$.requiredRole").value("GUEST"));
    }
    @Test
    void createCommandReturns404WhenDeviceIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(commandService.create(eq(deviceId), anyString(), anyMap(), any(RequiredRole.class)))
        .thenThrow(new DeviceNotFoundException(deviceId));
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name": "turn_on", "argsSchema": {}, "requiredRole": "GUEST"}
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
                        {"name": "", "argsSchema": {}, "requiredRole": "GUEST"}
                        """))
        .andExpect(status().isBadRequest());
    }
    @Test
    void createCommandReturns400WhenRequiredRoleIsMissing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        mockMvc.perform(post("/devices/{deviceId}/commands", deviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "turn_on", "argsSchema": {}}
                        """))
        .andExpect(status().isBadRequest());
    }
}