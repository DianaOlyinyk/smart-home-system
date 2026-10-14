package server.auth.internal;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtValidatorService {
    String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
}
