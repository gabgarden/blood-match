package bloodmatch.interfaces.rest.donationrequest.getdonationrequestsbypartyid;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.security.JwtAuthenticationFilter.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/donation-requests")
public class GetDonationRequestsByPartyIdController {

	private final GetDonationRequestsByPartyIdUseCase useCase;

	public GetDonationRequestsByPartyIdController(GetDonationRequestsByPartyIdUseCase useCase) {
		this.useCase = useCase;
	}

	@GetMapping("/{partyId}")
	public ResponseEntity<?> get(
			@PathVariable String partyId,
			@AuthenticationPrincipal JwtPrincipal principal,
			Authentication authentication) {
		try {
			if (isBlank(partyId))	
				throw new IllegalArgumentException("Party id cannot be blank");

			DomainID partyDomainId = parseDomainId(partyId, "partyId");
			if (!isSystemAdmin(authentication) && !partyId.equals(principal.partyId()))
				return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
			return ResponseEntity.ok(useCase.execute(partyDomainId));

		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	private static boolean isSystemAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("SYSTEM_ADMIN"));
	}
}
