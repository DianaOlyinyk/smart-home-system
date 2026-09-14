package server.users.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class User {
    @Id
    UUID id;

    String email;
    String passwordHash;
    String name;
    Instant createdAt;
}
