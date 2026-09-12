package bloodmatch.application.usecase.donationrequest;

import bloodmatch.application.exception.ConcurrencyException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class UpdateDonationRequestUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;

  public UpdateDonationRequestUseCase(DonationRequestRepositoryInterface donationRequestRepository) {
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");

    this.donationRequestRepository = donationRequestRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.version() == null)
      throw new ValidationException("version cannot be null");

    if (input.goalBloodBags() == null && input.dateLimit() == null) {
      throw new ValidationException("At least one mutable field must be provided");
    }

    DomainID requestId = DomainIdParser.parse(input.requestId(), "requestId");

    DonationRequest request = donationRequestRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("Donation request not found"));

    if (!Objects.equals(request.getVersion(), input.version())) {
      throw new ConcurrencyException("Resource version conflict: expected " + input.version() + " but found " + request.getVersion());
    }

    PartyOwnership.requireSameParty(request.getRequester().getParty().getId(), input.actorPartyId());

    if (input.goalBloodBags() != null) {
      try {
        request.setGoalBloodBags(input.goalBloodBags());
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }

    if (input.dateLimit() != null) {
      try {
        request.setDateLimit(input.dateLimit());
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }

    donationRequestRepository.save(request);
    return Output.from(request);
  }

  public record Input(String requestId, Long version, Integer goalBloodBags, LocalDate dateLimit, String actorPartyId) {
  }

  public record Output(String id, Long version, Integer goalBloodBags, LocalDate dateLimit) {
    public static Output from(DonationRequest request) {
      return new Output(
          request.getId().getValue().toString(),
          request.getVersion(),
          request.getGoalBloodBags(),
          request.getDateLimit());
    }
  }
}
