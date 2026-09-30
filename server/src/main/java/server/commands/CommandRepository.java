package server.commands;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandRepository extends JpaRepository<Command, UUID> {

	Command save(Command command);

	@Query
	Optional<Command> findById(UUID id);

	List<Command> findByDeviceIdOrderByNameAsc(UUID deviceId);

	List<Command> findByDeviceIdAndRequiredRoleOrderByNameAsc(UUID deviceId, RequiredRole required_role);

	Boolean existsByDeviceIdAndName(UUID deviceId, String device_name);
}
