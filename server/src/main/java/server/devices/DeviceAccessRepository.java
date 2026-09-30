package server.devices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceAccessRepository extends JpaRepository<DeviceAccess, UUID> {

    @Query("SELECT da FROM DeviceAccess da JOIN FETCH da.device WHERE da.user.id = :userId")
    List<DeviceAccess> findAllByUserIdWithDevice(@Param("userId") UUID userId);

    boolean existsByDeviceIdAndUser_Id(UUID deviceId, UUID userId);
}