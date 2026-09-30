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

class SetBrightnessStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final SetBrightnessStrategy strategy = new SetBrightnessStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("set_brightness")).isTrue();
        assertThat(strategy.supports("set_temperature")).isFalse();
    }

    @Test
    void sendsBrightnessToDevice() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("brightness", 80);
        when(deviceClient.send(any(UUID.class), eq("set_brightness"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("set_brightness"), args);

        verify(deviceClient).send(deviceId, "set_brightness", args);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsBrightnessOutOfRangeWithoutCallingDevice() {
        ExecutionOutcome result =
                strategy.execute(UUID.randomUUID(), command("set_brightness"), Map.of("brightness", 150));

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    @Test
    void rejectsMissingBrightnessWithoutCallingDevice() {
        ExecutionOutcome result = strategy.execute(UUID.randomUUID(), command("set_brightness"), Map.of());

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    private static Command command(String name) {
        return new Command(UUID.randomUUID(), name, null, null, null);
    }
}
