package server.devices.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import server.devices.Device;
import server.devices.DeviceAccessResponse;
import server.devices.DeviceType;

record DeviceResponse(UUID id, String name, DeviceType type, String connectionToken, Instant createdAt,
                      List<DeviceAccessResponse> accesses) {
    static DeviceResponse from(Device device) {
        List<DeviceAccessResponse> accesses = device.getAccesses().stream()
                .map(DeviceAccessResponse::from)
                .toList();
        return new DeviceResponse(device.getId(), device.getName(), device.getType(),
                device.getConnectionToken(), device.getCreatedAt(), accesses);
    }
}
