package server.devices.internal;

import jakarta.validation.constraints.NotBlank;

record RenameDeviceRequest(
    @NotBlank(message = "Назва пристрою є обов'язковою")
    String name) {
}
