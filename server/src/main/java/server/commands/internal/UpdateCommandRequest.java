package server.commands.internal;
import jakarta.validation.constraints.NotNull;
import server.commands.RequiredRole;

record UpdateCommandRequest(
        @NotNull(message = "Схема аргументів є обов'язковою")
        String argsSchema,
        @NotNull(message = "Роль доступу є обов'язковою")
        RequiredRole requiredRole) {
}
