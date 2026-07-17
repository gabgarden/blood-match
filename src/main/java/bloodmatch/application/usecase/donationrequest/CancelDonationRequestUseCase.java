package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelDonationRequestUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;

  public CancelDonationRequestUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");

    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
  }

  @Transactional
  public void execute(DomainID requestId) {
    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

    if (donationRepository.existsByRequestId(requestId))
      throw new IllegalStateException("Cannot cancel request with associated donations");

    donationRequestRepository.deleteById(request.getId());
  }
}
