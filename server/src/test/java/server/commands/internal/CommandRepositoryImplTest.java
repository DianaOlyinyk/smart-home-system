package server.commands.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import server.commands.Command;
import server.commands.CommandRepository;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceRepository;
import server.devices.DeviceType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommandRepositoryImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void deletingDeviceRemovesOnlyItsCommands() {
        Device deletedDevice = entityManager.persist(new Device("Lamp", DeviceType.LAMP, "token-delete", NOW));
        Device retainedDevice = entityManager.persist(new Device("Kettle", DeviceType.KETTLE, "token-retain", NOW));
        Command deleted = entityManager.persist(
                new Command(deletedDevice, "turn_on", "{}", RequiredRole.OWNER, NOW));
        Command retained = entityManager.persist(
                new Command(retainedDevice, "turn_off", "{}", RequiredRole.GUEST, NOW));
        entityManager.flush();
        entityManager.clear();

        deviceRepository.deleteById(deletedDevice.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(commandRepository.findById(deleted.getId())).isEmpty();
        assertThat(commandRepository.findById(retained.getId())).isPresent();
    }
}
