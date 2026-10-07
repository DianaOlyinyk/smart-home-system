package server.devices;

import java.util.UUID;

public record DeviceDeletedEvent(UUID deviceId) {
}