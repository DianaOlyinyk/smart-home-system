package server.devices;

import jakarta.persistence.*;
import server.users.User;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeviceType type;

    @Column(name = "connection_token", nullable = false)
    private String connectionToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "device",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<DeviceAccess> accesses = new HashSet<>();

    protected Device() {

    }

    public Device(String name, DeviceType type, String connectionToken, Instant createdAt) {
        this.name = name;
        this.type = type;
        this.connectionToken = connectionToken;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Device(UUID id, String name, DeviceType type, String connectionToken, Instant createdAt) {
        this(name, type, connectionToken, createdAt);
        this.id = id;
    }

    public void grantAccess(User user, AccessRole role, User grantedBy, Instant at) {
        Optional<DeviceAccess> existingAccess = accesses.stream()
                .filter(a -> a.getUserId().equals(user.getId()))
                .findFirst();
        if (existingAccess.isPresent()) {
            DeviceAccess access = existingAccess.get();
            access.setRole(role);
            access.updateGrantedDetails(grantedBy, at);
        } else {
            DeviceAccess access = new DeviceAccess(this, user, role, grantedBy, at);
            this.accesses.add(access);
        }
    }

    public boolean revokeAccess(UUID userId) {
        return this.accesses.removeIf(access -> access.getUserId().equals(userId));
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public DeviceType getType() { return type; }
    public String getConnectionToken() { return connectionToken; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID id() { return id; }
    public String name() { return name; }
    public DeviceType type() { return type; }
    public String connectionToken() { return connectionToken; }
    public Instant createdAt() { return createdAt; }

    public Set<DeviceAccess> getAccesses() {
        return Collections.unmodifiableSet(accesses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
