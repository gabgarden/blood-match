package bloodmatch.interfaces.rest.shared;

import bloodmatch.application.exception.ValidationException;

public final class RequestValidationSupport {

  private RequestValidationSupport() {
  }

  public static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public static void requireNonNull(Object value, String message) {
    if (value == null) {
      throw new ValidationException(message);
    }
  }

  public static void requireNotBlank(String value, String message) {
    if (isBlank(value)) {
      throw new ValidationException(message);
    }
  }
}
