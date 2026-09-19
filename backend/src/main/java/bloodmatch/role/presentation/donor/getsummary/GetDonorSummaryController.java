package bloodmatch.role.presentation.donor.getsummary;

import bloodmatch.role.application.donor.getsummary.GetDonorSummaryUseCase;
import bloodmatch.role.application.donor.getsummary.GetDonorSummaryUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.shared.presentation.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.shared.presentation.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Get Donor Summary", description = "Get a summary of a donor's information.")
@RequestMapping("/donors")
public class GetDonorSummaryController {

  private final GetDonorSummaryUseCase useCase;

  public GetDonorSummaryController(GetDonorSummaryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/{personId}/summary")
  public ResponseEntity<GetDonorSummaryResponseDto> get(@PathVariable String personId) {
    requireNotBlank(personId, "personId cannot be blank");
    requireSamePartyOrAdmin(personId);

    var output = useCase.execute(new Input(personId));

    return ResponseEntity.ok(GetDonorSummaryResponseDto.from(output));
  }
}
