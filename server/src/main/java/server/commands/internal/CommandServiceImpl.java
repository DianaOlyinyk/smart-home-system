package server.commands.internal;

import org.springframework.stereotype.Service;
import server.commands.Command;
import server.commands.CommandRepository;
import server.commands.CommandService;
import server.commands.CommandNotFoundException;
import server.commands.RequiredRole;
import server.devices.DeviceService;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;
    private final DeviceService deviceService;
    private final Clock clock;

    CommandServiceImpl(CommandRepository commandRepository, DeviceService deviceService, Clock clock) {
        this.commandRepository = commandRepository;
        this.deviceService = deviceService;
        this.clock = clock;
    }

    @Override
    public Command create(UUID deviceId, String name, String argsSchema, RequiredRole requiredRole) {
        deviceService.findById(deviceId);
        Command command = new Command(
                UUID.randomUUID(),
                deviceId,
                name,
                argsSchema,
                requiredRole,
                Instant.now(clock)
        );
        return commandRepository.save(command);
    }

    @Override
    public Command findByDeviceIdAndCommandId(UUID deviceId, UUID commandId) {
        return commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)
                .orElseThrow(() -> new CommandNotFoundException(deviceId, commandId));
    }
}