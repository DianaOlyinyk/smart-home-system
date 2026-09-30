package server.devices.internal;

import java.time.Instant;
import java.util.UUID;
import server.devices.Device;
import server.devices.DeviceType;

import java.util.Set;

record DeviceResponse(UUID id, String name, DeviceType type, String connectionToken, Instant createdAt,
                      Set<DeviceAccessResponse> accesses) {
    static DeviceResponse from(Device device) {
        Set<DeviceAccessResponse> accesses = device.getAccesses().stream()
                .map(DeviceAccessResponse::from)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new DeviceResponse(device.id(), device.name(), device.type(), device.connectionToken(),
                device.createdAt(), accesses);
    }
}
