package server.users.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
        @NotBlank(message = "Email є обов'язковим")
        @Email(message = "Некоректний формат email")
        String email,

        @NotBlank(message = "Пароль є обов'язковим")
        @Size(min = 8, max = 72, message = "Пароль має містити від 8 до 72 символів")
        String password,

        @NotBlank(message = "Ім'я є обов'язковим")
        String name) {
}
