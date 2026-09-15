package server.users.internal;

import server.users.UserAccount;

import java.time.Instant;
import java.util.UUID;

record UserResponse(UUID id, String email, String name, Instant createdAt) {

    static UserResponse from(UserAccount account) {
        return new UserResponse(account.id(), account.email(), account.name(), account.createdAt());
    }
}
