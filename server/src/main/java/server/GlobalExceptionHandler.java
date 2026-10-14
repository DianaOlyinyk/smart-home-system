package server;
import server.devices.AccessAlreadyGrantedException;
import server.devices.CommandExecutionNotFoundException;
import server.devices.DeviceAccessNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import server.auth.InvalidCredentialsException;
import server.commands.CommandNotFoundException;
import server.executions.InvalidCommandArgsException;
import server.executions.InvalidStateTransitionException;
import server.executions.UnsupportedCommandException;
import server.users.EmailAlreadyRegisteredException;
import server.users.UserNotFoundException;
import server.devices.DeviceNotFoundException;
import server.commands.CommandAlreadyExistsException;
import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleNotFound(UserNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Користувача не знайдено", ex.getMessage(), ex);
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    ProblemDetail handleDeviceNotFound(DeviceNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Пристрій не знайдено", ex.getMessage(), ex);
    }

    @ExceptionHandler(AccessAlreadyGrantedException.class)
    ProblemDetail handleAccessAlreadyGranted(AccessAlreadyGrantedException ex) {
        return problemDetail(HttpStatus.CONFLICT, "Доступ уже надано", ex.getMessage(), ex);
    }

    @ExceptionHandler(DeviceAccessNotFoundException.class)
    ProblemDetail handleDeviceAccessNotFound(DeviceAccessNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Доступ до пристрою не знайдено", ex.getMessage(), ex);
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

    @ExceptionHandler(CommandExecutionNotFoundException.class)
    ProblemDetail handleCommandExecutionNotFound(CommandExecutionNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Виконання команди не знайдено", ex.getMessage(), ex);
    }

    @ExceptionHandler(InvalidCommandArgsException.class)
    ProblemDetail handleInvalidCommandArgs(InvalidCommandArgsException ex) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Некоректні аргументи команди", ex.getMessage(), ex);
        if (!ex.getErrors().isEmpty()) {
            problemDetail.setProperty("errors", ex.getErrors());
        }
        return problemDetail;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ProblemDetail handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return problemDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Некоректний перехід стану виконання", ex.getMessage(), ex);
    }

    @ExceptionHandler(UnsupportedCommandException.class)
    ProblemDetail handleUnsupportedCommand(UnsupportedCommandException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, "Команда не підтримується", ex.getMessage(), ex);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Помилка валідації вхідних даних",
                "Один або декілька параметрів не пройшли валідацію", ex);

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first));
        problemDetail.setProperty("errors", errors);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Некоректне тіло запиту",
                "Тіло запиту не вдалося розібрати як JSON", ex);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Некоректний параметр запиту",
                "Параметр '" + parameter + "' має неправильний формат", ex);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.NOT_FOUND, "Ресурс не знайдено",
                "Маршрут '" + ex.getResourcePath() + "' не існує", ex);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.METHOD_NOT_ALLOWED, "Метод не підтримується",
                ex.getMessage(), ex);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера",
                "Сталася непередбачена помилка. Спробуйте пізніше", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problemDetail(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(), ex);
    }

    @ExceptionHandler(CommandAlreadyExistsException.class)
    ProblemDetail handleCommandConflict(CommandAlreadyExistsException ex) {
        return problemDetail(HttpStatus.CONFLICT, "Команда з такою назвою вже існує", ex.getMessage(), ex);
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
