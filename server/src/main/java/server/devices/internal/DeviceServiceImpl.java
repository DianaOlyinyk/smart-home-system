package server.devices.internal;

import org.springframework.stereotype.Service;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.devices.DeviceDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import server.users.User;
import server.users.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final Clock clock;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    DeviceServiceImpl(DeviceRepository deviceRepository, Clock clock) {
        this(deviceRepository, clock, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    DeviceServiceImpl(DeviceRepository deviceRepository, Clock clock, UserRepository userRepository,
                      ApplicationEventPublisher eventPublisher) {
        this.deviceRepository = deviceRepository;
        this.clock = clock;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Device create(String name, DeviceType type) {
        Device device = new Device(UUID.randomUUID(), name, type, UUID.randomUUID().toString(), Instant.now(clock));
        return deviceRepository.save(device);
    }

    @Override
    public Device create(String name, DeviceType type, UUID ownerId) {
        Device device = create(name, type);
        if (ownerId != null) {
            User owner = userRepository == null
                    ? new User(ownerId, "owner-" + ownerId + "@example.test", "", "Owner", Instant.now(clock))
                    : userRepository.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerId));
            device.grantAccess(owner, server.devices.AccessRole.OWNER, owner, Instant.now(clock));
            device = deviceRepository.save(device);
        }
        return device;
    }

    @Override
    public Device findById(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Override
    public List<Device> findAll(DeviceType type) {
        return deviceRepository.findAll(type);
    }

    @Override
    public Device getWithAccesses(UUID id) {
        return deviceRepository.getWithAccesses(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Override
    public Device rename(UUID id, String name) {
        Device device = findById(id);
        device.rename(name);
        return deviceRepository.save(device);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        deviceRepository.deleteById(id);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new DeviceDeletedEvent(id));
        }
    }
}