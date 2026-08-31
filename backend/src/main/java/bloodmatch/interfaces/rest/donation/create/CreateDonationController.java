package bloodmatch.interfaces.rest.donation.create;

import bloodmatch.application.usecase.donation.create.CreateDonationUseCase;
import bloodmatch.application.usecase.donation.create.CreateDonationUseCase.Input;
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
@Tag(name = "Create Donation", description = "Create a pending donation with intendedDate, or a completed donation with donationDate.")
@RequestMapping("/donations")
public class CreateDonationController {

  private final CreateDonationUseCase useCase;

  public CreateDonationController(CreateDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<CreateDonationResponseDto> create(
      @RequestBody CreateDonationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNotBlank(payload.organizationId(), "organizationId cannot be blank");
    requireSamePartyOrAdmin(payload.personId());

    var output = useCase.execute(new Input(
        payload.personId(),
        payload.organizationId(),
        payload.intendedDate(),
        payload.donationDate(),
        TimeFormats.parseOptionalHourMinute(payload.expectedTime(), "expectedTime")));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateDonationResponseDto.from(output));
  }
}
