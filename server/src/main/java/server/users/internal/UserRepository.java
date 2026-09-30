package server.users.internal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import server.users.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends ListCrudRepository<User, UUID>{

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE lower(u.name) LIKE lower(concat('%', :q, '%')) OR lower(u.email) LIKE lower(concat('%', :q, '%'))")
    List<User> search(@Param("q") String q);
}
