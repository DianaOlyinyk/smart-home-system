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
import static org.mockito.Mockito.when;

class CloseStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final CloseStrategy strategy = new CloseStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("close")).isTrue();
        assertThat(strategy.supports("open")).isFalse();
    }

    @Test
    void sendsCommandToDevice() {
        UUID deviceId = UUID.randomUUID();
        when(deviceClient.send(any(UUID.class), eq("close"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("close"), Map.of());

        verify(deviceClient).send(deviceId, "close", Map.of());
        assertThat(result.isSuccess()).isTrue();
    }

    private static Command command(String name) {
        return new Command(null, name, null, null, null);
    }
}
