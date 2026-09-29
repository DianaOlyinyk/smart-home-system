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
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.executions.CommandExecutedEvent;
import server.executions.CommandExecution;
import server.executions.CommandExecutionNotFoundException;
import server.executions.CommandExecutionRepository;
import server.executions.CommandExecutionStrategy;
import server.executions.ExecutionOutcome;
import server.executions.ExecutionStatus;
import server.executions.InvalidStateTransitionException;
import server.executions.UnsupportedCommandException;
import server.users.User;
import server.users.UserNotFoundException;
import server.users.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandExecutionServiceImplTest {

    @Mock
    private CommandService commandService;

    @Mock
    private DeviceService deviceService;

    @Mock
    private UserService userService;

    @Mock
    private CommandArgsValidator commandArgsValidator;

    @Mock
    private CommandExecutionRepository commandExecutionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final UUID deviceId = UUID.randomUUID();
    private final UUID commandId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();
    private final Device device = withId(new Device("Лампа", DeviceType.LAMP, "token", Instant.now()), deviceId);
    private final Command command = withId(
            new Command(device, "set_brightness", "{}", RequiredRole.OWNER, Instant.now()), commandId);
    private final User user = withId(new User("user@example.com", "hash", "Олена", Instant.now()), userId);
    private final Map<String, Object> args = Map.of("brightness", 80);

    @BeforeEach
    void setUp() {
        lenient().when(commandExecutionRepository.save(any()))
                .thenAnswer(invocation -> {
                    CommandExecution execution = invocation.getArgument(0);
                    if (execution.getId() == null) {
                        withId(execution, UUID.randomUUID());
                    }
                    return execution;
                });
    }

    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private CommandExecutionServiceImpl service(CommandExecutionStrategy... strategies) {
        return new CommandExecutionServiceImpl(
                commandService, deviceService, userService, commandArgsValidator, commandExecutionRepository,
                new CommandExecutionStrategyResolver(List.of(strategies)), eventPublisher);
    }

    private CommandExecutionStrategy strategyReturning(ExecutionOutcome outcome) {
        CommandExecutionStrategy strategy = mock(CommandExecutionStrategy.class);
        when(strategy.supports("set_brightness")).thenReturn(true);
        when(strategy.execute(eq(deviceId), eq(command), eq(args))).thenReturn(outcome);
        return strategy;
    }

    private CommandExecution storedExecution(ExecutionStatus status) {
        CommandExecution execution = withId(new CommandExecution(command, device, user, args, Instant.now()), executionId);
        ReflectionTestUtils.setField(execution, "status", status);
        return execution;
    }

    @Test
    void executeReturnsSuccessAndPublishesEvent() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);
        when(userService.getUser(userId)).thenReturn(user);

        CommandExecution execution = service(strategyReturning(ExecutionOutcome.success("OK")))
                .execute(deviceId, commandId, userId, args);

        assertEquals(ExecutionStatus.SUCCESS, execution.getStatus());
        assertNotNull(execution.getId());
        assertNotNull(execution.getCompletedAt());
        assertSame(command, execution.getCommand());
        assertSame(device, execution.getDevice());
        assertSame(user, execution.getExecutedBy());
        assertEquals(args, execution.getArgs());
        verify(commandArgsValidator).validate(command.getArgsSchema(), args);
        verify(commandExecutionRepository, times(2)).save(any());
        verify(eventPublisher).publishEvent(new CommandExecutedEvent(execution.getId(), ExecutionStatus.SUCCESS));
    }

    @Test
    void executeWithoutUserLeavesExecutorEmpty() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);

        CommandExecution execution = service(strategyReturning(ExecutionOutcome.success("OK")))
                .execute(deviceId, commandId, null, args);

        assertNull(execution.getExecutedBy());
        verifyNoInteractions(userService);
    }

    @Test
    void executeReturnsFailedWhenStrategyReportsFailure() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);
        when(userService.getUser(userId)).thenReturn(user);

        CommandExecution execution = service(strategyReturning(ExecutionOutcome.rejected("Device rejected the command")))
                .execute(deviceId, commandId, userId, args);

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        verify(eventPublisher).publishEvent(new CommandExecutedEvent(execution.getId(), ExecutionStatus.FAILED));
    }

    @Test
    void executeThrowsWhenNoStrategySupportsCommand() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);
        when(userService.getUser(userId)).thenReturn(user);

        CommandExecutionStrategy strategy = mock(CommandExecutionStrategy.class);
        when(strategy.supports("set_brightness")).thenReturn(false);

        assertThrows(UnsupportedCommandException.class,
                () -> service(strategy).execute(deviceId, commandId, userId, args));

        verify(commandExecutionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void executeThrowsWhenUserDoesNotExist() {
        when(commandService.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(command);
        when(userService.getUser(userId)).thenThrow(new UserNotFoundException(userId));

        assertThrows(UserNotFoundException.class,
                () -> service().execute(deviceId, commandId, userId, args));

        verify(commandExecutionRepository, never()).save(any());
    }

    @Test
    void findJournalWithoutUserReturnsFullJournal() {
        List<CommandExecution> journal = List.of(storedExecution(ExecutionStatus.SUCCESS));
        when(commandExecutionRepository.findJournalByDeviceId(deviceId)).thenReturn(journal);

        assertEquals(journal, service().findJournal(deviceId, null));

        verify(deviceService).findById(deviceId);
        verify(commandExecutionRepository, never())
                .findByDeviceIdAndExecutedByIdOrderByRequestedAtDesc(any(), any());
    }

    @Test
    void findJournalWithUserReturnsOnlyUserExecutions() {
        List<CommandExecution> journal = List.of(storedExecution(ExecutionStatus.SUCCESS));
        when(commandExecutionRepository.findByDeviceIdAndExecutedByIdOrderByRequestedAtDesc(deviceId, userId))
                .thenReturn(journal);

        assertEquals(journal, service().findJournal(deviceId, userId));

        verify(commandExecutionRepository, never()).findJournalByDeviceId(any());
    }

    @Test
    void findJournalThrowsWhenDeviceDoesNotExist() {
        when(deviceService.findById(deviceId)).thenThrow(new DeviceNotFoundException(deviceId));

        assertThrows(DeviceNotFoundException.class, () -> service().findJournal(deviceId, null));

        verifyNoInteractions(commandExecutionRepository);
    }

    @Test
    void findByIdReturnsExecutionOfDevice() {
        CommandExecution execution = storedExecution(ExecutionStatus.SUCCESS);
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.of(execution));

        assertSame(execution, service().findById(deviceId, executionId));
    }

    @Test
    void findByIdThrowsWhenExecutionNotFound() {
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.empty());

        assertThrows(CommandExecutionNotFoundException.class, () -> service().findById(deviceId, executionId));
    }

    @Test
    void changeStatusAppliesAllowedTransition() {
        CommandExecution execution = storedExecution(ExecutionStatus.RUNNING);
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.of(execution));

        CommandExecution updated = service().changeStatus(deviceId, executionId, ExecutionStatus.TIMEOUT);

        assertSame(execution, updated);
        assertEquals(ExecutionStatus.TIMEOUT, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void changeStatusRejectsForbiddenTransition() {
        CommandExecution execution = storedExecution(ExecutionStatus.SUCCESS);
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.of(execution));

        assertThrows(InvalidStateTransitionException.class,
                () -> service().changeStatus(deviceId, executionId, ExecutionStatus.RUNNING));
        assertEquals(ExecutionStatus.SUCCESS, execution.getStatus());
    }

    @Test
    void changeStatusThrowsWhenExecutionNotFound() {
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.empty());

        assertThrows(CommandExecutionNotFoundException.class,
                () -> service().changeStatus(deviceId, executionId, ExecutionStatus.TIMEOUT));
    }

    @Test
    void deleteRemovesExecution() {
        CommandExecution execution = storedExecution(ExecutionStatus.SUCCESS);
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.of(execution));

        service().delete(deviceId, executionId);

        verify(commandExecutionRepository).delete(execution);
    }

    @Test
    void deleteThrowsWhenExecutionNotFound() {
        when(commandExecutionRepository.findByIdAndDeviceId(executionId, deviceId)).thenReturn(Optional.empty());

        assertThrows(CommandExecutionNotFoundException.class, () -> service().delete(deviceId, executionId));

        verify(commandExecutionRepository, never()).delete(any());
    }
}
