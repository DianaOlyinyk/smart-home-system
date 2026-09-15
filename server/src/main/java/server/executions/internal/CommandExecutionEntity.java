package server.executions.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class CommandExecutionEntity {
    @Id
    UUID id;

    UUID deviceId;
    UUID commandId;
    UUID executedById;
    String argsJson;
    String status;
    String result;
    Instant requestedAt;
    Instant completedAt;
}
