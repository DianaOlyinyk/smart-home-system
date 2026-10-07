package server.commands.internal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import server.commands.Command;
import server.commands.CommandRepository;
import server.commands.CommandService;
import server.commands.CommandNotFoundException;
import server.commands.RequiredRole;
import server.devices.DeviceService;

import server.commands.CommandAlreadyExistsException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
        var device = deviceService.findById(deviceId);
        if (commandRepository.existsByDeviceIdAndName(deviceId, name)) {
            throw new CommandAlreadyExistsException(deviceId, name);
        }

        Command command = new Command(
                device,
                name,
                argsSchema,
                requiredRole,
                Instant.now(clock)
        );
        try {
            return commandRepository.save(command);
        } catch (DataIntegrityViolationException e) {
            // another request created the same name between the check and the save
            CommandAlreadyExistsException exception = new CommandAlreadyExistsException(deviceId, name);
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public Command findByDeviceIdAndCommandId(UUID deviceId, UUID commandId) {
        return commandRepository.findByDeviceIdAndCommandId(deviceId, commandId)
                .orElseThrow(() -> new CommandNotFoundException(deviceId, commandId));
    }

    @Override
    public List<Command> findAllByDevice(UUID deviceId, RequiredRole requiredRole) {
        deviceService.findById(deviceId);
        if (requiredRole == null) {
            return commandRepository.findByDeviceIdOrderByNameAsc(deviceId);
        }

        return commandRepository.findByDeviceIdAndRequiredRoleOrderByNameAsc(deviceId, requiredRole);
    }

    @Override
    public void update(UUID deviceId, UUID commandId, String argsSchema, RequiredRole requiredRole) {
        Command command = commandRepository.findByDeviceIdAndCommandId(deviceId, commandId).orElseThrow(() -> new CommandNotFoundException(deviceId, commandId));

        command.setArgsSchema(argsSchema);
        command.setRequiredRole(requiredRole);
        commandRepository.save(command);
    }

    @Override
    public void delete(UUID deviceId, UUID commandId) {
        Command command = commandRepository.findByDeviceIdAndCommandId(deviceId, commandId).orElseThrow(() -> new CommandNotFoundException(deviceId, commandId));
        commandRepository.delete(command);
    }
}
