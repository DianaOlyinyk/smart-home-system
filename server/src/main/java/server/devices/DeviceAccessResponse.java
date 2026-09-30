package server.devices;

import java.time.Instant;
import java.util.UUID;

public record DeviceAccessResponse(
        UUID id,
        UUID deviceId,
        UUID userId,
        AccessRole role,
        UUID grantedBy,
        Instant grantedAt
) {
    public static DeviceAccessResponse from(DeviceAccess access) {
        return new DeviceAccessResponse(
                access.getId(),
                access.getDevice().getId(),
                access.getUserId(),
                access.getRole(),
                access.getGrantedById(),
                access.getGrantedAt()
        );
    }
}