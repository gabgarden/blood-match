package bloodmatch.shared.application.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionsTest {

  static class TestApplicationException extends ApplicationException {
    TestApplicationException(String message) {
      super(message);
    }

    TestApplicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  @Test
  void shouldInstantiateApplicationException() {
    TestApplicationException ex1 = new TestApplicationException("test error");
    assertEquals("test error", ex1.getMessage());

    Throwable cause = new RuntimeException("root cause");
    TestApplicationException ex2 = new TestApplicationException("test error with cause", cause);
    assertEquals("test error with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }

  @Test
  void shouldInstantiateConcurrencyException() {
    ConcurrencyException ex1 = new ConcurrencyException("concurrency error");
    assertEquals("concurrency error", ex1.getMessage());

    Throwable cause = new RuntimeException("root cause");
    ConcurrencyException ex2 = new ConcurrencyException("concurrency error with cause", cause);
    assertEquals("concurrency error with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }

  @Test
  void shouldInstantiateOtherExceptions() {
    ConflictException conflict = new ConflictException("conflict");
    assertEquals("conflict", conflict.getMessage());

    ForbiddenException forbidden = new ForbiddenException("forbidden");
    assertEquals("forbidden", forbidden.getMessage());

    NotFoundException notFound = new NotFoundException("not found");
    assertEquals("not found", notFound.getMessage());

    UnauthorizedException unauthorized = new UnauthorizedException("unauthorized");
    assertEquals("unauthorized", unauthorized.getMessage());

    ValidationException validation = new ValidationException("validation");
    assertEquals("validation", validation.getMessage());
  }
}
