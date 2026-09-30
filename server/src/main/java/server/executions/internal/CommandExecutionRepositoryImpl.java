package server.executions.internal;

import org.springframework.stereotype.Repository;
import org.springframework.context.event.EventListener;
import server.executions.CommandExecution;
import server.executions.CommandExecutionRepository;
import server.devices.DeviceDeletedEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class CommandExecutionRepositoryImpl implements CommandExecutionRepository {

    private final Map<UUID, CommandExecution> executions = new ConcurrentHashMap<>();

    @Override
    public CommandExecution save(CommandExecution execution) {
        executions.put(execution.id(), execution);
        return execution;
    }

    @Override
    public Optional<CommandExecution> findById(UUID id) {
        return Optional.ofNullable(executions.get(id));
    }

    @Override
    public void deleteByDeviceId(UUID deviceId) {
        executions.entrySet().removeIf(entry -> deviceId.equals(entry.getValue().deviceId()));
    }

    @EventListener
    void onDeviceDeleted(DeviceDeletedEvent event) {
        deleteByDeviceId(event.deviceId());
    }
}
