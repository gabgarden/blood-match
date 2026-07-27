package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelDonationRequestUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;

  public CancelDonationRequestUseCase(
      DonationRequestRepositoryInterface donationRequestRepository) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");

    this.donationRequestRepository = donationRequestRepository;
  }

  @Transactional
  public void execute(DomainID requestId) {
    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

    request.close();
    donationRequestRepository.save(request);
  }
}
