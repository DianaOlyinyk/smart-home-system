import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Device {
    @Id
    UUID id;

    String name;
    String type;
    String connectionToken;
    String state;
    Instant createdAt;
}
