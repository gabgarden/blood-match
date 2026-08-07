package bloodmatch.interfaces.rest.donationrequest.recommendations;

import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Get Recommended Requests", description = "Get a list of recommended donation requests for a specific person.")
@RequestMapping("/requests")
public class GetRecommendedRequestsController {

  private final GetRecommendedRequestsUseCase useCase;

  public GetRecommendedRequestsController(GetRecommendedRequestsUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/recommendations")
  public ResponseEntity<List<RecommendedRequestResponseDto>> getByQuery(@RequestParam String personId) {
    requireNotBlank(personId, "personId cannot be blank");

    List<RecommendedRequestResponseDto> body = useCase.execute(new Input(personId)).stream()
        .map(RecommendedRequestResponseDto::from)
        .toList();

    return ResponseEntity.ok(body);
  }
}
