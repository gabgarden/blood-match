package bloodmatch.application.usecase.donor.getsummary;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class GetDonorSummaryUseCase {

  private static final int CONSERVATIVE_INTERVAL_DAYS = 90;
  private static final int IMPACT_LIVES_PER_DONATION = 4;

  private final DonorRepositoryInterface donorRepository;
  private final DonationRepositoryInterface donationRepository;

  public GetDonorSummaryUseCase(
      DonorRepositoryInterface donorRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.donationRepository = donationRepository;
  }

  public Output execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public Output execute(Input input, LocalDate currentDate) {
    if (input == null)
      throw new ValidationException("Request cannot be null");
    if (currentDate == null)
      throw new ValidationException("Current date cannot be null");

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));

    LocalDate lastDonationDate = donor.getLastDonationDate();
    int daysRemaining = calculateDaysRemaining(lastDonationDate, currentDate);

    long donationsCount = donationRepository.countByDonorId(personId);
    long livesImpacted = donationsCount * IMPACT_LIVES_PER_DONATION;

    return new Output(
        donor.getPerson().getId().getValue().toString(),
        donor.getPerson().getVersion(),
        donor.getVersion(),
        donor.getPerson().getName(),
        donor.getPerson().getPhoneNumber() != null ? donor.getPerson().getPhoneNumber().getValue() : null,
        donor.getBloodType().getType(),
        donor.getPerson().getAddress() != null ? donor.getPerson().getAddress().getFullAddressAsString() : null,
        lastDonationDate,
        daysRemaining,
        livesImpacted,
        donor.getWeight(),
        donor.getWeightUpdatedAt());
  }

  private int calculateDaysRemaining(LocalDate lastDonationDate, LocalDate currentDate) {
    if (lastDonationDate == null)
      return 0;

    long elapsedDays = ChronoUnit.DAYS.between(lastDonationDate, currentDate);
    long remaining = CONSERVATIVE_INTERVAL_DAYS - elapsedDays;

    if (remaining < 0)
      return 0;

    return (int) remaining;
  }

  public record Input(String personId) {
  }

  public record Output(
      String personId,
      Long partyVersion,
      Long donorVersion,
      String donorName,
      String phoneNumber,
      String bloodType,
      String address,
      LocalDate lastDonationDate,
      int daysRemaining,
      long livesImpacted,
      Double weight,
      LocalDate weightUpdatedAt) {
  }
}
