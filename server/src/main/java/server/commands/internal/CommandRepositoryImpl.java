package server.commands.internal;

import org.springframework.stereotype.Repository;
import server.commands.Command;
import server.commands.CommandRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CommandRepositoryImpl implements CommandRepository {

	private final Map<UUID, Command> storage = new ConcurrentHashMap<>();

	@Override
	public Command save(Command command) {
		if (command == null) {
			throw new IllegalArgumentException("Command cannot be null");
		}

		Command commandToSave = command;
		if (command.id() == null) {
			commandToSave = new Command(
					UUID.randomUUID(),
					command.deviceId(),
					command.name(),
					command.argsSchema(),
					command.requiredRole(),
					command.createdAt()
			);
		}

		storage.put(commandToSave.id(), commandToSave);
		return commandToSave;
	}

	@Override
	public Optional<Command> findById(UUID id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(storage.get(id));
	}

	@Override
	public Optional<Command> findByDeviceIdAndCommandId(UUID deviceId, UUID commandId) {
		if (deviceId == null || commandId == null) {
			return Optional.empty();
		}

		return findById(commandId)
				.filter(command -> deviceId.equals(command.deviceId()));
	}
}
