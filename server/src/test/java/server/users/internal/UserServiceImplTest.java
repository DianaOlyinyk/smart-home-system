package server.users.internal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import server.users.EmailAlreadyRegisteredException;
import server.users.UserAccount;
import server.users.UserNotFoundException;
import server.users.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void test01() {
        when(userRepository.existsByEmail("olena@example.com")).thenReturn(false);
        when(passwordEncoder.encode("qwerty123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserAccount account = userService.register("olena@example.com", "qwerty123", "Олена");
        assertNotNull(account);
        assertEquals("olena@example.com", account.email());
        assertEquals("Олена", account.name());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void test02() {
        when(userRepository.existsByEmail("olena@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyRegisteredException.class,
                () -> userService.register("olena@example.com", "qwerty123", "Олена"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void test03() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "olena@example.com", "HASHED", "Олена", Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserAccount account = userService.findById(id);
        assertEquals(id, account.id());
        assertEquals("olena@example.com", account.email());
    }

    @Test
    void test04() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findById(id));
    }

    @Test
    void test05() {
        User user = new User(UUID.randomUUID(), "olena@example.com", "HASHED", "Олена", Instant.now());
        when(userRepository.findByEmail("olena@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("qwerty123", "HASHED")).thenReturn(true);
        Optional<UserAccount> account = userService.authenticate("olena@example.com", "qwerty123");
        assertTrue(account.isPresent());
        assertEquals("olena@example.com", account.get().email());
    }

    @Test
    void test06() {
        User user = new User(UUID.randomUUID(), "olena@example.com", "HASHED", "Олена", Instant.now());
        when(userRepository.findByEmail("olena@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);
        Optional<UserAccount> account = userService.authenticate("olena@example.com", "wrong");
        assertTrue(account.isEmpty());
    }
}
