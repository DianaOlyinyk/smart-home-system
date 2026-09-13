import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Command {
    @Id
    UUID id;
    UUID deviceId;
    String name;
    String argsSchema;
    String requiredRole;
    Instant createdAt;
}