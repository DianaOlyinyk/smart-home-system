package server.executions.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import server.commands.Command;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceRepository;
import server.devices.DeviceType;
import server.executions.CommandExecution;
import server.executions.CommandExecutionRepository;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommandExecutionRepositoryImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommandExecutionRepository executionRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void deletingDeviceRemovesOnlyItsExecutions() {
    Device deletedDevice = entityManager.persist(new Device("Lamp", DeviceType.LAMP, "token-delete", NOW));
    Device retainedDevice = entityManager.persist(new Device("Kettle", DeviceType.KETTLE, "token-retain", NOW));
    Command deletedCommand = entityManager.persist(
        new Command(deletedDevice, "turn_on", "{}", RequiredRole.OWNER, NOW));
    Command retainedCommand = entityManager.persist(
        new Command(retainedDevice, "turn_off", "{}", RequiredRole.GUEST, NOW));
    CommandExecution deleted = entityManager.persist(
        new CommandExecution(deletedCommand, deletedDevice, null, Map.of(), NOW));
    CommandExecution retained = entityManager.persist(
        new CommandExecution(retainedCommand, retainedDevice, null, Map.of(), NOW));
    entityManager.flush();
    entityManager.clear();

    deviceRepository.deleteById(deletedDevice.getId());
    entityManager.flush();
    entityManager.clear();

    assertThat(executionRepository.findById(deleted.getId())).isEmpty();
    assertThat(executionRepository.findById(retained.getId())).isPresent();
    }
}
