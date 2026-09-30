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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

          @Test
          void createDeviceWithOwnerUsesOwnerOverload() throws Exception {
            UUID id = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            when(deviceService.create(eq("Лампа"), eq(DeviceType.LAMP), eq(ownerId)))
                .thenReturn(new Device(id, "Лампа", DeviceType.LAMP, "token-123", now));

            mockMvc.perform(post("/devices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name": "Лампа",
                          "type": "LAMP",
                          "ownerId": "%s"
                        }
                        """.formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/devices/" + id));

            verify(deviceService).create("Лампа", DeviceType.LAMP, ownerId);
          }

          @Test
          void findAllReturnsDevicesForType() throws Exception {
            UUID id = UUID.randomUUID();
            Device device = new Device(id, "Лампа", DeviceType.LAMP, "token-123", Instant.now());
            when(deviceService.findAll(DeviceType.LAMP)).thenReturn(List.of(device));

            mockMvc.perform(get("/devices").param("type", "LAMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Лампа"))
                .andExpect(jsonPath("$[0].accesses").isArray())
                .andExpect(jsonPath("$[0].accesses").isEmpty());

            verify(deviceService).findAll(DeviceType.LAMP);
          }

          @Test
          void getDeviceReturnsAccessAwareResponse() throws Exception {
            UUID id = UUID.randomUUID();
            Device device = new Device(id, "Лампа", DeviceType.LAMP, "token-123", Instant.now());
            when(deviceService.getWithAccesses(id)).thenReturn(device);

            mockMvc.perform(get("/devices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.accesses").isArray());

            verify(deviceService).getWithAccesses(id);
          }

          @Test
          void renameDeviceReturnsUpdatedDevice() throws Exception {
            UUID id = UUID.randomUUID();
            Device renamed = new Device(id, "New name", DeviceType.LAMP, "token-123", Instant.now());
            when(deviceService.rename(id, "New name")).thenReturn(renamed);

            mockMvc.perform(put("/devices/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name": "New name"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));

            verify(deviceService).rename(id, "New name");
          }

          @Test
          void deleteDeviceReturnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/devices/{id}", id))
                .andExpect(status().isNoContent());

            verify(deviceService).delete(id);
          }
}
