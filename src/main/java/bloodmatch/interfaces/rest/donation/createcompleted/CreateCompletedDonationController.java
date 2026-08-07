package bloodmatch.interfaces.rest.donation.createcompleted;

import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase;
import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase.Input;
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
@Tag(name = "Create Completed Donation", description = "Create a new completed donation.")
@RequestMapping("/donations")
public class CreateCompletedDonationController {

  private final CreateCompletedDonationUseCase useCase;

  public CreateCompletedDonationController(CreateCompletedDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/completed")
  public ResponseEntity<CreateCompletedDonationResponseDto> create(
      @RequestBody CreateCompletedDonationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNotBlank(payload.organizationId(), "organizationId cannot be blank");
    requireNonNull(payload.donationDate(), "donationDate cannot be null");
    requireSamePartyOrAdmin(payload.personId());

    var output = useCase.execute(new Input(
        payload.personId(),
        payload.organizationId(),
        payload.donationDate()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateCompletedDonationResponseDto.from(output));
  }
}
