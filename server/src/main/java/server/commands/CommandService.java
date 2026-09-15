package server.commands;
import java.util.UUID;
import java.util.Map;

public interface CommandService {
    Command create(UUID deviceId, String name, Map<String, String> argsSchema, RequiredRole requiredRole);
    Command findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);
}
