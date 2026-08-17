package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.application.exception.ConflictException;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Materializes {@code fulfilledBloodBags} for active requests at a blood center
 * after the completed-donation pool changes. Reloads the full batch on
 * optimistic-lock collisions. See docs/FULFILLMENT_STRATEGY.md and
 * docs/FULFILLMENT_CONCURRENCY.md.
 */
@Service
public class DonationRequestFulfillmentRefresher {

  static final int DEFAULT_MAX_ATTEMPTS = 8;
  static final String CONFLICT_MESSAGE =
      "Donation request was changed by another operation. Reload it and try again.";

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;
  private final DonationRequestFulfillmentService fulfillmentService;
  private final int maxAttempts;
  private final AttemptSleeper sleeper;

  @Autowired
  public DonationRequestFulfillmentRefresher(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    this(donationRequestRepository, donationRepository, fulfillmentService, DEFAULT_MAX_ATTEMPTS,
        attempt -> Thread.sleep(Math.min(50L * attempt, 200L)));
  }

  DonationRequestFulfillmentRefresher(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestFulfillmentService fulfillmentService,
      int maxAttempts,
      AttemptSleeper sleeper) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (fulfillmentService == null)
      throw new IllegalArgumentException("FulfillmentService cannot be null");
    if (maxAttempts <= 0)
      throw new IllegalArgumentException("Max attempts must be greater than zero");
    if (sleeper == null)
      throw new IllegalArgumentException("Sleeper cannot be null");
    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
    this.fulfillmentService = fulfillmentService;
    this.maxAttempts = maxAttempts;
    this.sleeper = sleeper;
  }

  /**
   * @param organizationId party id of the blood center's organization
   *                       (same value stored as {@code organizationId} on request/donation docs)
   */
  public void refresh(DomainID organizationId, LocalDate currentDate) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      List<DonationRequest> requests =
          donationRequestRepository.findActiveRequestsByOrganizationIds(
              List.of(organizationId),
              currentDate);

      if (requests.isEmpty()) {
        return;
      }

      List<Donation> donations = donationRepository
          .findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(List.of(organizationId));

      fulfillmentService.synchronize(requests, donations, currentDate);

      try {
        persistAll(requests);
        return;
      } catch (RuntimeException exception) {
        if (!isOptimisticLockConflict(exception)) {
          throw exception;
        }
        if (attempt == maxAttempts) {
          break;
        }
        pause(attempt);
      }
    }

    throw new ConflictException(CONFLICT_MESSAGE);
  }

  private void persistAll(List<DonationRequest> requests) {
    for (DonationRequest request : requests) {
      donationRequestRepository.save(request);
    }
  }

  private void pause(int attempt) {
    try {
      sleeper.sleep(attempt);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ConflictException(CONFLICT_MESSAGE);
    }
  }

  static boolean isOptimisticLockConflict(Throwable exception) {
    if (exception == null) {
      return false;
    }
    String message = exception.getMessage();
    if (exception instanceof IllegalStateException
        && message != null
        && message.contains("changed by another operation")) {
      return true;
    }
    return isOptimisticLockConflict(exception.getCause());
  }

  @FunctionalInterface
  interface AttemptSleeper {
    void sleep(int attempt) throws InterruptedException;
  }
}
