package server.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "Email є обов'язковим")
        @Email(message = "Некоректний формат email")
        String email,

        @NotBlank(message = "Пароль є обов'язковим")
        String password) {
}
