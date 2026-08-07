package bloodmatch.interfaces.rest.donationrequest.notification;

import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase;
import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase.Input;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@RequestMapping("/requests")
public class NotifyPotentialDonorsController {

  private final NotifyPotentialDonorsUseCase useCase;

  public NotifyPotentialDonorsController(NotifyPotentialDonorsUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/{id}/notify")
  public ResponseEntity<NotifyPotentialDonorsResponseDto> execute(
      @PathVariable("id") String requestIdValue) {
    requireNotBlank(requestIdValue, "Request ID cannot be blank");

    var output = useCase.execute(new Input(requestIdValue));

    return ResponseEntity.ok(NotifyPotentialDonorsResponseDto.from(output));
  }
}
