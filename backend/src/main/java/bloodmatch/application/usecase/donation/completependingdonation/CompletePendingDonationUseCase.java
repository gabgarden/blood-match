package bloodmatch.application.usecase.donation.completependingdonation;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.application.usecase.donation.fulfillment.DonationRequestFulfillmentRefresher;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CompletePendingDonationUseCase {

  private final DonationRepositoryInterface donationRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestFulfillmentRefresher fulfillmentRefresher;

  public CompletePendingDonationUseCase(
      DonationRepositoryInterface donationRepository,
      DonorRepositoryInterface donorRepository,
      DonationRequestFulfillmentRefresher fulfillmentRefresher) {
    this.donationRepository = donationRepository;
    this.donorRepository = donorRepository;
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

    DomainID donationId = DomainIdParser.parse(input.donationId(), "donationId");
    if (input.completionDate() == null) {
      throw new ValidationException("completionDate cannot be null");
    }

    Donation donation = donationRepository.findById(donationId)
        .orElseThrow(() -> new NotFoundException("Donation not found"));

    PartyOwnership.requireSameParty(donation.getDonor().getPerson().getId(), input.actorPartyId());

    donation.complete(input.completionDate(), currentDate);
    donation.getDonor().registerDonation(input.completionDate(), currentDate);

    donorRepository.save(donation.getDonor());
    donationRepository.save(donation);
    fulfillmentRefresher.refresh(
        donation.getBloodCenter().getOrganization().getId(), currentDate);

    return Output.from(donation);
  }

  public record Input(String donationId, LocalDate completionDate, String actorPartyId) {
  }

  public record Output(String id, LocalDate completionDate, String status) {
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
