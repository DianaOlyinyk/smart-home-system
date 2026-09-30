package server.commands;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandRepository extends JpaRepository<Command, UUID> {

	@Query("select c from Command c where c.deviceId = :deviceId and c.id = :commandId")
	Optional<Command> findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);

	List<Command> findByDeviceIdOrderByNameAsc(UUID deviceId);

	List<Command> findByDeviceIdAndRequiredRoleOrderByNameAsc(UUID deviceId, RequiredRole required_role);

	Boolean existsByDeviceIdAndName(UUID deviceId, String device_name);
}
