package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import server.auth.InvalidCredentialsException;
import server.commands.CommandNotFoundException;
import server.executions.InvalidCommandArgsException;
import server.users.EmailAlreadyRegisteredException;
import server.users.UserNotFoundException;

import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleNotFound(UserNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Користувача не знайдено", ex.getMessage(), ex);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleConflict(EmailAlreadyRegisteredException ex) {
        return problemDetail(HttpStatus.CONFLICT, "Користувач з такою поштою вже існує", ex.getMessage(), ex);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleUnauthorized(InvalidCredentialsException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "Невірні облікові дані", ex.getMessage(), ex);
    }

    @ExceptionHandler(CommandNotFoundException.class)
    ProblemDetail handleCommandNotFound(CommandNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Команду не знайдено", ex.getMessage(), ex);
    }

    @ExceptionHandler(InvalidCommandArgsException.class)
    ProblemDetail handleInvalidCommandArgs(InvalidCommandArgsException ex) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Некоректні аргументи команди", ex.getMessage(), ex);
        if (!ex.getErrors().isEmpty()) {
            problemDetail.setProperty("errors", ex.getErrors());
        }
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Помилка валідації вхідних даних",
                "Один або декілька параметрів не пройшли валідацію", ex);

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first));
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, "Некоректне тіло запиту",
                "Тіло запиту не вдалося розібрати як JSON", ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, "Некоректний параметр запиту",
                "Параметр '" + ex.getName() + "' має неправильний формат", ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Ресурс не знайдено",
                "Маршрут '" + ex.getResourcePath() + "' не існує", ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return problemDetail(HttpStatus.METHOD_NOT_ALLOWED, "Метод не підтримується", ex.getMessage(), ex);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера",
                "Сталася непередбачена помилка. Спробуйте пізніше", ex);
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail, Exception ex) {
        if (status.is5xxServerError()) {
            log.error(title, ex);
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
