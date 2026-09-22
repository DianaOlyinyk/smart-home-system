package server.devices.internal;

import org.springframework.stereotype.Service;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceService;
import server.devices.DeviceType;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final Clock clock;

    DeviceServiceImpl(DeviceRepository deviceRepository, Clock clock) {
        this.deviceRepository = deviceRepository;
        this.clock = clock;
    }

    @Override
    public Device create(String name, DeviceType type) {
        Device device = new Device(UUID.randomUUID(), name, type, UUID.randomUUID().toString(), Instant.now(clock));
        return deviceRepository.save(device);
    }

    @Override
    public Device findById(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }
}