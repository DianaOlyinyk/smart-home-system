package server.devices.internal;

import server.devices.AccessRole;
import server.devices.DeviceAccess;

import java.util.UUID;

record DeviceAccessResponse(UUID userId, AccessRole role, UUID grantedBy, java.time.Instant grantedAt) {
    static DeviceAccessResponse from(DeviceAccess access) {
        return new DeviceAccessResponse(access.getUserId(), access.getRole(), access.getGrantedById(),
                access.getGrantedAt());
    }
}