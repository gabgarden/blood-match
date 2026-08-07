package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestdatelimit;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestDateLimitUseCase;
import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestDateLimitUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Update Donation Request Date Limit", description = "Update the date limit of a blood donation request.")
@RequestMapping("/donation-requests")
public class UpdateDonationRequestDateLimitController {

  private final UpdateDonationRequestDateLimitUseCase useCase;

  public UpdateDonationRequestDateLimitController(UpdateDonationRequestDateLimitUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/date-limit")
  public ResponseEntity<UpdateDonationRequestDateLimitResponseDto> updateDateLimit(
      @RequestBody UpdateDonationRequestDateLimitDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.requestId(), "requestId cannot be blank");
    requireNonNull(payload.newDateLimit(), "newDateLimit cannot be null");

    var output = useCase.execute(new Input(payload.requestId(), payload.newDateLimit()));

    return ResponseEntity.ok(UpdateDonationRequestDateLimitResponseDto.from(output));
  }
}
