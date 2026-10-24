package server.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.commands.RequiredRole;

record DeviceCommandForm(
        @NotBlank(message = "Вкажіть назву команди")
        String name,
        @NotNull(message = "Вкажіть схему аргументів")
        String argsSchema,
        @NotNull(message = "Виберіть роль доступу")
        RequiredRole requiredRole) {

    static DeviceCommandForm empty() {
        return new DeviceCommandForm("", "{}", RequiredRole.GUEST);
    }
}
