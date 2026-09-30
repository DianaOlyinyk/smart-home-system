package server.commands.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import server.commands.Command;
import server.commands.CommandAlreadyExistsException;
import server.commands.CommandRepository;
import server.commands.CommandNotFoundException;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;
import server.devices.DeviceType;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        Device device = mock(Device.class);
        when(deviceService.findById(deviceId)).thenReturn(device);
        when(commandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Command result = commandService.create(deviceId, "turn_on", "{}", RequiredRole.GUEST);

        assertSame(device, result.getDevice());
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

    @Test
    void createThrowsWhenNameAlreadyExistsOnDevice() {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenReturn(mock(Device.class));
        when(commandRepository.existsByDeviceIdAndName(deviceId, "turn_on")).thenReturn(true);

        assertThrows(CommandAlreadyExistsException.class, () ->
                commandService.create(deviceId, "turn_on", "{}", RequiredRole.GUEST));
        verify(commandRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenConstraintFiresOnConcurrentDuplicate() {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenReturn(mock(Device.class));
        when(commandRepository.existsByDeviceIdAndName(deviceId, "turn_on")).thenReturn(false);
        when(commandRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(CommandAlreadyExistsException.class, () ->
                commandService.create(deviceId, "turn_on", "{}", RequiredRole.GUEST));
    }

    @Test
    void findAllByDeviceWithoutRoleReturnsAllCommands() {
        UUID deviceId = UUID.randomUUID();
        List<Command> commands = List.of(command(deviceId, "turn_on"));
        when(deviceService.findById(deviceId)).thenReturn(mock(Device.class));
        when(commandRepository.findByDeviceIdOrderByNameAsc(deviceId)).thenReturn(commands);

        assertEquals(commands, commandService.findAllByDevice(deviceId, null));
    }

    @Test
    void findAllByDeviceWithRoleFiltersByRole() {
        UUID deviceId = UUID.randomUUID();
        List<Command> commands = List.of(command(deviceId, "set_mode"));
        when(deviceService.findById(deviceId)).thenReturn(mock(Device.class));
        when(commandRepository.findByDeviceIdAndRequiredRoleOrderByNameAsc(deviceId, RequiredRole.OWNER))
                .thenReturn(commands);

        assertEquals(commands, commandService.findAllByDevice(deviceId, RequiredRole.OWNER));
    }

    @Test
    void findAllByDeviceThrowsWhenDeviceMissing() {
        UUID deviceId = UUID.randomUUID();
        when(deviceService.findById(deviceId)).thenThrow(new DeviceNotFoundException(deviceId));

        assertThrows(DeviceNotFoundException.class, () -> commandService.findAllByDevice(deviceId, null));
        verifyNoInteractions(commandRepository);
    }

    @Test
    void updateChangesFieldsAndSaves() {
        UUID deviceId = UUID.randomUUID(), commandId = UUID.randomUUID();
        Command command = command(deviceId, "set_mode");
        when(commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(Optional.of(command));

        commandService.update(deviceId, commandId, "{\"type\":\"object\"}", RequiredRole.OWNER);

        assertEquals("{\"type\":\"object\"}", command.getArgsSchema());
        assertEquals(RequiredRole.OWNER, command.getRequiredRole());
        verify(commandRepository).save(command);
    }

    @Test
    void updateThrowsWhenCommandMissing() {
        UUID deviceId = UUID.randomUUID(), commandId = UUID.randomUUID();
        when(commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(Optional.empty());

        assertThrows(CommandNotFoundException.class, () ->
                commandService.update(deviceId, commandId, "{}", RequiredRole.OWNER));
        verify(commandRepository, never()).save(any());
    }

    @Test
    void deleteRemovesCommand() {
        UUID deviceId = UUID.randomUUID(), commandId = UUID.randomUUID();
        Command command = command(deviceId, "turn_on");
        when(commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(Optional.of(command));

        commandService.delete(deviceId, commandId);

        verify(commandRepository).delete(command);
    }

    @Test
    void deleteThrowsWhenCommandMissing() {
        UUID deviceId = UUID.randomUUID(), commandId = UUID.randomUUID();
        when(commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)).thenReturn(Optional.empty());

        assertThrows(CommandNotFoundException.class, () -> commandService.delete(deviceId, commandId));
        verify(commandRepository, never()).delete(any());
    }

    private static Command command(UUID deviceId, String name) {
        Device device = new Device("Lamp", DeviceType.LAMP, "token", Instant.now());
        ReflectionTestUtils.setField(device, "id", deviceId);
        return new Command(device, name, "{}", RequiredRole.GUEST, Instant.now());
    }
}