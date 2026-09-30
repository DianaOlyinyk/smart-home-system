package server.devices.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.User;
import server.users.UserService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserService userService;
    private final Clock clock;

    DeviceServiceImpl(DeviceRepository deviceRepository, UserService userService, Clock clock) {
        this.deviceRepository = deviceRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Device create(String name, DeviceType type, UUID ownerId) {
        Instant now = Instant.now(clock);
        Device device = new Device(name, type, UUID.randomUUID().toString(), now);
        if (ownerId != null) {
            User owner = userService.getUser(ownerId);
            device.grantAccess(owner, AccessRole.OWNER, owner, now);
        }
        return deviceRepository.save(device);
    }

    @Override
    public Device findById(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> findAll(DeviceType type) {
        return type == null
                ? deviceRepository.findAllWithAccesses()
                : deviceRepository.findByTypeOrderByNameAsc(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Device getWithAccesses(UUID id) {
        return deviceRepository.findByIdWithAccesses(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Override
    @Transactional
    public Device rename(UUID id, String name) {
        Device device = getWithAccesses(id);
        device.rename(name);
        return device;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        deviceRepository.delete(findById(id));
    }
}
