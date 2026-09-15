package server.executions;

import java.util.UUID;

public interface CommandExecutionService {

    CommandExecution execute(UUID deviceId, UUID commandId, String argsJson);
}
