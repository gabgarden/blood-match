package bloodmatch.interfaces.rest.donation.createexternal;

import bloodmatch.application.usecase.donation.createexternal.CreateExternalDonationUseCase;
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
public class CreateExternalDonationController {

  private final CreateExternalDonationUseCase useCase;

  public CreateExternalDonationController(CreateExternalDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/external")
  public ResponseEntity<?> create(@RequestBody CreateExternalDonationDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.donorId()))
        throw new IllegalArgumentException("donorId cannot be blank");
      if (isBlank(payload.bloodCenterId()))
        throw new IllegalArgumentException("bloodCenterId cannot be blank");
      if (payload.donationDate() == null)
        throw new IllegalArgumentException("donationDate cannot be null");

      DomainID donorId = parseDomainId(payload.donorId(), "donorId");
      DomainID bloodCenterId = parseDomainId(payload.bloodCenterId(), "bloodCenterId");

      Donation donation = useCase.execute(donorId, bloodCenterId, payload.donationDate());

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
