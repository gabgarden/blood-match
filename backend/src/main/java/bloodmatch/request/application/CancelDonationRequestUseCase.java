package bloodmatch.request.application;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.shared.application.shared.PartyOwnership;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;

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

    if (!java.util.Objects.equals(request.getVersion(), input.version())) {
      throw new bloodmatch.shared.application.exception.ConcurrencyException(
          "Resource version conflict: expected " + input.version() + " but found " + request.getVersion());
    }

    PartyOwnership.requireSameParty(request.getRequester().getParty().getId(), input.actorPartyId());

    request.close();
    donationRequestRepository.save(request);
  }

  public record Input(String requestId, Long version, String actorPartyId) {
  }
}
