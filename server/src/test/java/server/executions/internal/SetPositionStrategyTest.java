package server.executions.internal;

import org.junit.jupiter.api.Test;
import server.commands.Command;
import server.executions.DeviceClient;
import server.executions.ExecutionOutcome;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SetPositionStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final SetPositionStrategy strategy = new SetPositionStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("set_position")).isTrue();
        assertThat(strategy.supports("open")).isFalse();
    }

    @Test
    void sendsPositionToDevice() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("position", 40);
        when(deviceClient.send(any(UUID.class), eq("set_position"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("set_position"), args);

        verify(deviceClient).send(deviceId, "set_position", args);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNegativePositionWithoutCallingDevice() {
        ExecutionOutcome result =
                strategy.execute(UUID.randomUUID(), command("set_position"), Map.of("position", -1));

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    private static Command command(String name) {
        return new Command(UUID.randomUUID(), name, null, null, null);
    }
}
