package bloodmatch.interfaces.rest.donationrequest.notification;

import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping("/requests")
public class NotifyPotentialDonorsController {

    private final NotifyPotentialDonorsUseCase useCase;

    public NotifyPotentialDonorsController(NotifyPotentialDonorsUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<?> execute(@PathVariable("id") String requestIdValue) {
        try {
            if (isBlank(requestIdValue))
                throw new IllegalArgumentException("Request ID cannot be blank");

            DomainID requestId = parseDomainId(requestIdValue, "requestId");
            
            useCase.execute(requestId, LocalDate.now());

            return ResponseEntity.ok(Map.of("message", "Notifications sent to eligible donors successfully."));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}