package bloodmatch.interfaces.rest.donation.gethistory;

import bloodmatch.application.usecase.donation.gethistory.GetDonorDonationHistoryUseCase;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donors")
public class GetDonorDonationHistoryController {

  private final GetDonorDonationHistoryUseCase useCase;

  public GetDonorDonationHistoryController(GetDonorDonationHistoryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/{personId}/donations")
  public ResponseEntity<?> getByPath(@PathVariable String personId) {
    return execute(personId);
  }



  private ResponseEntity<?> execute(String personIdValue) {
    try {
      if (isBlank(personIdValue))
        throw new IllegalArgumentException("personId cannot be blank");

      DomainID personId = parseDomainId(personIdValue, "personId");
      return ResponseEntity.ok(useCase.execute(personId));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
