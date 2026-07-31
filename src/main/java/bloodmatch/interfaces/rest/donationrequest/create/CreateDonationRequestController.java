package bloodmatch.interfaces.rest.donationrequest.create;

import bloodmatch.application.usecase.donationrequest.CreateDonationRequestUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.shared.valueObjects.BloodType;
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
@Tag(name = "Create Donation Request", description = "Create a new blood donation request.")
@RequestMapping("/donation-requests")
public class CreateDonationRequestController {

  private final CreateDonationRequestUseCase useCase;

  public CreateDonationRequestController(CreateDonationRequestUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public ResponseEntity<Map<String, String>> create(@RequestBody CreateDonationRequestDto payload) {
    try {
      validatePayload(payload);

      DomainID requesterDomainId = parseDomainId(payload.partyId(), "partyId");
      DomainID organizationId = parseDomainId(payload.organizationId(), "organizationId");
      BloodType bloodTypeNeeded = BloodType.of(payload.bloodTypeNeeded());
      Urgency urgency = Urgency.valueOf(payload.urgency().toUpperCase());

      DonationRequest request = useCase.execute(
          requesterDomainId,
          organizationId,
          bloodTypeNeeded,
          payload.goalBloodBags(),
          payload.dateLimit(),
          urgency,
          payload.directedTo());

      return ResponseEntity
          .status(HttpStatus.CREATED)
          .body(Map.of("id", request.getId().getValue().toString()));

    } catch (IllegalArgumentException e) {
      return ResponseEntity
          .badRequest()
          .body(Map.of("error", e.getMessage()));
    }
  }

  private static void validatePayload(CreateDonationRequestDto payload) {
    if (payload == null)
      throw new IllegalArgumentException("Request body cannot be null");

    if (isBlank(payload.partyId()))
      throw new IllegalArgumentException("partyId cannot be blank");

    if (isBlank(payload.organizationId()))
      throw new IllegalArgumentException("organizationId cannot be blank");

    if (isBlank(payload.bloodTypeNeeded()))
      throw new IllegalArgumentException("bloodTypeNeeded cannot be blank");

    if (payload.goalBloodBags() == null)
      throw new IllegalArgumentException("goalBloodBags cannot be null");

    if (payload.goalBloodBags() <= 0)
      throw new IllegalArgumentException("goalBloodBags must be greater than zero");

    if (payload.dateLimit() == null)
      throw new IllegalArgumentException("dateLimit cannot be null");

    if (isBlank(payload.urgency()))
      throw new IllegalArgumentException("urgency cannot be blank");
      
    //directedTo é um campo opcional e pode ser null, por isso n tem validação.
  }
}