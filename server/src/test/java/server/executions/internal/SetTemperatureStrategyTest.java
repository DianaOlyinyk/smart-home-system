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

class SetTemperatureStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final SetTemperatureStrategy strategy = new SetTemperatureStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("set_temperature")).isTrue();
        assertThat(strategy.supports("set_mode")).isFalse();
    }

    @Test
    void sendsTemperatureToDevice() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("temperature", 21);
        when(deviceClient.send(any(UUID.class), eq("set_temperature"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("set_temperature"), args);

        verify(deviceClient).send(deviceId, "set_temperature", args);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void passesFractionalTemperatureThroughUntouched() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("temperature", 21.5);
        when(deviceClient.send(any(UUID.class), eq("set_temperature"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        strategy.execute(deviceId, command("set_temperature"), args);

        verify(deviceClient).send(deviceId, "set_temperature", Map.of("temperature", 21.5));
    }

    @Test
    void leavesRangeEnforcementToTheCommandSchema() {
        UUID deviceId = UUID.randomUUID();
        Map<String, Object> args = Map.of("temperature", 95);
        when(deviceClient.send(any(UUID.class), eq("set_temperature"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("set_temperature"), args);

        verify(deviceClient).send(deviceId, "set_temperature", args);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNonNumericTemperatureWithoutCallingDevice() {
        ExecutionOutcome result =
                strategy.execute(UUID.randomUUID(), command("set_temperature"), Map.of("temperature", "warm"));

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    @Test
    void rejectsMissingTemperatureWithoutCallingDevice() {
        ExecutionOutcome result = strategy.execute(UUID.randomUUID(), command("set_temperature"), Map.of());

        assertThat(result.isSuccess()).isFalse();
        verifyNoInteractions(deviceClient);
    }

    private static Command command(String name) {
        return new Command(null, name, null, null, null);
    }
}
