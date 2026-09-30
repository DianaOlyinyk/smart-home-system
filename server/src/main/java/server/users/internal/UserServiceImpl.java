package server.users.internal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.users.EmailAlreadyRegisteredException;
import server.users.User;
import server.users.UserAccount;
import server.users.UserNotFoundException;
import server.users.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserAccount register(String email, String rawPassword, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        // Hibernate сам згенерує UUID завдяки @GeneratedValue
        User user = new User(email, passwordEncoder.encode(rawPassword), name, Instant.now());

        User saved = userRepository.save(user);
        return toAccount(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount findById(UUID id) {
        return toAccount(getUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(this::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findAll(String query) {
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findAll()
                : userRepository.search(query);
        return users.stream().map(this::toAccount).toList();
    }

    @Override
    public UserAccount rename(UUID id, String newName) {
        User user = getUser(id);
        user.setName(newName);
        return toAccount(userRepository.save(user));
    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private UserAccount toAccount(User user) {
        return new UserAccount(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}