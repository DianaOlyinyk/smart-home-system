package server.devices.internal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import server.devices.AccessRole;
import server.devices.DeviceAccessRepository;
import server.users.User;
import java.util.UUID;

@Component("deviceAccessChecker")
public class DeviceAccessChecker {
    private final DeviceAccessRepository accessRepository;
    DeviceAccessChecker(DeviceAccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }
    public boolean isOwner(UUID deviceId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return accessRepository.existsByDeviceIdAndUser_IdAndRole(deviceId, user.getId(), AccessRole.OWNER);
    }
}