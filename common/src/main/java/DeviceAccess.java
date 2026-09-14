import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class DeviceAccess {
    @Id
    UUID id;
    UUID deviceId;
    UUID userId;
    String role;
    UUID grantedById;
    Instant grantedAt;
}
