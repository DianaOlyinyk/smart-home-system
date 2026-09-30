package server.devices.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import server.devices.Device;
import server.users.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_access")
public class DeviceAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private UUID grantedById;

    @Column(nullable = false, updatable = false)
    private Instant grantedAt;

    protected DeviceAccess() {} // Вимога JPA

    public DeviceAccess(Device device, User user, String role, UUID grantedById, Instant grantedAt) {
        this.device = device;
        this.user = user;
        this.role = role;
        this.grantedById = grantedById;
        this.grantedAt = grantedAt;
    }

    public UUID getId() { return id; }
    public Device getDevice() { return device; }
    public User getUser() { return user; }
    public String getRole() { return role; }
    public UUID getGrantedById() { return grantedById; }
    public Instant getGrantedAt() { return grantedAt; }
}