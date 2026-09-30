package server.devices.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        Device device = new Device("Лампа", DeviceType.LAMP, "token-123", now);
        ReflectionTestUtils.setField(device, "id", id);
        when(deviceService.create(eq("Лампа"), eq(DeviceType.LAMP), isNull()))
                .thenReturn(device);

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
    void createDevicePassesUserFromHeaderAsOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        Device device = device(UUID.randomUUID(), "Лампа", DeviceType.LAMP);
        when(deviceService.create("Лампа", DeviceType.LAMP, userId)).thenReturn(device);

        mockMvc.perform(post("/devices")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Лампа",
                                  "type": "LAMP"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(deviceService).create("Лампа", DeviceType.LAMP, userId);
    }

    @Test
    void createDeviceReturns400WhenUserHeaderIsNotUuid() throws Exception {
        mockMvc.perform(post("/devices")
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Лампа",
                                  "type": "LAMP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllReturnsDevices() throws Exception {
        Device lamp = device(UUID.randomUUID(), "Лампа", DeviceType.LAMP);
        Device kettle = device(UUID.randomUUID(), "Чайник", DeviceType.KETTLE);
        when(deviceService.findAll(null)).thenReturn(List.of(lamp, kettle));

        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Лампа"))
                .andExpect(jsonPath("$[1].name").value("Чайник"));
    }

    @Test
    void findAllFiltersByType() throws Exception {
        Device lamp = device(UUID.randomUUID(), "Лампа", DeviceType.LAMP);
        when(deviceService.findAll(DeviceType.LAMP)).thenReturn(List.of(lamp));

        mockMvc.perform(get("/devices").param("type", "LAMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LAMP"));
    }

    @Test
    void findAllReturns400WhenTypeIsUnknown() throws Exception {
        mockMvc.perform(get("/devices").param("type", "TOASTER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsDeviceWithAccesses() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Device device = device(id, "Лампа", DeviceType.LAMP);
        User owner = new User("olena@example.com", "hash", "Олена", now);
        ReflectionTestUtils.setField(owner, "id", userId);
        device.grantAccess(owner, AccessRole.OWNER, owner, now);
        when(deviceService.getWithAccesses(id)).thenReturn(device);

        mockMvc.perform(get("/devices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.accesses.length()").value(1))
                .andExpect(jsonPath("$.accesses[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.accesses[0].role").value("OWNER"));
    }

    @Test
    void getByIdReturns404WhenDeviceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(deviceService.getWithAccesses(id)).thenThrow(new DeviceNotFoundException(id));

        mockMvc.perform(get("/devices/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameReturnsUpdatedDevice() throws Exception {
        UUID id = UUID.randomUUID();
        when(deviceService.rename(id, "Нова лампа")).thenReturn(device(id, "Нова лампа", DeviceType.LAMP));

        mockMvc.perform(put("/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Нова лампа"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Нова лампа"));
    }

    @Test
    void renameReturns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(put("/devices/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renameReturns404WhenDeviceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(deviceService.rename(id, "Нова лампа")).thenThrow(new DeviceNotFoundException(id));

        mockMvc.perform(put("/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Нова лампа"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/devices/{id}", id))
                .andExpect(status().isNoContent());

        verify(deviceService).delete(id);
    }

    @Test
    void deleteReturns404WhenDeviceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DeviceNotFoundException(id)).when(deviceService).delete(id);

        mockMvc.perform(delete("/devices/{id}", id))
                .andExpect(status().isNotFound());
    }

    private static Device device(UUID id, String name, DeviceType type) {
        Device device = new Device(name, type, "token-123", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }
}
