package bloodmatch.application.usecase.donation.creatependingfromrequest;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreatePendingDonationFromRequestUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreatePendingDonationFromRequestUseCase(
      DonorRepositoryInterface donorRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
  }

  public Donation execute(
      DomainID donorId,
      DomainID requestId,
      LocalDate expectedDate) {

    return execute(donorId, requestId, expectedDate, LocalDate.now());
  }

  public Donation execute(
      DomainID donorId,
      DomainID requestId,
      LocalDate expectedDate,
      LocalDate currentDate) {

    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");
    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");
    if (expectedDate == null)
      throw new IllegalArgumentException("Expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

    Donation donation = Donation.createFromRequest(donor, request, expectedDate, currentDate);
    donationRepository.save(donation);

    return donation;
  }
}
