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

class OpenStrategyTest {

    private final DeviceClient deviceClient = mock(DeviceClient.class);
    private final OpenStrategy strategy = new OpenStrategy(deviceClient);

    @Test
    void supportsOnlyItsOwnCommand() {
        assertThat(strategy.supports("open")).isTrue();
        assertThat(strategy.supports("close")).isFalse();
    }

    @Test
    void sendsCommandToDevice() {
        UUID deviceId = UUID.randomUUID();
        when(deviceClient.send(any(UUID.class), eq("open"), anyMap()))
                .thenReturn(ExecutionOutcome.success("ok"));

        ExecutionOutcome result = strategy.execute(deviceId, command("open"), Map.of());

        verify(deviceClient).send(deviceId, "open", Map.of());
        assertThat(result.isSuccess()).isTrue();
    }

    private static Command command(String name) {
        return new Command(UUID.randomUUID(), name, null, null, null);
    }
}
