package server.executions.internal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.commands.Command;
import server.commands.CommandService;
import server.devices.DeviceService;
import server.executions.CommandExecutedEvent;
import server.executions.CommandExecution;
import server.executions.CommandExecutionNotFoundException;
import server.executions.CommandExecutionRepository;
import server.executions.CommandExecutionService;
import server.executions.CommandExecutionStrategy;
import server.executions.ExecutionOutcome;
import server.executions.ExecutionStatus;
import server.users.User;
import server.users.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class CommandExecutionServiceImpl implements CommandExecutionService {

    private final CommandService commandService;
    private final DeviceService deviceService;
    private final UserService userService;
    private final CommandArgsValidator commandArgsValidator;
    private final CommandExecutionRepository commandExecutionRepository;
    private final CommandExecutionStrategyResolver strategyResolver;
    private final ApplicationEventPublisher eventPublisher;

    CommandExecutionServiceImpl(
            CommandService commandService,
            DeviceService deviceService,
            UserService userService,
            CommandArgsValidator commandArgsValidator,
            CommandExecutionRepository commandExecutionRepository,
            CommandExecutionStrategyResolver strategyResolver,
            ApplicationEventPublisher eventPublisher) {
        this.commandService = commandService;
        this.deviceService = deviceService;
        this.userService = userService;
        this.commandArgsValidator = commandArgsValidator;
        this.commandExecutionRepository = commandExecutionRepository;
        this.strategyResolver = strategyResolver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CommandExecution execute(UUID deviceId, UUID commandId, UUID userId, Map<String, Object> args) {
        Command command = commandService.findByDeviceIdAndCommandId(deviceId, commandId);
        User executedBy = userId == null ? null : userService.getUser(userId);
        commandArgsValidator.validate(command.getArgsSchema(), args);

        CommandExecutionStrategy strategy = strategyResolver.resolve(command.getName());

        CommandExecution execution = new CommandExecution(
                command, command.getDevice(), executedBy, args, Instant.now());
        execution = commandExecutionRepository.save(execution.transitionTo(ExecutionStatus.RUNNING));

        ExecutionOutcome outcome = strategy.execute(deviceId, command, args);
        execution = commandExecutionRepository.save(
                execution.transitionTo(outcome.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED));

        eventPublisher.publishEvent(new CommandExecutedEvent(execution.getId(), execution.getStatus()));

        return execution;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandExecution> findJournal(UUID deviceId, UUID userId) {
        deviceService.findById(deviceId);
        if (userId == null) {
            return commandExecutionRepository.findJournalByDeviceId(deviceId);
        }
        return commandExecutionRepository.findByDeviceIdAndExecutedByIdOrderByRequestedAtDesc(deviceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CommandExecution findById(UUID deviceId, UUID executionId) {
        return getExecution(deviceId, executionId);
    }

    @Override
    @Transactional
    public CommandExecution changeStatus(UUID deviceId, UUID executionId, ExecutionStatus status) {
        return getExecution(deviceId, executionId).transitionTo(status);
    }

    @Override
    @Transactional
    public void delete(UUID deviceId, UUID executionId) {
        commandExecutionRepository.delete(getExecution(deviceId, executionId));
    }

    private CommandExecution getExecution(UUID deviceId, UUID executionId) {
        return commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)
                .orElseThrow(() -> new CommandExecutionNotFoundException(deviceId, executionId));
    }
}
