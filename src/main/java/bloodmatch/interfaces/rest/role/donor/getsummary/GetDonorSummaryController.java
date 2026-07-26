package bloodmatch.interfaces.rest.role.donor.getsummary;

import bloodmatch.application.usecase.donor.getsummary.GetDonorSummaryUseCase;
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
public class GetDonorSummaryController {

  private final GetDonorSummaryUseCase useCase;

  public GetDonorSummaryController(GetDonorSummaryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/{personId}/summary")
  public ResponseEntity<?> get(@PathVariable String personId) {
    return execute(personId);
  }

  private ResponseEntity<?> execute(String personIdValue) {
    try {
      if (isBlank(personIdValue))
        throw new IllegalArgumentException("personId cannot be blank");

      DomainID personId = parseDomainId(personIdValue, "personId");
      GetDonorSummaryUseCase.Output output = useCase.execute(personId);

      return ResponseEntity.ok(Map.of(
          "personId", output.personId(),
          "donorName", output.donorName(),
          "bloodType", output.bloodType(),
          "adreess", output.adreess(), // Só pra testar o endereço mrm, remover dps se quiser **
          "lastDonationDate", String.valueOf(output.lastDonationDate()),
          "daysRemaining", output.daysRemaining(),
          "livesImpacted", output.livesImpacted()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
