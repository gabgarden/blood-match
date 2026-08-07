package bloodmatch.application.usecase.donation.createcompleted;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CreateCompletedDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonationRepositoryInterface donationRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRequestFulfillmentService fulfillmentService;

  public CreateCompletedDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (fulfillmentService == null)
      throw new IllegalArgumentException("DonationRequestFulfillmentService cannot be null");
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.fulfillmentService = fulfillmentService;
  }

  public Output execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public Output execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");
    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    if (input.donationDate() == null) {
      throw new ValidationException("donationDate cannot be null");
    }

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    Donation donation = Donation.registerExternalDonation(donor, input.donationDate(), bloodCenter, currentDate);
    donor.registerDonation(input.donationDate(), currentDate);
    donorRepository.save(donor);
    donationRepository.save(donation);
    refreshFulfillment(bloodCenter.getOrganization().getId(), currentDate);

    return Output.from(donation);
  }

  private void refreshFulfillment(DomainID bloodCenterId, LocalDate currentDate) {
    List<DonationRequest> requests =
        donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(bloodCenterId),
            currentDate);

    if (requests.isEmpty()) {
      return;
    }

    List<Donation> donations = donationRepository
        .findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(List.of(bloodCenterId));

    fulfillmentService.synchronize(requests, donations, currentDate);

    for (DonationRequest request : requests) {
      donationRequestRepository.save(request);
    }
  }

  public record Input(String personId, String organizationId, LocalDate donationDate) {
  }

  public record Output(String id, LocalDate donationDate, String status) {
    public static Output from(Donation donation) {
      return new Output(
          donation.getId().getValue().toString(),
          donation.getDonationDate(),
          statusOf(donation));
    }

    private static String statusOf(Donation donation) {
      if (donation.isCompleted()) {
        return "COMPLETED";
      }
      if (donation.isPending()) {
        return "PENDING";
      }
      if (donation.isCancelled()) {
        return "CANCELLED";
      }
      return "UNKNOWN";
    }
  }
}
