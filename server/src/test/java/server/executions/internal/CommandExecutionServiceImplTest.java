package server.executions.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.commands.Command;
import server.commands.CommandService;
import server.commands.RequiredRole;
import server.executions.CommandExecutedEvent;
import server.executions.CommandExecution;
import server.executions.CommandExecutionRepository;
import server.executions.CommandExecutionStrategy;
import server.executions.ExecutionOutcome;
import server.executions.ExecutionStatus;
import server.executions.UnsupportedCommandException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandExecutionServiceImplTest {

    @Mock
    private CommandService commandService;

    @Mock
    private CommandArgsValidator commandArgsValidator;

    @Mock
    private CommandExecutionRepository commandExecutionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final UUID deviceId = UUID.randomUUID();
    private final UUID commandId = UUID.randomUUID();
    private final Command command = new Command(
            deviceId, "set_brightness", "{}", RequiredRole.OWNER, Instant.now());
    private final Map<String, Object> args = Map.of("brightness", 80);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "id", commandId);
        org.mockito.Mockito.lenient().when(commandExecutionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void executeReturnsSuccessAndPublishesEvent() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);

        CommandExecutionStrategy strategy = mock(CommandExecutionStrategy.class);
        when(strategy.supports("set_brightness")).thenReturn(true);
        when(strategy.execute(eq(deviceId), eq(command), eq(args)))
                .thenReturn(ExecutionOutcome.success("OK"));

        CommandExecutionServiceImpl service = new CommandExecutionServiceImpl(
                commandService, commandArgsValidator, commandExecutionRepository,
                new CommandExecutionStrategyResolver(List.of(strategy)), eventPublisher);

        CommandExecution execution = service.execute(deviceId, commandId, args);

        assertEquals(ExecutionStatus.SUCCESS, execution.status());
        verify(commandArgsValidator).validate(command.getArgsSchema(), args);
        verify(commandExecutionRepository, org.mockito.Mockito.times(2)).save(any());
        verify(eventPublisher).publishEvent(new CommandExecutedEvent(execution.id(), ExecutionStatus.SUCCESS));
    }

    @Test
    void executeReturnsFailedWhenStrategyReportsFailure() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);

        CommandExecutionStrategy strategy = mock(CommandExecutionStrategy.class);
        when(strategy.supports("set_brightness")).thenReturn(true);
        when(strategy.execute(eq(deviceId), eq(command), eq(args)))
                .thenReturn(ExecutionOutcome.rejected("Device rejected the command"));

        CommandExecutionServiceImpl service = new CommandExecutionServiceImpl(
                commandService, commandArgsValidator, commandExecutionRepository,
                new CommandExecutionStrategyResolver(List.of(strategy)), eventPublisher);

        CommandExecution execution = service.execute(deviceId, commandId, args);

        assertEquals(ExecutionStatus.FAILED, execution.status());
        verify(eventPublisher).publishEvent(new CommandExecutedEvent(execution.id(), ExecutionStatus.FAILED));
    }

    @Test
    void executeThrowsWhenNoStrategySupportsCommand() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);

        CommandExecutionStrategy strategy = mock(CommandExecutionStrategy.class);
        when(strategy.supports("set_brightness")).thenReturn(false);

        CommandExecutionServiceImpl service = new CommandExecutionServiceImpl(
                commandService, commandArgsValidator, commandExecutionRepository,
                new CommandExecutionStrategyResolver(List.of(strategy)), eventPublisher);

        assertThrows(UnsupportedCommandException.class, () -> service.execute(deviceId, commandId, args));

        verify(eventPublisher, never()).publishEvent(any());
    }
}
