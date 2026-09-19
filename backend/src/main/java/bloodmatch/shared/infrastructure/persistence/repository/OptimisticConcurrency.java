package bloodmatch.shared.infrastructure.persistence.repository;

import bloodmatch.shared.application.exception.ConcurrencyException;
import org.springframework.dao.OptimisticLockingFailureException;

public final class OptimisticConcurrency {

  private OptimisticConcurrency() {
  }

  public static void save(Runnable action, String entityName) {
    try {
      action.run();
    } catch (OptimisticLockingFailureException e) {
      throw new ConcurrencyException(
          entityName + " was changed by another operation. Reload it and try again.", e);
    }
  }
}
