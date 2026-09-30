package server.users.internal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import server.users.EmailAlreadyRegisteredException;
import server.users.UserAccount;
import server.users.UserNotFoundException;
import server.users.UserService;
import server.users.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
@Service
class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserAccount register(String email, String rawPassword, String name){
        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode(rawPassword), name, Instant.now());

        User saved = userRepository.save(user);
        return toAccount(saved);
    }

    @Override
    public UserAccount findById(UUID id){
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
                return toAccount(user);
    }

    @Override
    public Optional<UserAccount> authenticate(String email, String rawPassword){
        return userRepository.findByEmail(email).filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(this::toAccount);
    }

    private UserAccount toAccount(User user){
        return new UserAccount(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
