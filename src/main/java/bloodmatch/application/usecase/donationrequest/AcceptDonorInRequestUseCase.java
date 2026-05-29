package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcceptDonorInRequestUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonorRepositoryInterface donorRepository;

  public AcceptDonorInRequestUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonorRepositoryInterface donorRepository) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");

    this.donationRequestRepository = donationRequestRepository;
    this.donorRepository = donorRepository;
  }

  @Transactional
  public void execute(
      DomainID requestId,
      DomainID donorId) {
    execute(requestId, donorId, LocalDate.now());
  }

  @Transactional
  public void execute(
      DomainID requestId,
      DomainID donorId,
      LocalDate currentDate) {

    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    request.acceptBy(donor, currentDate);


    donationRequestRepository.save(request);
  }
}
