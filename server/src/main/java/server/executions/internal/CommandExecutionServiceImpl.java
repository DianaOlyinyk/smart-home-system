package server.executions.internal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import server.commands.Command;
import server.commands.CommandService;
import server.executions.CommandExecutedEvent;
import server.executions.CommandExecution;
import server.executions.CommandExecutionRepository;
import server.executions.CommandExecutionService;
import server.executions.CommandExecutionStrategy;
import server.executions.ExecutionOutcome;
import server.executions.ExecutionStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
class CommandExecutionServiceImpl implements CommandExecutionService {

    private final CommandService commandService;
    private final CommandArgsValidator commandArgsValidator;
    private final CommandExecutionRepository commandExecutionRepository;
    private final CommandExecutionStrategyResolver strategyResolver;
    private final ApplicationEventPublisher eventPublisher;

    CommandExecutionServiceImpl(
            CommandService commandService,
            CommandArgsValidator commandArgsValidator,
            CommandExecutionRepository commandExecutionRepository,
            CommandExecutionStrategyResolver strategyResolver,
            ApplicationEventPublisher eventPublisher) {
        this.commandService = commandService;
        this.commandArgsValidator = commandArgsValidator;
        this.commandExecutionRepository = commandExecutionRepository;
        this.strategyResolver = strategyResolver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CommandExecution execute(UUID deviceId, UUID commandId, Map<String, Object> args) {
        Command command = commandService.findByDeviceIdAndCommandId(deviceId, commandId);
        commandArgsValidator.validate(command.getArgsSchema(), args);

        CommandExecutionStrategy strategy = strategyResolver.resolve(command.getName());

        CommandExecution execution = new CommandExecution(
                command, command.getDevice(), null, args, Instant.now());
        execution = commandExecutionRepository.save(execution.transitionTo(ExecutionStatus.RUNNING));

        ExecutionOutcome outcome = strategy.execute(deviceId, command, args);
        execution = commandExecutionRepository.save(
                execution.transitionTo(outcome.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED));

        eventPublisher.publishEvent(new CommandExecutedEvent(execution.getId(), execution.getStatus()));

        return execution;
    }
}
