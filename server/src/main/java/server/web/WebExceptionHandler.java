package server.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import server.devices.DeviceNotFoundException;

import java.io.IOException;

@ControllerAdvice(basePackages = "server.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
class WebExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    void handleNotFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(Exception.class)
    void rethrow(Exception exception) throws Exception {
        throw exception;
    }
}
