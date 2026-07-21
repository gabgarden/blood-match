package bloodmatch.application.usecase.donation.createpending;

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
public class CreatePendingDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreatePendingDonationUseCase(
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

    if (!request.isActive())
      throw new IllegalStateException("Request is not active");
    if (request.isExpired(currentDate))
      throw new IllegalStateException("Request has expired");
    if (!donor.getBloodType().canDonateTo(request.getBloodTypeNeeded()))
      throw new IllegalStateException("Incompatible blood type");
    if (expectedDate.isAfter(request.getDateLimit()))
      throw new IllegalArgumentException("Expected date cannot be after the request deadline");

    Donation donation = Donation.createPending(donor, expectedDate, request.getBloodCenter(), currentDate);
    donationRepository.save(donation);

    return donation;
  }
}