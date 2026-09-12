package bloodmatch.interfaces.rest.donationrequest.updatedonationrequest;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donation-requests")
public class UpdateDonationRequestController {

  private final UpdateDonationRequestUseCase updateDonationRequestUseCase;

  public UpdateDonationRequestController(UpdateDonationRequestUseCase updateDonationRequestUseCase) {
    this.updateDonationRequestUseCase = updateDonationRequestUseCase;
  }

  @PatchMapping
  public ResponseEntity<UpdateDonationRequestResponseDto> updateDonationRequest(@RequestBody UpdateDonationRequestDto dto) {
      
    String actorPartyId = bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership();
      
    UpdateDonationRequestUseCase.Input input = new UpdateDonationRequestUseCase.Input(
        dto.requestId(),
        dto.version(),
        dto.goalBloodBags(),
        dto.dateLimit(),
        actorPartyId);

    UpdateDonationRequestUseCase.Output output = updateDonationRequestUseCase.execute(input);

    return ResponseEntity.ok(UpdateDonationRequestResponseDto.from(output));
  }
}
