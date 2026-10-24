# Звіт за 2 групове завдання
- Малій Олександра:
    - Реалізовано контролер сутності User, що дозволяє реєструвати користувача та отримувати профіль за id, та авторизація у TokenController
    - Реалізовано ендпоінт виконання команди на пристрої (`POST /devices/{deviceId}/commands/{commandId}/executions`)
    - Додано валідацію DTO-сутностей, зокрема кастомна валідація аргументів команди проти JSON Schema (бібліотека json-schema-validator)
    - Розширено глобальний обробник помилок (`GlobalExceptionHandler`) — catch-all для 400/404/405/500 та єдиний формат `ProblemDetail`
    - Написано тести взаємодії (MockMvc) для auth, users, executions
- Олійник Діана:
    - Допомога з реалізацією `devices`, DTO-контрактів, написання тестів
- Забіяка Денис:
    - Реалізував REST-ендпоінт `POST /devices` для додавання нового розумного пристрою в систему
    - Створив DTO-контракти (`CreateDeviceRequest`, `DeviceResponse`) на базі Java Records
    - Налаштував Jakarta Validation (`@NotBlank`, `@NotNull`) — перевірка непорожньої назви та відповідності типу enum `DeviceType` (LAMP, KETTLE, AC, COFFEE_MACHINE, BLINDS)
    - Написав тести взаємодії (MockMvc): успішне створення (201 Created + заголовок `Location`) та чотири сценарії валідації (400 Bad Request)
- Мошенський Олег:
    - Реалізував REST-ендпоінт `POST /devices/{deviceId}/commands` для додавання нових команд до пристрою
    - Створив DTO-контракти (`CreateCommandRequest`, `CommandResponse`) на базі Java Records
    - Налаштував Jakarta Validation (`@NotBlank`, `@NotNull`) для перевірки вхідних даних на рівні контролера
    - Написав тести для веб-шару через MockMvc, повністю покривши успішний сценарій (201 Created) та помилки валідації/відсутності ресурсу (400, 404)

# Звіт за 3 групове завдання
- Малій Олександра:
    - Імплементовано сервісний шар для `CommandExecution`: формальна модель переходів станів (`ExecutionStatus`, `transitionTo`, `InvalidStateTransitionException`) та `CommandExecutionServiceImpl`, що оркеструє виконання команди від пошуку до публікації події `CommandExecutedEvent`
    - Визначено контракти для Strategy-патерна (`CommandExecutionStrategy`, `ExecutionOutcome`) та `CommandExecutionRepository`, без прив'язки до конкретних реалізацій
    - Доопрацьовано обробку помилок (`GlobalExceptionHandler`) та документацію (`README.md`)
    - Проведено рев'ю коду команди, знайдено й виправлено кілька структурних неузгодженостей
    - Написано Mockito-тести без підняття Spring-контексту для всього нового функціоналу CommandExecution
- Олійник Діана:
    - Додано інтерфейс `DeviceRepository` з методами `save` та `findById`.
    - Додано клас `DeviceRepositoryImpl`, що використовує `ConcurrentHashMap` для зберігання даних у пам'яті.
    - Реалізовано `DeviceService.create(name, type)` — створює пристрій із новим `id`, `connectionToken` і `createdAt`, зберігає його через репозиторій.
    - Реалізовано `DeviceService.findById(id)` — повертає пристрій або кидає `DeviceNotFoundException`, якщо його не знайдено.
    - Додано модульні тести `DeviceServiceImplTest` (з `DeviceRepository`, замоканим через Mockito):
      - успішне створення пристрою;
      - успішний пошук існуючого пристрою;
      - `DeviceNotFoundException`, якщо пристрою немає.
    - Реалізовано `CommandService.create(deviceId, name, argsSchema, requiredRole)` та `findByDeviceIdAndCommandId(deviceId, commandId)`
    - Додано `CommandRepository` (інтерфейс: `save`, `findById`, `findByDeviceIdAndCommandId`) і `CommandRepositoryImpl` із використанням `ConcurrentHashMap`.
    - Модульні тести `CommandServiceImplTest` (із моками `DeviceService` і `CommandRepository` )
- Забіяка Денис:
	- Реалізовано стратегії виконання команд: окремий клас на кожну команду — `turn_on`, `turn_off`, `set_brightness`, `set_temperature`, `set_mode`, `open`, `close`, `set_position`, зі спільною частиною та валідацією аргументів в `AbstractDeviceCommandStrategy`
	- Виділено транспорт до пристрою в інтерфейс `DeviceClient` і додано мінімальну синхронну in-memory реалізацію `SimulatedDeviceClient`, що тримає стан кожного пристрою окремо
	- Додано `CommandExecutionStrategyResolver` — вибір стратегії з колекції `List<CommandExecutionStrategy>` без `@Qualifier`; використовується `CommandExecutionServiceImpl`
	- Перевірку діапазону `set_temperature` винесено з коду в `argsSchema` команди, що усунуло зрізання дробових значень
	- Написано Mockito-тести на кожну стратегію окремо з мокнутим `DeviceClient`
