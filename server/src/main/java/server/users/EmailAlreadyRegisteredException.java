package server.users;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Користувач з поштою " + email + " вже існує");
    }
}
