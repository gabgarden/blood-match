package bloodmatch.interfaces.rest.donation.reschedulependingdonation;

import bloodmatch.application.usecase.donation.reschedulependingdonation.ReschedulePendingDonationUseCase;
import bloodmatch.application.usecase.donation.reschedulependingdonation.ReschedulePendingDonationUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Reschedule Pending Donation", description = "Reschedule a pending donation operation.")
@RequestMapping("/donations")
public class ReschedulePendingDonationController {

  private final ReschedulePendingDonationUseCase useCase;

  public ReschedulePendingDonationController(ReschedulePendingDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/reschedule")
  public ResponseEntity<ReschedulePendingDonationResponseDto> reschedule(
      @RequestBody ReschedulePendingDonationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.donationId(), "donationId cannot be blank");
    requireNonNull(payload.newExpectedDate(), "newExpectedDate cannot be null");

    var output = useCase.execute(new Input(payload.donationId(), payload.newExpectedDate()));

    return ResponseEntity.ok(ReschedulePendingDonationResponseDto.from(output));
  }
}
