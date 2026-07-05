package bloodmatch.interfaces.rest.donation.completefromrequest;

import bloodmatch.application.usecase.donation.completefromrequest.CompletePendingDonationFromRequestUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donations")
public class CompleteDonationFromRequestController {

  private final CompletePendingDonationFromRequestUseCase useCase;

  public CompleteDonationFromRequestController(CompletePendingDonationFromRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/from-request/complete")
  public ResponseEntity<?> complete(@RequestBody CompleteDonationFromRequestDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.donationId()))
        throw new IllegalArgumentException("donationId cannot be blank");
      if (payload.completionDate() == null)
        throw new IllegalArgumentException("completionDate cannot be null");

      DomainID donationId = parseDomainId(payload.donationId(), "donationId");

      Donation donation = useCase.execute(donationId, payload.completionDate());

        String status = donation.isCompleted() ? "COMPLETED" : donation.isPending() ? "PENDING" : donation.isCancelled() ? "CANCELLED" : "UNKNOWN";

        return ResponseEntity.ok(Map.of(
          "id", donation.getId().getValue().toString(),
          "completionDate", donation.getDonationDate().toString(),
          "status", status));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
