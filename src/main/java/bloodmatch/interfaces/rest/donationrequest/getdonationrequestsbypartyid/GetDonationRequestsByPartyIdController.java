package bloodmatch.interfaces.rest.donationrequest.getdonationrequestsbypartyid;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Get Donation Requests by Party ID", description = "Get all donation requests associated with a specific party.")
@RequestMapping("/donation-requests")
public class GetDonationRequestsByPartyIdController {

	private final GetDonationRequestsByPartyIdUseCase useCase;

	public GetDonationRequestsByPartyIdController(GetDonationRequestsByPartyIdUseCase useCase) {
		this.useCase = useCase;
	}

	@GetMapping("/{partyId}")
	public ResponseEntity<?> get(
			@PathVariable String partyId) {
		try {
			if (isBlank(partyId))	
				throw new IllegalArgumentException("Party id cannot be blank");

			DomainID partyDomainId = parseDomainId(partyId, "partyId");
			
			return ResponseEntity.ok(useCase.execute(partyDomainId));

		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	
}
