package bloodmatch.application.usecase.donation.completependingdonation;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CompletePendingDonationUseCase {

  private final DonationRepositoryInterface donationRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRequestFulfillmentService fulfillmentService;

  public CompletePendingDonationUseCase(
      DonationRepositoryInterface donationRepository,
      DonorRepositoryInterface donorRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    this.donationRepository = donationRepository;
    this.donorRepository = donorRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.fulfillmentService = fulfillmentService;
  }

  public Donation execute(DomainID donationId, LocalDate completionDate) {
    if (donationId == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (completionDate == null)
      throw new IllegalArgumentException("Completion date cannot be null");

    Donation donation = donationRepository.findById(donationId)
        .orElseThrow(() -> new IllegalArgumentException("Donation not found"));

    donation.complete(completionDate, LocalDate.now());
    donation.getDonor().registerDonation(completionDate, LocalDate.now());

    donorRepository.save(donation.getDonor());
    donationRepository.save(donation);
    refreshFulfillment(donation.getBloodCenter().getOrganization().getId(), LocalDate.now());

    return donation;
  }

  private void refreshFulfillment(DomainID bloodCenterId, LocalDate currentDate) {
    List<bloodmatch.domain.donationrequest.DonationRequest> requests =
        donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(bloodCenterId),
            currentDate);

    if (requests.isEmpty()) {
      return;
    }

    List<Donation> donations = donationRepository
        .findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(List.of(bloodCenterId));

    fulfillmentService.synchronize(requests, donations, currentDate);

    for (bloodmatch.domain.donationrequest.DonationRequest request : requests) {
      donationRequestRepository.save(request);
    }
  }
}
