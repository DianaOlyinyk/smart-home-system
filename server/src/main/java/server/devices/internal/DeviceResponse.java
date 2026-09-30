package server.devices.internal;

import java.time.Instant;
import java.util.UUID;
import server.devices.Device;
import server.devices.DeviceType;

record DeviceResponse(UUID id, String name, DeviceType type, String connectionToken, Instant createdAt) {
    static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getName(), device.getType(), device.getConnectionToken(), device.getCreatedAt());
    }
}
