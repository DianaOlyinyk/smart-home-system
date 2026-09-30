package server.devices;
import org.springframework.data.repository.ListCrudRepository;
import java.util.UUID;

public interface DeviceRepository extends ListCrudRepository<Device, UUID> {
}