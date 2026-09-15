package server.executions;

import java.time.Instant;
import java.util.UUID;

public record CommandExecution(
        UUID id,
        UUID deviceId,
        UUID commandId,
        String argsJson,
        String status,
        Instant requestedAt) {
}
