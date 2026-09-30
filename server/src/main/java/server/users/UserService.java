package server.users;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface UserService {

    UserAccount register(String email, String rawPassword, String name);

    UserAccount findById(UUID id);

    Optional<UserAccount> authenticate(String name, String rawPassword);

    User getUser(UUID id);
    User getUserByEmail(String email);
    List<UserAccount> findAll(String query);
    UserAccount rename(UUID id, String newName);
    void delete(UUID id);
}
