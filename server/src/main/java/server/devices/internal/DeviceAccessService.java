package server.devices.internal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.devices.AccessAlreadyGrantedException;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceAccess;
import server.devices.DeviceAccessNotFoundException;
import server.devices.DeviceAccessRepository;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.users.User;
import server.users.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class DeviceAccessService {

    private final DeviceRepository deviceRepository;
    private final DeviceAccessRepository accessRepository;
    private final UserService userService;

    DeviceAccessService(
            DeviceRepository deviceRepository,
            DeviceAccessRepository accessRepository,
            UserService userService) {
        this.deviceRepository = deviceRepository;
        this.accessRepository = accessRepository;
        this.userService = userService;
    }

    DeviceAccess grant(
            UUID deviceId,
            String email,
            AccessRole role,
            UUID grantedById) {

        Device device = deviceRepository.findByIdWithAccesses(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        User targetUser = userService.getUserByEmail(email);
        if (accessRepository.existsByDeviceIdAndUserId(
                deviceId, targetUser.getId())) {
            throw new AccessAlreadyGrantedException();
        }

        User grantedBy = userService.getUser(grantedById);

        device.grantAccess(
                targetUser,
                role,
                grantedBy,
                Instant.now()
        );

        Device saved = deviceRepository.save(device);
        return saved.getAccesses().stream()
                .filter(access ->
                        access.getUserId().equals(targetUser.getId()))
                .findFirst()
                .orElseThrow();
    }

    void revoke(UUID deviceId, UUID userId) {
        Device device = deviceRepository.findByIdWithAccesses(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        boolean removed = device.revokeAccess(userId);
        if (!removed) {
            throw new DeviceAccessNotFoundException();
        }
        deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    List<DeviceAccess> findByDevice(UUID deviceId) {
        Device device = deviceRepository.findByIdWithAccesses(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        return List.copyOf(device.getAccesses());
    }

    @Transactional(readOnly = true)
    List<DeviceAccess> findByUser(UUID userId) {
        return accessRepository.findAllByUserIdWithDevice(userId);
    }
}