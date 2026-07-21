package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GetDonationRequestsByPartyIdUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;

  public GetDonationRequestsByPartyIdUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository) {
    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
  }

  public List<OutputItem> execute(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");

    return donationRequestRepository.findByRequesterPartyId(partyId)
        .stream()
        .sorted(Comparator.comparing(DonationRequest::getDateRequested).reversed())
        .map(this::toOutput)
        .toList();
  }

  private OutputItem toOutput(DonationRequest request) {
    LocalDate currentDate = LocalDate.now();
    int fulfilledBloodBags = request.countFulfilledBloodBags(donationRepository, currentDate);

    return new OutputItem(
        request.getId().getValue().toString(),
        request.getBloodTypeNeeded().getType(),
        request.getDateRequested(),
        request.getDateLimit(),
        request.isActive(),
        request.getBloodCenter().getOrganization().getName(),
      request.getUrgency(),
      request.getGoalBloodBags(),
      fulfilledBloodBags,
      request.hasReachedGoal(donationRepository, currentDate));
  }

  public record OutputItem(
      String requestId,
      String bloodTypeNeeded,
      LocalDate dateRequested,
      LocalDate dateLimit,
      boolean active,
      String bloodCenterName,
      Urgency urgency,
      int goalBloodBags,
      int fulfilledBloodBags,
      boolean goalReached) {
  }
}
