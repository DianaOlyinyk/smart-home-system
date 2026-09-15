package server.commands;

import java.util.UUID;

public interface CommandService {

    Command findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);
}