- Мошенський Олег:
    - Додав окремий пакет server.audit з класом CommandExecutionAuditListener — @component, публічний клас і метод, що слухає CommandExecutedEvent (публічний тип із server.executions) через анотацію @ApplicationModuleListener і пише запис у лог застосунку через SLF4J: id виконання команди та фінальний статус. Модуль audit лише імпортує подію й нічого не знає про внутрішню логіку виконання команди — зв'язок відбувається виключно через подію, без прямої залежності між модулями.
    - Реалізував UserService.register(email, rawPassword, name) та findById(id), а також AuthService.login(email, rawPassword). Додав UserRepository (інтерфейс: save, findById, findByEmail, existsByEmail) і UserRepositoryImpl — мінімальна реалізація на ConcurrentHashMap, без реальної БД. Конструкторна ін'єкція UserRepository в сервіс.
 
# Звіт за 4 групове завдання
- Олег Мошенський:
	- Реалізував CRUD-операції для користувачів та пошук за email/запитом.
	- Додав `DeviceAccessService` і `DeviceAccessController` для надання, відкликання та перегляду доступів до пристроїв.
	- Налаштував H2 та JPA для роботи з базою даних і тестування.
	- Переніс `User` у серверний модуль та створив відповідний JPA Repository.
 	- Додав необхідні винятки та обробку помилок `404` і `409`.
	- Додав та оновив тести для перевірки роботи репозиторіїв, доступів, cascade та `orphanRemoval`.
	-	Вирішив конфлікти під час об'єднання змін із `main` та перевірив збірку проєкту.

- Олійник Діана
  	- Реалізовано DeviceService з методами:
	- Контролер DeviceController з REST‑ендпоінтами для CRUD операцій.
 	- DTO DeviceResponse
	- Додано сутність Device з колекцією accesses (@OneToMany(mappedBy = "device", cascade = ALL, orphanRemoval = true)).
	- DeviceAccess як сторону‑власника
	- DeviceRepository:
 	- DeviceAccessRepository:
	- Написано тест DeviceRepositoryTest 

- Олександра Малій:
    - Перевела `CommandExecution` на JPA-сутність: зв'язки `@ManyToOne(LAZY)` з `Command`, `Device` і `User`, каскадне видалення через `@OnDelete`, аргументи виконання в JSON-колонці, поле `completedAt`
    - Реалізувала `CommandExecutionRepository` на Spring Data: журнал пристрою через `@Query` з `JOIN FETCH` (один SQL-запит замість N+1), журнал гостя та пошук виконання в межах пристрою через `@EntityGraph`
    - Реалізувала CRUD виконань команд: запуск із виконавцем через `X-User-Id`, журнал пристрою з фільтром за користувачем, перегляд, зміна статусу через `PATCH` і видалення; транзакційний сервіс, завдяки якому спрацьовує аудит-лог
    - Написала `@DataJpaTest` для репозиторію (JSON, кількість SQL-запитів, каскади), Mockito- і MockMvc-тести для сервісу та контролера виконань

- Забіяка Денис:
	- Переведено `Command` на JPA-сутність: id генерує Hibernate, унікальна пара `(device_id, name)`, роль зберігається як рядок (`@Enumerated(EnumType.STRING)`)
	- `CommandRepository` переведено на Spring Data: derived-запити `findByDeviceIdOrderByNameAsc`, `findByDeviceIdAndRequiredRoleOrderByNameAsc`, `existsByDeviceIdAndName` та `@Query` `findByDeviceIdAndCommandId` (команда шукається лише в межах свого пристрою); `CommandRepositoryImpl` видалено
	- Реалізовано CRUD команд: `GET /devices/{id}/commands?requiredRole=`, `PUT` і `DELETE /devices/{id}/commands/{commandId}`; 404 для неіснуючого пристрою чи чужої команди, 409 на дублікат назви (включно з одночасними запитами)
	- Оновлено README: таблиця ендпоінтів команд
	- Написано `@DataJpaTest` для репозиторію (сортування, фільтр за роллю, унікальність), Mockito- і MockMvc-тести для сервісу та контролера

