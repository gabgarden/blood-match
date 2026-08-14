package bloodmatch.application.usecase.donation.createcompleted;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.usecase.donation.fulfillment.DonationRequestFulfillmentRefresher;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreateCompletedDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonationRepositoryInterface donationRepository;
  private final DonationRequestFulfillmentRefresher fulfillmentRefresher;

  public CreateCompletedDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestFulfillmentRefresher fulfillmentRefresher) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (fulfillmentRefresher == null)
      throw new IllegalArgumentException("DonationRequestFulfillmentRefresher cannot be null");
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
    this.fulfillmentRefresher = fulfillmentRefresher;
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
    fulfillmentRefresher.refresh(bloodCenter.getOrganization().getId(), currentDate);

    return Output.from(donation);
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
