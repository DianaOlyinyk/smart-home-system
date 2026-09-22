package server.devices.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void createDeviceReturns201WithLocationAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(deviceService.create(eq("Лампа"), eq(DeviceType.LAMP)))
                .thenReturn(new Device(id, "Лампа", DeviceType.LAMP, "token-123", now));

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Лампа",
                                  "type": "LAMP"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/devices/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Лампа"))
                .andExpect(jsonPath("$.type").value("LAMP"))
                .andExpect(jsonPath("$.connectionToken").value("token-123"));
    }

    @Test
    void createDeviceReturns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "type": "LAMP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDeviceReturns400WhenTypeIsUnknown() throws Exception {
        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Лампа",
                                  "type": "TOASTER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDeviceReturns400WhenNameIsWhitespaceOnly() throws Exception {
        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "type": "LAMP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDeviceReturns400WhenTypeIsMissing() throws Exception {
        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Лампа"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