# Звіт за 5 групове завдання
- Малій Олександра:
    - Підключила SpringDoc (OpenAPI) до серверного модуля та додала `OpenApiConfig` з описом API (назва, версія, опис, локальний сервер)
    - Задокументувала всі контролери в Swagger UI: групи через `@Tag`, опис кожного ендпоінта через `@Operation`, реальні коди відповідей через `@ApiResponses`, опис параметрів (path-змінні, query-параметри, заголовок `X-User-Id`) і тіл запитів
    - Додала `@Schema` з описом і прикладами (`example`) до всіх DTO-контрактів та перелічила допустимі значення enum-ів
    - Описала помилки єдиною схемою `ProblemDetail` (RFC 9457)
    - Змінила лише анотації й конфігурацію: шляхи, методи, коди та поля API лишились без змін, наявні тести не чіпала
- Олійник Діана:
    - Створила `logback-spring.xml` з `ConsoleAppender` та `RollingFileAppender` (`SizeAndTimeBasedRollingPolicy`): файл `logs/application.log`, архівація старих файлів у `.log.gz`, ліміти за розміром, кількістю днів і загальним обсягом; каталог `logs/` додано до `.gitignore`
    - Реалізувала маскування конфіденційних даних у пакеті `server.logging`: `MaskingMessageConverter` замінює значення полів `password`, `passwordHash`, `token`, `connectionToken` на `***` (формати `ключ=значення`, `ключ: значення` та JSON), підключений у патерні як `%maskedMsg`; звичайний текст, UUID і числа не змінюються
    - Написала тести `MaskingMessageConverterTest` для перевірки маскування
    - Провела аудит блоків `catch` у сервісах (`CommandServiceImpl`, `CommandArgsValidator`) на відсутність проковтнутих винятків; результат наведено в описі PR
- Забіяка Денис:
    - Підготував колекцію Postman `postman/collection.json` (формат v2.1) та оточення `postman/env.json` зі змінними `baseUrl`, `email`, `userId`, `deviceId`, `commandId`, `executionId`
    - Реалізував наскрізний сценарій із 5 кроків: реєстрація користувача, створення пристрою, створення команди, виконання команди, перегляд журналу виконань; ідентифікатори зберігаються між запитами через `pm.environment.set`
    - Додав перевірки статусів і даних відповіді в кожному запиті та генерацію унікального `email` у pre-request скрипті, щоб повторні запуски не давали `409`
    - Налаштував запуск через Newman (`npx newman run collection.json -e env.json`) та додав у `.github/workflows/build.yml` job `api-tests` (після `build`), який збирає й запускає сервіс, чекає на його готовність і виконує колекцію; помилка будь-якої перевірки робить job червоним
- Мошенський Олег:
    - Створив спільний стартер у двох Gradle-модулях: `smarthome-spring-boot-autoconfigure` (код, властивості, `AutoConfiguration.imports`) та порожній `smarthome-spring-boot-starter` (лише залежності); підключив модулі в `settings.gradle` та стартер у `server/build.gradle`
    - Реалізував інтерфейс `NotificationService`, реалізацію за замовчуванням `LoggingNotificationService` (запис у лог через SLF4J) та `NotificationAutoConfiguration`: бін вмикається прапорцем `smarthome.notifications.enabled` (`@ConditionalOnProperty`, `matchIfMissing = true`) і замінюється власним бінем (`@ConditionalOnMissingBean`)
    - Додав типізовані налаштування `NotificationProperties` (Java Record, `@ConfigurationProperties`, `@Validated`, `@NotBlank` для `sender`)
    - Налаштував ієрархію налаштувань і профілі середовищ: базове значення в `application.properties`, перекриття в `application-dev.properties` та `application-prod.properties`
    - Написав тести автоконфігурації через `ApplicationContextRunner`: бін є при `enabled=true`, відсутній при `enabled=false`, є за відсутності властивості, замінюється власним бінем, значення `sender` за замовчуванням

# Звіт за 6 групове завдання
- Малій Олександра:
    - Підготувала розбиття завдання на issues із контрактами між ними та провела рев'ю PR команди: виправила українськими текстами повідомлення про помилки безпеки, усунула конфлікт `JwtValidatorService` у PR фільтра, переспрямувала PR на `main`
    - Реалізувала JWT-сервіси на бібліотеці JJWT: `JwtService` (токен із `subject`, `roles`, `iat`, `exp`, підпис HMAC) і `JwtValidatorService` (перевірка підпису, терміну дії та користувача); секрет береться з `security.jwt.secret` зі змінної оточення `JWT_SECRET`, термін дії 15 хвилин
    - Написала `JwtServiceTest`: валідний токен, підмінений payload, токен із чужим ключем, токен іншого користувача, прострочений токен, занадто короткий секрет
    - Зібрала `SecurityFilterChain`: вимкнений CSRF, CORS, `SessionCreationPolicy.STATELESS`, обробники 401 і 403, відкриті шляхи (`POST /login`, `POST /users`, Swagger, `/v3/api-docs`, H2-консоль), решта запитів лише для автентифікованих; фільтри `JwtAuthenticationFilter` і `JwtLoginFilter` створюються через `new` у ланцюжку; додала бін `AuthenticationManager` та `@EnableMethodSecurity`
    - Виправила `GlobalExceptionHandlerTest` (`addFilters = false`), який зламала безпека
    - Пройшла сценарій перевірки: запит без токена дає 401, вхід повертає токен, запит із `Bearer` дає 200, гість без прав на пристрій отримує 403
