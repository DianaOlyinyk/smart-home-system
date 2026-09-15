package server.auth;

import java.util.UUID;

public record IssuedToken(String token, UUID userId) {
}
