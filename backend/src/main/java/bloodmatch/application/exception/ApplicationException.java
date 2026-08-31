package bloodmatch.application.exception;

public abstract class ApplicationException extends RuntimeException {

  protected ApplicationException(String message) {
    super(message);
  }
}
