package server.devices.internal;

import org.springframework.stereotype.Repository;
import server.devices.Device;
import server.devices.DeviceRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    private final Map<UUID, Device> storage = new ConcurrentHashMap<>();

    @Override
    public Device save(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }
        Device deviceToSave = device;

        if (device.id() == null) {
            deviceToSave = new Device(
                    UUID.randomUUID(),
                    device.name(),
                    device.type(),
                    device.connectionToken(),
                    device.createdAt()
            );
        }

        storage.put(deviceToSave.id(), deviceToSave);
        return deviceToSave;
    }

    @Override
    public Optional<Device> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }
}