package bloodmatch.request.presentation.notification;

import bloodmatch.request.application.notification.NotifyPotentialDonorsUseCase;
import bloodmatch.request.application.notification.NotifyPotentialDonorsUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.shared.presentation.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.shared.presentation.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Notify Potential Donors", description = "Notify eligible donors about a donation request.")
@RequestMapping("/donation-requests")
public class NotifyPotentialDonorsController {

  private final NotifyPotentialDonorsUseCase useCase;

  public NotifyPotentialDonorsController(NotifyPotentialDonorsUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/{id}/notify")
  public ResponseEntity<NotifyPotentialDonorsResponseDto> execute(
      @PathVariable("id") String requestIdValue) {
    requireNotBlank(requestIdValue, "Request ID cannot be blank");

    var output = useCase.execute(new Input(requestIdValue, actorPartyIdForOwnership()));

    return ResponseEntity.ok(NotifyPotentialDonorsResponseDto.from(output));
  }
}
