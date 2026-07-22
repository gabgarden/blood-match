package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestdatelimit;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestDateLimitUseCase;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.shared.valueObjects.DomainID;

@RestController
@RequestMapping("/donation-requests")
public class UpdateDonationRequestDateLimitController {
    private final UpdateDonationRequestDateLimitUseCase useCase;

    public UpdateDonationRequestDateLimitController(UpdateDonationRequestDateLimitUseCase useCase) {
        this.useCase = useCase;
    }

    @PatchMapping("/date-limit")
    public ResponseEntity<Map<String, String>> updateDateLimit(@RequestBody UpdateDonationRequestDateLimitDto payload) {
        try {
            if (payload == null)
                throw new IllegalArgumentException("Request body cannot be null");
            if (isBlank(payload.requestId()))
                throw new IllegalArgumentException("requestId cannot be blank");
            if (payload.newDateLimit() == null)
                throw new IllegalArgumentException("newDateLimit cannot be null");  
            if (payload.newDateLimit().isBefore(LocalDate.now()))
                throw new IllegalArgumentException("newDateLimit cannot be in the past");

            DomainID requestId = parseDomainId(payload.requestId(), "requestId");
            DonationRequest donationRequest = useCase.execute(requestId, payload.newDateLimit());

        return ResponseEntity.ok(Map.of(
                "id", donationRequest.getId().getValue().toString(),
                "dateLimit", donationRequest.getDateLimit().toString()));
        
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
