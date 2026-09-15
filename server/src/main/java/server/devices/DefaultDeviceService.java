package server.devices;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
class DefaultDeviceService implements DeviceService {

    @Override
    public Device create(String name, DeviceType type) {
        return new Device(
                UUID.randomUUID(),
                name,
                type,
                UUID.randomUUID().toString(),
                Instant.now());
    }
}