package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestgoalbloodbags;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestGoalBloodBagsUseCase;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.shared.valueObjects.DomainID;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

import java.util.Map;

@RestController
@RequestMapping("/donation-requests")
public class UpdateDonationRequestGoalBloodBagsController {

    private final UpdateDonationRequestGoalBloodBagsUseCase useCase;
    
    public UpdateDonationRequestGoalBloodBagsController(UpdateDonationRequestGoalBloodBagsUseCase useCase) {
        this.useCase = useCase;
    }
    
    @PatchMapping("/goal-blood-bags")
    public ResponseEntity<Map<String, String>> updateGoalBloodBags(@RequestBody UpdateDonationRequestGoalBloodBagsDto payload) {
     try {
        if (payload == null)
            throw new IllegalArgumentException("Request body cannot be null");
        if (isBlank(payload.requestId()))
            throw new IllegalArgumentException("requestId cannot be blank");
        if (payload.newGoalBloodBags() <= 0)
            throw new IllegalArgumentException("newGoalBloodBags must be greater than zero");

        DomainID requestId = parseDomainId(payload.requestId(), "requestId");
        DonationRequest donationRequest = useCase.execute(requestId, payload.newGoalBloodBags());

        return ResponseEntity.ok(Map.of(
                "id", donationRequest.getId().getValue().toString(),
                "goalBloodBags", String.valueOf(donationRequest.getGoalBloodBags())));
        
    } catch (IllegalArgumentException | IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    }
}
