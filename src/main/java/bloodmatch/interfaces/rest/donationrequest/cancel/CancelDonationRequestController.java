package bloodmatch.interfaces.rest.donationrequest.cancel;

import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donation-requests")
public class CancelDonationRequestController {

  private final CancelDonationRequestUseCase useCase;

  public CancelDonationRequestController(CancelDonationRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @DeleteMapping("/{requestId}")
  public ResponseEntity<?> cancel(@PathVariable String requestId) {
    try {
      if (isBlank(requestId))
        throw new IllegalArgumentException("requestId cannot be blank");

      DomainID id = parseDomainId(requestId, "requestId");
      useCase.execute(id);

      return ResponseEntity.noContent().build();

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
