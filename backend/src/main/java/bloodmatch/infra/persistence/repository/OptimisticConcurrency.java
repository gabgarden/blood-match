package bloodmatch.infra.persistence.repository;

import bloodmatch.application.exception.ConcurrencyException;
import org.springframework.dao.OptimisticLockingFailureException;

final class OptimisticConcurrency {

  private OptimisticConcurrency() {
  }

  static void save(Runnable action, String entityName) {
    try {
      action.run();
    } catch (OptimisticLockingFailureException e) {
      throw new ConcurrencyException(
          entityName + " was changed by another operation. Reload it and try again.", e);
    }
  }
}
