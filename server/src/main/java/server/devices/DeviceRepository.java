package server.devices;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(UUID id);
}