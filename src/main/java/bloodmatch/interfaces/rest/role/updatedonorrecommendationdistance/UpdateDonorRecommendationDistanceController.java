package bloodmatch.interfaces.rest.role.updatedonorrecommendationdistance;

import bloodmatch.application.usecase.role.UpdateDonorRecommendationDistanceUseCase;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@Tag(name = "Update Donor Recommendation Distance", description = "Configure the maximum distance for donation request recommendations.")
@RequestMapping("/donors")
public class UpdateDonorRecommendationDistanceController {

  private final UpdateDonorRecommendationDistanceUseCase useCase;

  public UpdateDonorRecommendationDistanceController(UpdateDonorRecommendationDistanceUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/recommendation-distance")
  public ResponseEntity<Map<String, Object>> update(@RequestBody UpdateDonorRecommendationDistanceDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.personId()))
        throw new IllegalArgumentException("personId cannot be blank");
      if (payload.maxDistanceInKm() == null || payload.maxDistanceInKm() <= 0)
        throw new IllegalArgumentException("maxDistanceInKm must be greater than zero");

      DomainID personId = parseDomainId(payload.personId(), "personId");
      Donor donor = useCase.execute(personId, payload.maxDistanceInKm());

      return ResponseEntity.ok(Map.of(
          "personId", donor.getPerson().getId().getValue().toString(),
          "maxDistanceInKm", donor.getMaxRecommendationDistanceKm()));
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
