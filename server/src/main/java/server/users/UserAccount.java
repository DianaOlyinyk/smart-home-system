package server.users;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(UUID id, String email, String name, Instant createdAt) {
}
