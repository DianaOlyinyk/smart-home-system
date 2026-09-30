package server.commands;
import java.util.List;
import java.util.UUID;

public interface CommandService {
    Command create(UUID deviceId, String name, String argsSchema, RequiredRole requiredRole);
    Command findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);
    List<Command> findAllByDevice(UUID deviceId, RequiredRole requiredRole);
    void update(UUID deviceId, UUID commandId, String argsSchema, RequiredRole requiredRole);
    void delete(UUID deviceId, UUID commandId);
}
