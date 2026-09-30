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

class SetModeStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final SetModeStrategy strategy = new SetModeStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("set_mode")).isTrue();
        assertThat(strategy.supports("set_brightness")).isFalse();
    }

    @Test
    void sendsModeToDevice() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("mode", "cool");
        when(deviceClient.send(any(UUID.class), eq("set_mode"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("set_mode"), args);

        verify(deviceClient).send(deviceId, "set_mode", args);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsUnknownModeWithoutCallingDevice() {
        ExecutionOutcome result =
                strategy.execute(UUID.randomUUID(), command("set_mode"), Map.of("mode", "turbo"));

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    private static Command command(String name) {
        return new Command(null, name, null, null, null);
    }
}
