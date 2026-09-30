package server.commands;
import java.time.Instant;
import java.util.UUID;


import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import server.devices.Device;

@Entity
@Table(uniqueConstraints = @UniqueConstraint (columnNames = {"device_id", "name"}))
public class Command{
    @GeneratedValue
    @Id
    UUID id;

    @ManyToOne(fetch =  FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Device device;
    String name;
    String argsSchema;
    @Enumerated(EnumType.STRING)
    RequiredRole requiredRole;
    Instant createdAt;

    public Command() {}

    public Command(Device device, String name, String argsSchema, RequiredRole requiredRole, Instant createdAt) {
        this.device = device;
        this.name = name;
        this.argsSchema = argsSchema;
        this.requiredRole = requiredRole;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Device getDevice() { return  device; }

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
