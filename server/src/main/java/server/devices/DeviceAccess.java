package server.devices;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import server.users.User;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "device_accesses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_user",
                columnNames = {"device_id", "user_id"}
        )
)
public class DeviceAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AccessRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected DeviceAccess() {

    }

    public DeviceAccess(Device device, User user, AccessRole role, User grantedBy, Instant grantedAt) {
        this.device = Objects.requireNonNull(device, "Device must not be null");
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.role = Objects.requireNonNull(role, "Role must not be null");
        this.grantedBy = Objects.requireNonNull(grantedBy, "GrantedBy must not be null");
        this.grantedAt = Objects.requireNonNull(grantedAt, "GrantedAt must not be null");
    }

    public UUID getId() { return id; }
    public Device getDevice() { return device; }
    public User getUser() { return user; }
    public UUID getUserId() { return user.getId(); }
    public AccessRole getRole() { return role; }
    public User getGrantedBy() { return grantedBy; }
    public UUID getGrantedById() { return grantedBy == null ? null : grantedBy.getId(); }
    public Instant getGrantedAt() { return grantedAt; }

    public void setRole(AccessRole role) {
        this.role = Objects.requireNonNull(role, "Role must not be null");
    }

    public void updateGrantedDetails(User grantedBy, Instant grantedAt) {
        this.grantedBy = Objects.requireNonNull(grantedBy, "GrantedBy must not be null");
        this.grantedAt = Objects.requireNonNull(grantedAt, "GrantedAt must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceAccess that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}