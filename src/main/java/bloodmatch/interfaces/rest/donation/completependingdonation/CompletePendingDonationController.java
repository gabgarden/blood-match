package bloodmatch.interfaces.rest.donation.completependingdonation;

import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase;
import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Complete Pending Donation", description = "Complete a pending donation operation.")
@RequestMapping("/donations")
public class CompletePendingDonationController {

  private final CompletePendingDonationUseCase useCase;

  public CompletePendingDonationController(CompletePendingDonationUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/complete")
  public ResponseEntity<CompletePendingDonationResponseDto> complete(
      @RequestBody CompletePendingDonationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.donationId(), "donationId cannot be blank");
    requireNonNull(payload.completionDate(), "completionDate cannot be null");

    var output = useCase.execute(new Input(payload.donationId(), payload.completionDate()));

    return ResponseEntity.ok(CompletePendingDonationResponseDto.from(output));
  }
}
