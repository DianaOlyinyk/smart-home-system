package server.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.devices.DeviceType;

record CreateDeviceForm(
        @NotBlank(message = "Вкажіть назву пристрою")
        @Size(max = 255, message = "Назва не може бути довшою за 255 символів") String name,
        @NotNull(message = "Оберіть тип пристрою") DeviceType type
) {}
