package server.devices.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import server.commands.Command;
import server.commands.RequiredRole;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceAccess;
import server.devices.DeviceRepository;
import server.devices.DeviceType;
import server.executions.CommandExecution;
import server.users.User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Should cascade save accesses when device is persisted")
    void shouldCascadeSaveAccesses() {

        Device device = new Device("Living Room Camera", DeviceType.LAMP, "token-123", Instant.now());
        User user = persistUser();
        User grantedBy = persistUser();

        device.grantAccess(user, AccessRole.OWNER, grantedBy, Instant.now());

        Device savedDevice = deviceRepository.save(device);
        entityManager.flush();
        entityManager.clear();

        Device foundDevice = entityManager.find(Device.class, savedDevice.getId());
        assertThat(foundDevice).isNotNull();
        assertThat(foundDevice.getAccesses()).hasSize(1);

        DeviceAccess access = foundDevice.getAccesses().iterator().next();
        assertThat(access.getUserId()).isEqualTo(user.getId());
        assertThat(access.getRole()).isEqualTo(AccessRole.OWNER);
    }

    @Test
    @DisplayName("Should remove access via orphanRemoval when revoked from entity")
    void shouldRemoveAccessViaOrphanRemoval() {

        Device device = new Device("Smart Lock", DeviceType.BLINDS, "token-456", Instant.now());
        User user = persistUser();
        User grantedBy = persistUser();
        device.grantAccess(user, AccessRole.GUEST, grantedBy, Instant.now());

        Device savedDevice = deviceRepository.save(device);
        entityManager.flush();
        entityManager.clear();

        Device loadedDevice = deviceRepository.findById(savedDevice.getId()).orElseThrow();
        loadedDevice.revokeAccess(user.getId());

        deviceRepository.save(loadedDevice);
        entityManager.flush();
        entityManager.clear();

        Device reloadedDevice = entityManager.find(Device.class, savedDevice.getId());
        assertThat(reloadedDevice.getAccesses()).isEmpty();

        Long count = entityManager
            .createQuery("SELECT COUNT(a) FROM DeviceAccess a WHERE a.user.id = :userId", Long.class)
            .setParameter("userId", user.getId())
                .getSingleResult();
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("findAllWithAccesses should execute exactly 1 SQL query")
    void shouldFetchAllWithAccessesInSingleQuery() {

        Device device1 = new Device("Camera 1", DeviceType.LAMP, "token-1", Instant.now());
        device1.grantAccess(persistUser(), AccessRole.OWNER, persistUser(), Instant.now());

        Device device2 = new Device("Camera 2", DeviceType.LAMP, "token-2", Instant.now());
        device2.grantAccess(persistUser(), AccessRole.GUEST, persistUser(), Instant.now());

        entityManager.persist(device1);
        entityManager.persist(device2);
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        long queryCountBefore = sessionFactory.getStatistics().getPrepareStatementCount();

        List<Device> devices = deviceRepository.findAllWithAccesses();

        long queryCountAfter = sessionFactory.getStatistics().getPrepareStatementCount();

        assertThat(devices).hasSize(2);
        assertThat(queryCountAfter - queryCountBefore).isEqualTo(1);
    }

    @Test
    @DisplayName("Deleting a device removes its accesses, commands and executions")
    void deletingDeviceRemovesAccessesCommandsAndExecutions() {
        User user = persistUser();
        Device device = new Device("Lamp", DeviceType.LAMP, "token-delete", Instant.now());
        device.grantAccess(user, AccessRole.OWNER, user, Instant.now());
        entityManager.persist(device);
        Command command = new Command(device, "turn_on", "{}", RequiredRole.GUEST, Instant.now());
        entityManager.persist(command);
        entityManager.persist(new CommandExecution(command, device, user, Map.of("level", 1), Instant.now()));
        entityManager.flush();
        entityManager.clear();

        deviceRepository.delete(deviceRepository.findById(device.getId()).orElseThrow());
        entityManager.flush();
        entityManager.clear();

        assertThat(count("Device")).isZero();
        assertThat(count("DeviceAccess")).isZero();
        assertThat(count("Command")).isZero();
        assertThat(count("CommandExecution")).isZero();
        assertThat(count("User")).isEqualTo(1);
    }

    private long count(String entityName) {
        return entityManager
                .createQuery("SELECT COUNT(e) FROM " + entityName + " e", Long.class)
                .getSingleResult();
    }

    private User persistUser() {
        User user = new User(
                UUID.randomUUID() + "@example.test",
                "hash",
                "Test user",
                Instant.now()
        );
        entityManager.persist(user);
        return user;
    }
}

