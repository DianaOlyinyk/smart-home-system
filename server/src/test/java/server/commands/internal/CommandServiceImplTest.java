package server.commands.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.commands.Command;
import server.commands.CommandRepository;
import server.commands.CommandNotFoundException;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceImplTest {
    @Mock
    CommandRepository commandRepository;
    @Mock
    DeviceService deviceService;
    CommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new CommandServiceImpl(commandRepository, deviceService, Clock.systemUTC());
    }

    @Test
    void createsCommandWhenDeviceExists() {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenReturn(mock(Device.class));
        when(commandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Command result = commandService.create(deviceId, "turn_on", "{}", RequiredRole.GUEST);

        assertEquals(deviceId, result.deviceId());
        verify(commandRepository).save(any());
    }

    @Test
    void throwsWhenDeviceMissing() {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenThrow(new DeviceNotFoundException(deviceId));

        assertThrows(DeviceNotFoundException.class, () ->
                commandService.create(deviceId, "turn_on", "{}", RequiredRole.GUEST));
        verifyNoInteractions(commandRepository);
    }

    @Test
    void throwsWhenCommandMissing() {
        UUID deviceId = UUID.randomUUID(), commandId = UUID.randomUUID();
        when(commandRepository.findByDeviceIdAndCommandId(deviceId, commandId))
                .thenReturn(Optional.empty());

        assertThrows(CommandNotFoundException.class, () ->
                commandService.findByDeviceIdAndCommandId(deviceId, commandId));
    }
}