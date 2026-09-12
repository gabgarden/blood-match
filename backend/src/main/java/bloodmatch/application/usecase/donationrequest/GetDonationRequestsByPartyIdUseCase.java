package bloodmatch.application.usecase.donationrequest;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class GetDonationRequestsByPartyIdUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRequestFulfillmentService fulfillmentService;

  public GetDonationRequestsByPartyIdUseCase(
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    this.donationRequestRepository = donationRequestRepository;
    this.fulfillmentService = fulfillmentService;
  }

  public List<OutputItem> execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public List<OutputItem> execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID partyId = DomainIdParser.parse(input.partyId(), "partyId");

    List<DonationRequest> userRequests =
        donationRequestRepository.findByRequesterPartyId(partyId);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot =
        fulfillmentService.fill(userRequests, currentDate);

    return userRequests.stream()
        .sorted(
            Comparator.comparing(DonationRequest::getDateRequested)
                .reversed()
                .thenComparing(request -> request.getId().getValue()))
        .map(request -> toOutput(request, currentDate, snapshot))
        .toList();
  }

  private OutputItem toOutput(
      DonationRequest request,
      LocalDate currentDate,
      Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot) {
    DonationRequestFulfillmentStatusRecord status = snapshot.getOrDefault(
        request.getId(),
        new DonationRequestFulfillmentStatusRecord(0, false));
    int fulfilledBloodBags = status.fulfilledBloodBags();

    return new OutputItem(
        request.getId().getValue().toString(),
        request.getVersion(),
        request.getBloodTypeNeeded().getType(),
        request.getDateRequested(),
        request.getDateLimit(),
        request.isActive(),
        request.isExpired(currentDate),
        request.getBloodCenter().getOrganization().getName(),
        request.getBloodCenter().getOrganization().getPhoneNumber() != null ? request.getBloodCenter().getOrganization().getPhoneNumber().getValue() : null,
        request.getUrgency().name(),
        request.getGoalBloodBags(),
        fulfilledBloodBags,
        Math.max(0, request.getGoalBloodBags() - fulfilledBloodBags),
        status.goalReached());
  }

  public record Input(String partyId) {
  }

  public record OutputItem(
      String requestId,
      Long version,
      String bloodTypeNeeded,
      LocalDate dateRequested,
      LocalDate dateLimit,
      boolean active,
      boolean expired,
      String bloodCenterName,
      String bloodCenterPhoneNumber,
      String urgency,
      int goalBloodBags,
      int fulfilledBloodBags,
      int remainingBloodBags,
      boolean goalReached) {
  }
}
