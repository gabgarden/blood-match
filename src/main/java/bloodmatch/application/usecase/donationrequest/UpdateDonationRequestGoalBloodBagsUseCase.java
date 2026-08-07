package bloodmatch.application.usecase.donationrequest;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDonationRequestGoalBloodBagsUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;

  public UpdateDonationRequestGoalBloodBagsUseCase(DonationRequestRepositoryInterface donationRequestRepository) {
    if (donationRequestRepository == null) {
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    }
    this.donationRequestRepository = donationRequestRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }

    DomainID donationRequestId = DomainIdParser.parse(input.requestId(), "requestId");
    if (input.newGoalBloodBags() <= 0) {
      throw new ValidationException("newGoalBloodBags must be greater than zero");
    }

    DonationRequest donationRequest = donationRequestRepository.findById(donationRequestId)
        .orElseThrow(() -> new NotFoundException("DonationRequest not found"));

    donationRequest.setGoalBloodBags(input.newGoalBloodBags());
    donationRequestRepository.save(donationRequest);
    return Output.from(donationRequest);
  }

  public record Input(String requestId, int newGoalBloodBags) {
  }

  public record Output(String id, int goalBloodBags) {
    public static Output from(DonationRequest donationRequest) {
      return new Output(
          donationRequest.getId().getValue().toString(),
          donationRequest.getGoalBloodBags());
    }
  }
}
