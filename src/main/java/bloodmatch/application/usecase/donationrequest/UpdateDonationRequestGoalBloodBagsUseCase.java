package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDonationRequestGoalBloodBagsUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;

  public UpdateDonationRequestGoalBloodBagsUseCase(DonationRequestRepositoryInterface donationRequestRepository) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");

    this.donationRequestRepository = donationRequestRepository;
  }

  @Transactional
  public DonationRequest execute(DomainID donationRequestId, int newGoalBloodBags) {

    if (donationRequestId == null)
      throw new IllegalArgumentException("DonationRequest id cannot be null");
    if (newGoalBloodBags <= 0)
      throw new IllegalArgumentException("newGoalBloodBags must be greater than zero");

    DonationRequest donationRequest = donationRequestRepository.findById(donationRequestId)
        .orElseThrow(() -> new IllegalArgumentException("DonationRequest not found"));

    donationRequest.setGoalBloodBags(newGoalBloodBags);
    donationRequestRepository.save(donationRequest);
    return donationRequest;
  }
}
