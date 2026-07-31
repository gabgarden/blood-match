package bloodmatch.application.usecase.donor.getsummary;

import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
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

  public Output execute(DomainID personId) {
    return execute(personId, LocalDate.now());
  }

  public Output execute(DomainID personId, LocalDate currentDate) {
    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    LocalDate lastDonationDate = donor.getLastDonationDate();
    int daysRemaining = calculateDaysRemaining(lastDonationDate, currentDate);

    long donationsCount = donationRepository.countByDonorId(personId);
    long livesImpacted = donationsCount * IMPACT_LIVES_PER_DONATION;

    return new Output(
        donor.getPerson().getId().getValue().toString(),
        donor.getPerson().getName(),
        donor.getPerson().getPhoneNumber().getValue(),
        donor.getBloodType().getType(),
        donor.getPerson().getAddress().getFullAddressAsString(),
        lastDonationDate,
        daysRemaining,
        livesImpacted);
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

  public record Output(
      String personId,
      String donorName,
      String phoneNumber,
      String bloodType,
      String address, 
      LocalDate lastDonationDate,
      int daysRemaining,
      long livesImpacted) {
  }
}
