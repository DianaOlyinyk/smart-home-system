import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class CommandExecution {
    @Id
    UUID id;
    UUID commandId;
    UUID deviceId;
    UUID executedById;
    String[] args;
    String status;
    String result;
    Instant requestedAt;
    Instant completedAt;
}

