package server.commands;

import java.time.Instant;
import java.util.UUID;

public record Command(
        UUID id,
        UUID deviceId,
        String name,
        String argsSchema,
        String requiredRole,
        Instant createdAt) {
}
