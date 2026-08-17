package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationFulfillmentLockTest {

  private final OrganizationFulfillmentLock lock = new OrganizationFulfillmentLock();

  @Test
  void serializesCallsForTheSameOrganization() throws Exception {
    DomainID organizationId = DomainID.generate();
    AtomicInteger inside = new AtomicInteger();
    AtomicInteger maxInside = new AtomicInteger();
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch finished = new CountDownLatch(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    Runnable task = () -> {
      started.countDown();
      lock.call(organizationId, () -> {
        int current = inside.incrementAndGet();
        maxInside.accumulateAndGet(current, Math::max);
        try {
          Thread.sleep(50);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
        }
        inside.decrementAndGet();
        return null;
      });
      finished.countDown();
    };

    pool.submit(task);
    pool.submit(task);
    assertTrue(started.await(2, TimeUnit.SECONDS));
    assertTrue(finished.await(2, TimeUnit.SECONDS));
    pool.shutdownNow();

    assertEquals(1, maxInside.get());
  }

  @Test
  void allowsParallelCallsForDifferentOrganizations() throws Exception {
    DomainID first = DomainID.generate();
    DomainID second = DomainID.generate();
    CountDownLatch bothInside = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    List<String> order = new ArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(2);

    pool.submit(() -> lock.call(first, () -> {
      bothInside.countDown();
      await(bothInside);
      order.add("a");
      await(release);
      return null;
    }));
    pool.submit(() -> lock.call(second, () -> {
      bothInside.countDown();
      await(bothInside);
      order.add("b");
      await(release);
      return null;
    }));

    assertTrue(bothInside.await(2, TimeUnit.SECONDS));
    release.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
    assertEquals(2, order.size());
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(2, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
