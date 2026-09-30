package server.executions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import server.commands.Command;
import server.devices.Device;
import server.users.User;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "command_executions")
public class CommandExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "command_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Command command;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User executedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> args;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant completedAt;

    protected CommandExecution() {
    }

    public CommandExecution(Command command, Device device, User executedBy,
                            Map<String, Object> args, Instant requestedAt) {
        this.command = command;
        this.device = device;
        this.executedBy = executedBy;
        this.args = new HashMap<>(args);
        this.status = ExecutionStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public CommandExecution transitionTo(ExecutionStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(status, newStatus);
        }
        status = newStatus;
        if (newStatus.isTerminal()) {
            completedAt = Instant.now();
        }
        return this;
    }

    public UUID getId() {
        return id;
    }

    public Command getCommand() {
        return command;
    }

    public Device getDevice() {
        return device;
    }

    public User getExecutedBy() {
        return executedBy;
    }

    public Map<String, Object> getArgs() {
        return Collections.unmodifiableMap(args);
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommandExecution other = (CommandExecution) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
