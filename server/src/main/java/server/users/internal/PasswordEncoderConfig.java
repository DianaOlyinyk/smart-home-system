package server.users.internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration()
public class PasswordEncoderConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordEncoder.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
            16, // Довжина солі в байтах
            32, // Довжина вихідного хешу в байтах
            1, // Кількість паралельних потоків обчислення
            16384, // Обсяг оперативної пам'яті в кілобайтах (16 МБ)
            3 // Кількість ітерацій обчислення
        );
    }

    @Bean
    public ApplicationRunner logSampleHash(PasswordEncoder passwordEncoder) {
        return args -> LOGGER.info("Приклад хешу: {}", passwordEncoder.encode("demo-password"));
    }
}
