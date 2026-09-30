package server.devices.internal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceAccessRepository extends ListCrudRepository<DeviceAccess, UUID> {
    boolean existsByDeviceIdAndUserId(UUID deviceId, UUID userId);

    @Query("SELECT da FROM DeviceAccess da JOIN FETCH da.user WHERE da.device.id = :deviceId")
    List<DeviceAccess> findByDeviceIdWithUser(@Param("deviceId") UUID deviceId);

    @Query("SELECT da FROM DeviceAccess da JOIN FETCH da.device WHERE da.user.id = :userId")
    List<DeviceAccess> findByUserIdWithDevice(@Param("userId") UUID userId);
    Optional<DeviceAccess> findByDeviceIdAndUserId(UUID deviceId, UUID userId);
}