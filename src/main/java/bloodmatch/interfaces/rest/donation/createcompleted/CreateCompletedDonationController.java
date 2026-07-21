package bloodmatch.interfaces.rest.donation.createcompleted;

import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donations")
public class CreateCompletedDonationController {

  private final CreateCompletedDonationUseCase useCase;

  public CreateCompletedDonationController(CreateCompletedDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/completed")
  public ResponseEntity<?> create(@RequestBody CreateCompletedDonationDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.personId()))
        throw new IllegalArgumentException("personId cannot be blank");
      if (isBlank(payload.bloodCenterId()))
        throw new IllegalArgumentException("bloodCenterId cannot be blank");
      if (payload.donationDate() == null)
        throw new IllegalArgumentException("donationDate cannot be null");

      DomainID personId = parseDomainId(payload.personId(), "personId");
      DomainID bloodCenterId = parseDomainId(payload.bloodCenterId(), "bloodCenterId");

      Donation donation = useCase.execute(personId, bloodCenterId, payload.donationDate());

      String status = donation.isCompleted() ? "COMPLETED" : donation.isPending() ? "PENDING" : donation.isCancelled() ? "CANCELLED" : "UNKNOWN";

      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
          "id", donation.getId().getValue().toString(),
          "donationDate", donation.getDonationDate().toString(),
          "status", status));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}