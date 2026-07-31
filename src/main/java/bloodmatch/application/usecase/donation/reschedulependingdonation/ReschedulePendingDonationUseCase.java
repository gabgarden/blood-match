package bloodmatch.application.usecase.donation.reschedulependingdonation;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReschedulePendingDonationUseCase {

  private final DonationRepositoryInterface donationRepository;

  public ReschedulePendingDonationUseCase(DonationRepositoryInterface donationRepository) {
    this.donationRepository = donationRepository;
  }

  public Donation execute(DomainID donationId, LocalDate newExpectedDate) {
    return execute(donationId, newExpectedDate, LocalDate.now());
  }

  public Donation execute(DomainID donationId, LocalDate newExpectedDate, LocalDate currentDate) {
    if (donationId == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (newExpectedDate == null)
      throw new IllegalArgumentException("New expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Donation donation = donationRepository.findById(donationId)
        .orElseThrow(() -> new IllegalArgumentException("Donation not found"));

    donation.reschedule(newExpectedDate, currentDate);

    donationRepository.save(donation);

    return donation;
  }
}