package server.devices;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import server.devices.internal.DeviceAccess;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Column(nullable = false, unique = true)
    private String connectionToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DeviceAccess> accesses = new HashSet<>();

    protected Device() {}

    public Device(String name, DeviceType type, String connectionToken, Instant createdAt) {
        this.name = name;
        this.type = type;
        this.connectionToken = connectionToken;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DeviceType getType() { return type; }
    public String getConnectionToken() { return connectionToken; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<DeviceAccess> getAccesses() { return accesses; }
}