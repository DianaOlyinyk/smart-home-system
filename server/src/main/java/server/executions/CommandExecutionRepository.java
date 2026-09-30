package server.executions;

import java.util.Optional;
import java.util.UUID;

public interface CommandExecutionRepository {

    CommandExecution save(CommandExecution execution);

    Optional<CommandExecution> findById(UUID id);

    void deleteByDeviceId(UUID deviceId);
}
