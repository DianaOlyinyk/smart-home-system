package server.auth.internal;

import server.auth.IssuedToken;

import java.util.UUID;

record TokenResponse(String token, UUID userId) {

    static TokenResponse from(IssuedToken issuedToken) {
        return new TokenResponse(issuedToken.token(), issuedToken.userId());
    }
}
