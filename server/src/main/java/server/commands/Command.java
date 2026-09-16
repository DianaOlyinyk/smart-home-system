package server.commands;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Command(
    UUID id,
    UUID deviceId,
    String name,
    String argsSchema,
    RequiredRole requiredRole,
    Instant createdAt) {
}