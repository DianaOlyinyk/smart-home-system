package server.devices.internal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.devices.AccessAlreadyGrantedException;
import server.devices.Device; // Імпорт сутності з публічного пакета
import server.devices.DeviceAccessNotFoundException;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository; // Імпорт репозиторію з публічного пакета
import server.users.User;
import server.users.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
@Service
@Transactional
public class DeviceAccessService {

    private final DeviceRepository deviceRepository;
    private final DeviceAccessRepository accessRepository;
    private final UserService userService;

    public DeviceAccessService(DeviceRepository deviceRepository, DeviceAccessRepository accessRepository, UserService userService) {
        this.deviceRepository = deviceRepository;
        this.accessRepository = accessRepository;
        this.userService = userService;
    }

    public DeviceAccess grant(UUID deviceId, String email, String role, UUID grantedById) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        User targetUser = userService.getUserByEmail(email);
        if (accessRepository.existsByDeviceIdAndUserId(deviceId, targetUser.getId())) {
            throw new AccessAlreadyGrantedException();
        }
        DeviceAccess access = new DeviceAccess(device, targetUser, role, grantedById, Instant.now());
        device.getAccesses().add(access);
        return accessRepository.save(access);
    }

    public void revoke(UUID deviceId, UUID userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        DeviceAccess access = accessRepository.findByDeviceIdAndUserId(deviceId, userId)
                .orElseThrow(DeviceAccessNotFoundException::new);
        device.getAccesses().remove(access);
    }

    @Transactional(readOnly = true)
    public List<DeviceAccess> findByDevice(UUID deviceId) {
        return accessRepository.findByDeviceIdWithUser(deviceId);
    }

    @Transactional(readOnly = true)
    public List<DeviceAccess> findByUser(UUID userId) {
        return accessRepository.findByUserIdWithDevice(userId);
    }
}