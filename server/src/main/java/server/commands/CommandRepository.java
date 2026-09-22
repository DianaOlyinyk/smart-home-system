package server.commands;

import java.util.Optional;
import java.util.UUID;

public interface CommandRepository {

	Command save(Command command);

	Optional<Command> findById(UUID id);

	Optional<Command> findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);
}
