package server.users;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("Користувача з id " + userId + " не знайдено");
    }
}
