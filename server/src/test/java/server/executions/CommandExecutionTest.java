package server.executions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandExecutionTest {

    private static final Set<Arguments> ALLOWED_TRANSITIONS = Set.of(
            Arguments.of(ExecutionStatus.PENDING, ExecutionStatus.RUNNING),
            Arguments.of(ExecutionStatus.RUNNING, ExecutionStatus.SUCCESS),
            Arguments.of(ExecutionStatus.RUNNING, ExecutionStatus.FAILED),
            Arguments.of(ExecutionStatus.RUNNING, ExecutionStatus.TIMEOUT));

    static Stream<Arguments> allowedTransitions() {
        return ALLOWED_TRANSITIONS.stream();
    }

    static Stream<Arguments> forbiddenTransitions() {
        return EnumSet.allOf(ExecutionStatus.class).stream()
                .flatMap(from -> EnumSet.allOf(ExecutionStatus.class).stream()
                        .map(to -> Arguments.of(from, to)))
                .filter(pair -> !isAllowed(pair));
    }

    private static boolean isAllowed(Arguments pair) {
        Object[] values = pair.get();
        return ALLOWED_TRANSITIONS.stream()
                .anyMatch(allowed -> allowed.get()[0] == values[0] && allowed.get()[1] == values[1]);
    }

    private CommandExecution newExecution() {
        return new CommandExecution(null, null, null, Map.of(), Instant.now());
    }

    private CommandExecution executionWithStatus(ExecutionStatus status) {
        CommandExecution execution = newExecution();
        ReflectionTestUtils.setField(execution, "status", status);
        return execution;
    }

    @Test
    void newExecutionIsPendingAndNotCompleted() {
        CommandExecution execution = newExecution();

        assertEquals(ExecutionStatus.PENDING, execution.getStatus());
        assertNull(execution.getCompletedAt());
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowedTransitions")
    void allowedTransitionChangesStatusInPlace(ExecutionStatus from, ExecutionStatus to) {
        CommandExecution execution = executionWithStatus(from);

        CommandExecution updated = execution.transitionTo(to);

        assertSame(execution, updated);
        assertEquals(to, execution.getStatus());
    }

    @ParameterizedTest(name = "{0} -> {1} is forbidden")
    @MethodSource("forbiddenTransitions")
    void forbiddenTransitionThrows(ExecutionStatus from, ExecutionStatus to) {
        CommandExecution execution = executionWithStatus(from);

        assertThrows(InvalidStateTransitionException.class,
                () -> execution.transitionTo(to));
        assertEquals(from, execution.getStatus());
    }

    @Test
    void runningDoesNotSetCompletedAt() {
        CommandExecution execution = newExecution().transitionTo(ExecutionStatus.RUNNING);

        assertNull(execution.getCompletedAt());
    }

    @ParameterizedTest(name = "RUNNING -> {0} sets completedAt")
    @MethodSource("terminalStatuses")
    void terminalTransitionSetsCompletedAt(ExecutionStatus terminal) {
        CommandExecution execution = newExecution().transitionTo(ExecutionStatus.RUNNING);

        execution.transitionTo(terminal);

        assertNotNull(execution.getCompletedAt());
    }

    static Stream<ExecutionStatus> terminalStatuses() {
        return Stream.of(ExecutionStatus.SUCCESS, ExecutionStatus.FAILED, ExecutionStatus.TIMEOUT);
    }
}
