package bloodmatch.interfaces.rest.donation.reschedulependingdonation;

import bloodmatch.application.usecase.donation.reschedulependingdonation.ReschedulePendingDonationUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reschedule Pending Donation", description = "Reschedule a pending donation operation.")
@RequestMapping("/donations")
public class ReschedulePendingDonationController {

  private final ReschedulePendingDonationUseCase useCase;

  public ReschedulePendingDonationController(ReschedulePendingDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/reschedule")
  public ResponseEntity<?> reschedule(@RequestBody ReschedulePendingDonationDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.donationId()))
        throw new IllegalArgumentException("donationId cannot be blank");
      if (payload.newExpectedDate() == null)
        throw new IllegalArgumentException("newExpectedDate cannot be null");

      DomainID donationId = parseDomainId(payload.donationId(), "donationId");

      Donation donation = useCase.execute(donationId, payload.newExpectedDate());

      String status = donation.isCompleted() ? "COMPLETED" : donation.isPending() ? "PENDING" : donation.isCancelled() ? "CANCELLED" : "UNKNOWN";

      return ResponseEntity.ok(Map.of(
          "id", donation.getId().getValue().toString(),
          "expectedDate", donation.getDonationDate().toString(),
          "status", status));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}