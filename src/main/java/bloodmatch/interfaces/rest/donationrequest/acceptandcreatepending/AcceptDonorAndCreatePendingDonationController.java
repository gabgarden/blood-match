package bloodmatch.interfaces.rest.donationrequest.acceptandcreatepending;

import bloodmatch.application.usecase.donation.acceptandcreatependingfromrequest.AcceptDonorAndCreatePendingDonationFromRequestUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donation-requests")
public class AcceptDonorAndCreatePendingDonationController {

  private final AcceptDonorAndCreatePendingDonationFromRequestUseCase useCase;

  public AcceptDonorAndCreatePendingDonationController(AcceptDonorAndCreatePendingDonationFromRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/accept-and-create-pending")
  public ResponseEntity<?> execute(@RequestBody AcceptDonorAndCreatePendingDonationDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.requestId()))
        throw new IllegalArgumentException("requestId cannot be blank");
      if (isBlank(payload.personId()))
        throw new IllegalArgumentException("personId cannot be blank");
      if (payload.expectedDate() == null)
        throw new IllegalArgumentException("expectedDate cannot be null");

      DomainID requestId = parseDomainId(payload.requestId(), "requestId");
      DomainID personId = parseDomainId(payload.personId(), "personId");

      Donation donation = useCase.execute(requestId, personId, payload.expectedDate());

        String status = donation.isCompleted() ? "COMPLETED" : donation.isPending() ? "PENDING" : donation.isCancelled() ? "CANCELLED" : "UNKNOWN";

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
          "id", donation.getId().getValue().toString(),
          "expectedDate", donation.getDonationDate().toString(),
          "requestId", donation.getRequest().getId().getValue().toString(),
          "status", status));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
