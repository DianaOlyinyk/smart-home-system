package server.devices.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.devices.AccessRole;

import java.util.UUID;

record GrantAccessRequest(
        @NotBlank(message = "Email є обов'язковим")
        String email,

        @NotNull(message = "Роль є обов'язковою")
        AccessRole role,

        @NotNull(message = "grantedById є обов'язковим")
        UUID grantedById
) {
}
