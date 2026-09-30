package server.users.internal;
import jakarta.validation.constraints.NotBlank;

public record RenameUserRequest(
        @NotBlank(message = "Ім'я є обов'язковим!")
        String name) {
}
