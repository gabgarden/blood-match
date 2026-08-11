package bloodmatch.interfaces.rest.donationrequest.create;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donationrequest.CreateDonationRequestUseCase;
import bloodmatch.application.usecase.donationrequest.CreateDonationRequestUseCase.Input;
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
@Tag(name = "Create Donation Request", description = "Create a new blood donation request.")
@RequestMapping("/donation-requests")
public class CreateDonationRequestController {

  private final CreateDonationRequestUseCase useCase;

  public CreateDonationRequestController(CreateDonationRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<CreateDonationRequestResponseDto> create(@RequestBody CreateDonationRequestDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.partyId(), "partyId cannot be blank");
    requireNotBlank(payload.organizationId(), "organizationId cannot be blank");
    requireNotBlank(payload.bloodTypeNeeded(), "bloodTypeNeeded cannot be blank");
    requireNonNull(payload.goalBloodBags(), "goalBloodBags cannot be null");
    if (payload.goalBloodBags() <= 0) {
      throw new ValidationException("goalBloodBags must be greater than zero");
    }
    requireNonNull(payload.dateLimit(), "dateLimit cannot be null");
    requireNotBlank(payload.urgency(), "urgency cannot be blank");
    requireSamePartyOrAdmin(payload.partyId());

    var output = useCase.execute(new Input(
        payload.partyId(),
        payload.organizationId(),
        payload.bloodTypeNeeded(),
        payload.goalBloodBags(),
        payload.dateLimit(),
        payload.urgency(),
        payload.directedTo()));

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(CreateDonationRequestResponseDto.from(output));
  }
}