- Олійник Діана:
    - Реалізувала `ProblemDetailAuthenticationEntryPoint` (401) та `ProblemDetailAccessDeniedHandler` (403): відповідь `application/problem+json` у UTF-8 з полями `status`, `title`, `detail`, `type`, `timestamp`, `path` на верхньому рівні
    - Додала обробник `AccessDeniedException` у `GlobalExceptionHandler`, щоб відмова від `@PreAuthorize` повертала 403, а не 500
    - Реалізувала `CorsConfig`: дозволений origin `http://localhost:3000`, методи `GET, POST, PUT, DELETE, PATCH, OPTIONS`, заголовки `Authorization` і `Content-Type`, без облікових даних, кеш preflight 1 година
    - Реалізувала `JwtAuthenticationFilter` (`OncePerRequestFilter`): читає токен із заголовка `Authorization: Bearer`, перевіряє його, кладе користувача в `SecurityContextHolder`, а для недійсного токена очищає контекст і повертає 401 через `AuthenticationEntryPoint`
    - Написала `ProblemDetailSecurityHandlersTest` та `JwtAuthenticationFilterTest` (запит без токена, інша схема авторизації, валідний токен, недійсний токен, налаштування CORS)
- Забіяка Денис:
    - Перейшов на Argon2id (`Argon2PasswordEncoder`, залежність BouncyCastle) і додав запис прикладу хешу в лог при старті
    - Зробив `User` реалізацією `UserDetails` (логін це email, глобальних ролей немає) та створив `JpaUserDetailsService`, що шукає користувача через JPA-репозиторій і кидає `UsernameNotFoundException`
    - Реалізував `JwtLoginFilter` на `POST /login`: читає облікові дані з тіла, перевіряє їх через `AuthenticationManager`, повертає `AuthResponse` (`accessToken`, `tokenType`, `expiresIn`), а невдалий вхід віддає як 401 через `AuthenticationEntryPoint`
    - Перейменував ресурс входу з `/tokens` на `/login` у `TokenController` та його тесті
    - Написав `JwtLoginFilterTest`: інші шляхи пропускаються, успішний вхід, неправильний пароль, пошкоджене тіло запиту
- Мошенський Олег:
    - Реалізував `SeedDataLoader` для тестових даних при старті: користувачі owner, guest і stranger, пристрій «Seed Lamp»; гостя додано до пристрою через `DeviceRepository`; повторне засівання пропускається, у лог виводиться id пристрою
    - Реалізував авторизацію по пристрою: `DeviceAccessChecker` (бін `deviceAccessChecker`) з методом `isOwner`, який через `DeviceAccessRepository.existsByDeviceIdAndUser_IdAndRole` перевіряє роль `OWNER` на конкретному пристрої
    - Додав `@PreAuthorize` на створення, зміну й видалення команд та на надання й відкликання доступів: власник дозволений, гість і користувач без доступу отримують 403

# Звіт за 7 групове завдання
Докладний звіт зі знімками сторінок для різних ролей: `Report_Lab7.md`.
- Малій Олександра:
    - Розбила завдання на issues, провела рев'ю та злиття PR команди, розв'язала конфлікт у PR сторінки пристрою
    - Перенесла REST під `/api`, розділила безпеку на три `SecurityFilterChain` (JWT, інструменти, сторінки з формою входу й CSRF), створила модуль `server.web` і `WebExceptionHandler`
    - Зробила лейаути `layout` і `plain`, глобальний `app.css` та версіонування статики за хешем
- Олійник Діана:
    - Реалізувала сторінку входу та сторінки помилок 403, 404 і 5xx
    - Реалізувала сторінку пристрою зі списком команд за роллю та формою «Додати команду» лише для OWNER (валідація, PRG, `@PreAuthorize`), написала `WebPagesTest` і `DevicePageControllerTest`
- Забіяка Денис:
    - Додав у `DeviceService` методи `findAllForUser` і `findRole`
    - Реалізував сторінку «Мої пристрої» та форму додавання пристрою (валідація, PRG) з тестами контролерів
- Мошенський Олег:
    - Написав `WebSecurityTest`: 13 тестів безпеки сторінок на справжньому ланцюгу
    - Підготував знімки сторінок для різних ролей, звіт `Report_Lab7.md` і перелік сторінок у README
