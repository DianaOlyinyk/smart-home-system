package server.commands;
import java.time.Instant;
import java.util.UUID;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint (columnNames = {"device_id", "name"}))
public class Command{
    @GeneratedValue
    @Id
    UUID id;

    UUID deviceId;
    String name;
    String argsSchema;
    @Enumerated(EnumType.STRING)
    RequiredRole requiredRole;
    Instant createdAt;

    public Command() {}

    public Command(UUID deviceId, String name, String argsSchema, RequiredRole requiredRole, Instant createdAt) {
        this.deviceId = deviceId;
        this.name = name;
        this.argsSchema = argsSchema;
        this.requiredRole = requiredRole;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public String getArgsSchema() {
        return argsSchema;
    }

    public RequiredRole getRequiredRole() {
        return requiredRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setArgsSchema(String argsSchema) {
        this.argsSchema = argsSchema;
    }

    public void setRequiredRole(RequiredRole requiredRole) {
        this.requiredRole = requiredRole;
    }
}
