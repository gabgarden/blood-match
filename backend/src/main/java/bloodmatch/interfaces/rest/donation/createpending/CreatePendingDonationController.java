package bloodmatch.interfaces.rest.donation.createpending;

import bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase;
import bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase.Input;
import bloodmatch.application.shared.TimeFormats;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Create Pending Donation", description = "Create a new pending donation.")
@RequestMapping("/donations")
public class CreatePendingDonationController {

  private final CreatePendingDonationUseCase useCase;

  public CreatePendingDonationController(CreatePendingDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/create-pending")
  public ResponseEntity<CreatePendingDonationResponseDto> execute(
      @RequestBody CreatePendingDonationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.organizationId(), "organizationId cannot be blank");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNonNull(payload.expectedDate(), "expectedDate cannot be null");
    requireSamePartyOrAdmin(payload.personId());

    var output = useCase.execute(new Input(
        payload.personId(),
        payload.organizationId(),
        payload.expectedDate(),
        TimeFormats.parseOptionalHourMinute(payload.expectedTime(), "expectedTime")));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreatePendingDonationResponseDto.from(output));
  }
}
