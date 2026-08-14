package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Materializes {@code fulfilledBloodBags} for active requests at a blood center
 * after the completed-donation pool changes. See docs/FULFILLMENT_STRATEGY.md.
 */
@Service
public class DonationRequestFulfillmentRefresher {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;
  private final DonationRequestFulfillmentService fulfillmentService;

  public DonationRequestFulfillmentRefresher(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (fulfillmentService == null)
      throw new IllegalArgumentException("DonationRequestFulfillmentService cannot be null");
    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
    this.fulfillmentService = fulfillmentService;
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

    for (DonationRequest request : requests) {
      donationRequestRepository.save(request);
    }
  }
}
