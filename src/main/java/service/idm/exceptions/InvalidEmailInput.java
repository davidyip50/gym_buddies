package service.idm.exceptions;

public class InvalidEmailInput extends Exception{
    public InvalidEmailInput() {
    }

    public InvalidEmailInput(String message) {
        super(message);
    }

    public InvalidEmailInput(String message, Throwable cause) {
        super(message, cause);
    }
}
