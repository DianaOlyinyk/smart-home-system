package server.commands.internal;
import server.commands.Command;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record CommandResponse(
    UUID id, UUID deviceId, String name,
    Map<String, String> argsSchema, String requiredRole, Instant createdAt) {
        static CommandResponse from(Command command) {
            return new CommandResponse(
                command.id(), command.deviceId(), command.name(),
                command.argsSchema(), command.requiredRole().name(), command.createdAt());
    }
}