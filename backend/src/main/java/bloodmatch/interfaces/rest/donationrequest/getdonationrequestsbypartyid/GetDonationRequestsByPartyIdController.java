package bloodmatch.interfaces.rest.donationrequest.getdonationrequestsbypartyid;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Get Donation Requests by Party ID", description = "Get all donation requests associated with a specific party.")
@RequestMapping("/donation-requests")
public class GetDonationRequestsByPartyIdController {

  private final GetDonationRequestsByPartyIdUseCase useCase;

  public GetDonationRequestsByPartyIdController(GetDonationRequestsByPartyIdUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/{partyId}")
  public ResponseEntity<List<DonationRequestByPartyResponseDto>> get(@PathVariable String partyId) {
    requireNotBlank(partyId, "Party id cannot be blank");
    requireSamePartyOrAdmin(partyId);

    List<DonationRequestByPartyResponseDto> body = useCase.execute(new Input(partyId)).stream()
        .map(DonationRequestByPartyResponseDto::from)
        .toList();

    return ResponseEntity.ok(body);
  }
}
