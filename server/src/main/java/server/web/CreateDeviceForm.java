package server.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.devices.DeviceType;

record CreateDeviceForm(
        @NotBlank(message = "Вкажіть назву пристрою") String name,
        @NotNull(message = "Оберіть тип пристрою") DeviceType type
) {}
