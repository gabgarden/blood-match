package bloodmatch.shared.application.exception;

public class ConcurrencyException extends ApplicationException {

  public ConcurrencyException(String message) {
    super(message);
  }

  public ConcurrencyException(String message, Throwable cause) {
    super(message, cause);
  }
}
