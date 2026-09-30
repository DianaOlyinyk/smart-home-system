package server.executions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CommandExecutionService {

    CommandExecution execute(UUID deviceId, UUID commandId, UUID userId, Map<String, Object> args);

    List<CommandExecution> findJournal(UUID deviceId, UUID userId);

    CommandExecution findById(UUID deviceId, UUID executionId);

    CommandExecution changeStatus(UUID deviceId, UUID executionId, ExecutionStatus status);

    void delete(UUID deviceId, UUID executionId);
}
