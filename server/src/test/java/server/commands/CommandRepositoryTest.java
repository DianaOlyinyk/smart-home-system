package server.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import server.devices.Device;
import server.devices.DeviceRepository;
import server.devices.DeviceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CommandRepositoryTest {

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private Device device;
    private Device otherDevice;

    @BeforeEach
    void setUp() {
        device = saveDevice("Lamp");
        otherDevice = saveDevice("Kettle");
    }

    private Device saveDevice(String name) {
        return deviceRepository.saveAndFlush(new Device(name, DeviceType.LAMP, UUID.randomUUID().toString(), now));
    }

    private Command save(Device device, String name, RequiredRole role) {
        return commandRepository.saveAndFlush(new Command(device, name, "{}", role, now));
    }

    @Test
    void saveGeneratesId() {
        Command saved = save(device, "turn_on", RequiredRole.GUEST);

        assertNotNull(saved.getId());
        assertEquals("turn_on", commandRepository.findById(saved.getId()).orElseThrow().getName());
    }

    @Test
    void findByDeviceIdAndCommandIdFindsOwnCommand() {
        Command saved = save(device, "turn_on", RequiredRole.GUEST);

        assertEquals(saved.getId(),
                commandRepository.findByDeviceIdAndCommandId(device.getId(), saved.getId()).orElseThrow().getId());
    }

    @Test
    void findByDeviceIdAndCommandIdIgnoresOtherDevicesCommand() {
        Command otherDevicesCommand = save(otherDevice, "unlock", RequiredRole.OWNER);

        assertTrue(commandRepository.findByDeviceIdAndCommandId(device.getId(), otherDevicesCommand.getId()).isEmpty());
    }

    @Test
    void findByDeviceIdReturnsOnlyThatDeviceSortedByName() {
        save(device, "turn_on", RequiredRole.GUEST);
        save(device, "set_brightness", RequiredRole.OWNER);
        save(otherDevice, "turn_off", RequiredRole.GUEST);

        List<String> names = commandRepository.findByDeviceIdOrderByNameAsc(device.getId()).stream()
                .map(Command::getName)
                .toList();

        assertEquals(List.of("set_brightness", "turn_on"), names);
    }

    @Test
    void findByDeviceIdAndRequiredRoleFiltersByRole() {
        save(device, "turn_on", RequiredRole.GUEST);
        save(device, "set_brightness", RequiredRole.OWNER);
        save(device, "set_mode", RequiredRole.OWNER);

        List<String> names = commandRepository
                .findByDeviceIdAndRequiredRoleOrderByNameAsc(device.getId(), RequiredRole.OWNER).stream()
                .map(Command::getName)
                .toList();

        assertEquals(List.of("set_brightness", "set_mode"), names);
    }

    @Test
    void existsByDeviceIdAndName() {
        save(device, "turn_on", RequiredRole.GUEST);

        assertTrue(commandRepository.existsByDeviceIdAndName(device.getId(), "turn_on"));
        assertFalse(commandRepository.existsByDeviceIdAndName(device.getId(), "turn_off"));
        assertFalse(commandRepository.existsByDeviceIdAndName(otherDevice.getId(), "turn_on"));
    }

    @Test
    void sameNameOnSameDeviceIsRejected() {
        save(device, "turn_on", RequiredRole.GUEST);

        assertThrows(DataIntegrityViolationException.class,
                () -> save(device, "turn_on", RequiredRole.OWNER));
    }

    @Test
    void sameNameOnDifferentDevicesIsAllowed() {
        save(device, "turn_on", RequiredRole.GUEST);
        save(otherDevice, "turn_on", RequiredRole.GUEST);

        assertEquals(1, commandRepository.findByDeviceIdOrderByNameAsc(device.getId()).size());
    }
}
