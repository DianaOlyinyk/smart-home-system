package server.auth;

public interface AuthService {

    IssuedToken login(String email, String rawPassword);
}
