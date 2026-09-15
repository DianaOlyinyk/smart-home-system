package server.devices;

import java.time.Instant;
import java.util.UUID;

public record Device(
        UUID id,
        String name,
        DeviceType type,
        String connectionToken,
        Instant createdAt) {
}