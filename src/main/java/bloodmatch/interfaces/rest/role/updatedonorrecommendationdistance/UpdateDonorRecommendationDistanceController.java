package bloodmatch.interfaces.rest.role.updatedonorrecommendationdistance;

import bloodmatch.application.usecase.role.UpdateDonorRecommendationDistanceUseCase;
import bloodmatch.application.usecase.role.UpdateDonorRecommendationDistanceUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Update Donor Recommendation Distance", description = "Configure the maximum distance for donation request recommendations.")
@RequestMapping("/donors")
public class UpdateDonorRecommendationDistanceController {

  private final UpdateDonorRecommendationDistanceUseCase useCase;

  public UpdateDonorRecommendationDistanceController(UpdateDonorRecommendationDistanceUseCase useCase) {
    this.useCase = useCase;
  }

  @PatchMapping("/recommendation-distance")
  public ResponseEntity<UpdateDonorRecommendationDistanceResponseDto> update(
      @RequestBody UpdateDonorRecommendationDistanceDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNonNull(payload.maxDistanceInKm(), "maxDistanceInKm cannot be null");

    var output = useCase.execute(new Input(payload.personId(), payload.maxDistanceInKm()));

    return ResponseEntity.ok(UpdateDonorRecommendationDistanceResponseDto.from(output));
  }
}
