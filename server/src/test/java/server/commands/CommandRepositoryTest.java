package server.commands;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

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

    private final UUID deviceId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private Command save(UUID deviceId, String name, RequiredRole role) {
        return commandRepository.saveAndFlush(new Command(deviceId, name, "{}", role, now));
    }

    @Test
    void saveGeneratesId() {
        Command saved = save(deviceId, "turn_on", RequiredRole.GUEST);

        assertNotNull(saved.getId());
        assertEquals("turn_on", commandRepository.findById(saved.getId()).orElseThrow().getName());
    }

    @Test
    void findByDeviceIdAndCommandIdFindsOwnCommand() {
        Command saved = save(deviceId, "turn_on", RequiredRole.GUEST);

        assertEquals(saved.getId(),
                commandRepository.findByDeviceIdAndCommandId(deviceId, saved.getId()).orElseThrow().getId());
    }

    @Test
    void findByDeviceIdAndCommandIdIgnoresOtherDevicesCommand() {
        Command otherDevicesCommand = save(UUID.randomUUID(), "unlock", RequiredRole.OWNER);

        assertTrue(commandRepository.findByDeviceIdAndCommandId(deviceId, otherDevicesCommand.getId()).isEmpty());
    }

    @Test
    void findByDeviceIdReturnsOnlyThatDeviceSortedByName() {
        save(deviceId, "turn_on", RequiredRole.GUEST);
        save(deviceId, "set_brightness", RequiredRole.OWNER);
        save(UUID.randomUUID(), "turn_off", RequiredRole.GUEST);

        List<String> names = commandRepository.findByDeviceIdOrderByNameAsc(deviceId).stream()
                .map(Command::getName)
                .toList();

        assertEquals(List.of("set_brightness", "turn_on"), names);
    }

    @Test
    void findByDeviceIdAndRequiredRoleFiltersByRole() {
        save(deviceId, "turn_on", RequiredRole.GUEST);
        save(deviceId, "set_brightness", RequiredRole.OWNER);
        save(deviceId, "set_mode", RequiredRole.OWNER);

        List<String> names = commandRepository
                .findByDeviceIdAndRequiredRoleOrderByNameAsc(deviceId, RequiredRole.OWNER).stream()
                .map(Command::getName)
                .toList();

        assertEquals(List.of("set_brightness", "set_mode"), names);
    }

    @Test
    void existsByDeviceIdAndName() {
        save(deviceId, "turn_on", RequiredRole.GUEST);

        assertTrue(commandRepository.existsByDeviceIdAndName(deviceId, "turn_on"));
        assertFalse(commandRepository.existsByDeviceIdAndName(deviceId, "turn_off"));
        assertFalse(commandRepository.existsByDeviceIdAndName(UUID.randomUUID(), "turn_on"));
    }

    @Test
    void sameNameOnSameDeviceIsRejected() {
        save(deviceId, "turn_on", RequiredRole.GUEST);

        assertThrows(DataIntegrityViolationException.class,
                () -> save(deviceId, "turn_on", RequiredRole.OWNER));
    }

    @Test
    void sameNameOnDifferentDevicesIsAllowed() {
        save(deviceId, "turn_on", RequiredRole.GUEST);
        save(UUID.randomUUID(), "turn_on", RequiredRole.GUEST);

        assertEquals(1, commandRepository.findByDeviceIdOrderByNameAsc(deviceId).size());
    }
}
