package server.devices.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.devices.DeviceType;

import java.util.UUID;

record CreateDeviceRequest(
    @NotBlank(message = "Назва пристрою є обов'язковою")
    String name,

    @NotNull(message = "Тип пристрою є обов'язковим")
    DeviceType type,

    UUID ownerId) {
}
