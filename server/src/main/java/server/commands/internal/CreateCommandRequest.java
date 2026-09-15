package server.commands.internal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.commands.RequiredRole;
import java.util.Map;

record CreateCommandRequest(
        @NotBlank(message = "Назва команди є обов'язковою")
        String name,
        @NotNull(message = "Схема аргументів є обов'язковою")
        Map<String, String> argsSchema,
        @NotNull(message = "Роль доступу є обов'язковою")
        RequiredRole requiredRole) {
}