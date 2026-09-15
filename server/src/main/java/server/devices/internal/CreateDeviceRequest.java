package server.devices.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.devices.DeviceType;

record CreateDeviceRequest(
        @NotBlank String name,
        @NotNull DeviceType type) {
}
