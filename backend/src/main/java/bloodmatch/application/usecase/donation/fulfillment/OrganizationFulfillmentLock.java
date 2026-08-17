package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serializes completed-donation writes per blood center so the donation persist
 * and the fulfillment refresh cannot interleave on the same organization.
 * See docs/FULFILLMENT_CONCURRENCY.md.
 */
@Service
public class OrganizationFulfillmentLock {

  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  public <T> T call(DomainID organizationId, Supplier<T> action) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (action == null)
      throw new IllegalArgumentException("Action cannot be null");

    ReentrantLock lock = locks.computeIfAbsent(
        organizationId.getValue().toString(),
        key -> new ReentrantLock());
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
    }
  }
}
