package server.users;

import java.util.UUID;

public interface UserService {

    UserAccount register(String email, String rawPassword, String name);

    UserAccount findById(UUID id);
}
