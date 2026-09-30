package server.users.internal;
import java.util.Optional;
import java.util.UUID;
import server.users.User;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
