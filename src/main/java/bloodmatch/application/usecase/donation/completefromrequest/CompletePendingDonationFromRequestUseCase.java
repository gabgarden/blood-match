package bloodmatch.application.usecase.donation.completefromrequest;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CompletePendingDonationFromRequestUseCase {

  private final DonationRepositoryInterface donationRepository;
  private final DonorRepositoryInterface donorRepository;

  public CompletePendingDonationFromRequestUseCase(
      DonationRepositoryInterface donationRepository,
      DonorRepositoryInterface donorRepository) {
    this.donationRepository = donationRepository;
    this.donorRepository = donorRepository;
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

    return donation;
  }
}
