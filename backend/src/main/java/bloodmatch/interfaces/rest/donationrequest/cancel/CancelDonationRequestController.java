package bloodmatch.interfaces.rest.donationrequest.cancel;

import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase;
import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Cancel Donation Request", description = "Cancel a  donation request.")
@RequestMapping("/donation-requests")
public class CancelDonationRequestController {

  private final CancelDonationRequestUseCase useCase;

  public CancelDonationRequestController(CancelDonationRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @DeleteMapping("/{requestId}")
  public ResponseEntity<Void> cancel(
      @PathVariable String requestId,
      @org.springframework.web.bind.annotation.RequestParam Long version) {
    requireNotBlank(requestId, "requestId cannot be blank");
    useCase.execute(new Input(requestId, version, actorPartyIdForOwnership()));
    return ResponseEntity.noContent().build();
  }
}
