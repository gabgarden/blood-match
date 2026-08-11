package bloodmatch.application.usecase.donationrequest;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
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
    if (donationRequestRepository == null) {
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    }
    this.donationRequestRepository = donationRequestRepository;
  }

  @Transactional
  public void execute(Input input) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }

    DomainID requestId = DomainIdParser.parse(input.requestId(), "requestId");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("Donation request not found"));

    PartyOwnership.requireSameParty(request.getRequester().getParty().getId(), input.actorPartyId());

    request.close();
    donationRequestRepository.save(request);
  }

  public record Input(String requestId, String actorPartyId) {
  }
}
