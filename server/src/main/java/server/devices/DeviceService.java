package server.devices;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    Device create(String name, DeviceType type, UUID ownerId);
    Device findById(UUID id);
    List<Device> findAll(DeviceType type);
    Device getWithAccesses(UUID id);
    Device rename(UUID id, String name);
    void delete(UUID id);
}
