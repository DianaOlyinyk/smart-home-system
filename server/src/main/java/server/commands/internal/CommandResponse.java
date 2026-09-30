package server.commands.internal;
import server.commands.Command;
import java.time.Instant;
import java.util.UUID;

record CommandResponse(
    UUID id, UUID deviceId, String name,
    String argsSchema, String requiredRole, Instant createdAt) {
        static CommandResponse from(Command command) {
            return new CommandResponse(
                command.getId(), command.getDevice().getId(), command.getName(),
                command.getArgsSchema(), command.getRequiredRole().name(), command.getCreatedAt());
    }
}
