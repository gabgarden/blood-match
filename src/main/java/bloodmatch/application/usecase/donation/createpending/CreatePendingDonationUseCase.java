package bloodmatch.application.usecase.donation.createpending;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreatePendingDonationUseCase {
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreatePendingDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
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

    DomainID donorId = DomainIdParser.parse(input.personId(), "personId");
    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    if (input.expectedDate() == null) {
      throw new ValidationException("expectedDate cannot be null");
    }

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    Donation donation = Donation.createPending(donor, input.expectedDate(), bloodCenter, currentDate);
    donationRepository.save(donation);

    return Output.from(donation);
  }

  public record Input(String personId, String organizationId, LocalDate expectedDate) {
  }

  public record Output(String id, LocalDate expectedDate, String status) {
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
