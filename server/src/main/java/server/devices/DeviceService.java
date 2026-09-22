package server.devices;
import java.util.UUID;

public interface DeviceService {

    Device create(String name, DeviceType type);
    Device findById(UUID id);
}
