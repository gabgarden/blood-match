package bloodmatch.donation.application.completependingdonation;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.shared.application.shared.PartyOwnership;
import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CompletePendingDonationUseCase {

  private final DonationRepositoryInterface donationRepository;
  private final DonorRepositoryInterface donorRepository;

  public CompletePendingDonationUseCase(
      DonationRepositoryInterface donationRepository,
      DonorRepositoryInterface donorRepository) {
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    this.donationRepository = donationRepository;
    this.donorRepository = donorRepository;
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

    if (!java.util.Objects.equals(donation.getVersion(), input.version())) {
      throw new bloodmatch.shared.application.exception.ConcurrencyException(
          "Resource version conflict: expected " + input.version() + " but found " + donation.getVersion());
    }

    PartyOwnership.requireSameParty(donation.getDonor().getPerson().getId(), input.actorPartyId());

    donation.complete(input.completionDate(), currentDate);
    donation.getDonor().registerDonation(input.completionDate(), currentDate);
    donorRepository.save(donation.getDonor());
    donationRepository.save(donation);
    return Output.from(donation);
  }

  public record Input(String donationId, Long version, LocalDate completionDate, String actorPartyId) {
  }

  public record Output(String id, LocalDate completionDate, String status) {
    public static Output from(Donation donation) {
      return new Output(
          donation.getId().getValue().toString(),
          donation.getDonationDate(),
          donation.status());
    }
  }
}
