package server.devices.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceAccess;
import server.devices.DeviceAccessRepository;
import server.devices.DeviceType;
import server.users.User;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeviceAccessRepositoryTest {

    @Autowired
    private DeviceAccessRepository accessRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void existsByDeviceIdAndUserIdFindsOnlyGrantedAccess() {
        User owner = persistUser();
        User guest = persistUser();
        Device device = persistDeviceWithAccess(guest, owner);

        assertThat(accessRepository.existsByDeviceIdAndUser_Id(device.getId(), guest.getId())).isTrue();
        assertThat(accessRepository.existsByDeviceIdAndUser_Id(device.getId(), owner.getId())).isFalse();
    }

    @Test
    void deletingUserRemovesTheirAccesses() {
        User owner = persistUser();
        User guest = persistUser();
        Device device = persistDeviceWithAccess(guest, owner);

        entityManager.remove(entityManager.find(User.class, guest.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(accessRepository.existsByDeviceIdAndUser_Id(device.getId(), guest.getId())).isFalse();
    }

    @Test
    void deletingGrantorKeepsAccessWithoutGrantor() {
        User owner = persistUser();
        User guest = persistUser();
        Device device = persistDeviceWithAccess(guest, owner);

        entityManager.remove(entityManager.find(User.class, owner.getId()));
        entityManager.flush();
        entityManager.clear();

        DeviceAccess access = accessRepository.findAll().getFirst();
        assertThat(access.getDevice().getId()).isEqualTo(device.getId());
        assertThat(access.getGrantedById()).isNull();
    }

    private Device persistDeviceWithAccess(User user, User grantedBy) {
        Device device = new Device("Lamp", DeviceType.LAMP, UUID.randomUUID().toString(), Instant.now());
        device.grantAccess(user, AccessRole.GUEST, grantedBy, Instant.now());
        entityManager.persist(device);
        entityManager.flush();
        entityManager.clear();
        return device;
    }

    private User persistUser() {
        User user = new User(UUID.randomUUID() + "@example.test", "hash", "Test user", Instant.now());
        entityManager.persist(user);
        return user;
    }
}
