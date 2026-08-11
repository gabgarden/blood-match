package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestgoalbloodbags;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestGoalBloodBagsUseCase;
import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestGoalBloodBagsUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Update Donation Request Goal Blood Bags", description = "Update the goal blood bags of a blood donation request.")
@RequestMapping("/donation-requests")
public class UpdateDonationRequestGoalBloodBagsController {

  private final UpdateDonationRequestGoalBloodBagsUseCase useCase;

  public UpdateDonationRequestGoalBloodBagsController(UpdateDonationRequestGoalBloodBagsUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/goal-blood-bags")
  public ResponseEntity<UpdateDonationRequestGoalBloodBagsResponseDto> updateGoalBloodBags(
      @RequestBody UpdateDonationRequestGoalBloodBagsDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.requestId(), "requestId cannot be blank");
    if (payload.newGoalBloodBags() <= 0) {
      throw new ValidationException("newGoalBloodBags must be greater than zero");
    }

    var output = useCase.execute(new Input(
        payload.requestId(),
        payload.newGoalBloodBags(),
        actorPartyIdForOwnership()));

    return ResponseEntity.ok(UpdateDonationRequestGoalBloodBagsResponseDto.from(output));
  }
}
