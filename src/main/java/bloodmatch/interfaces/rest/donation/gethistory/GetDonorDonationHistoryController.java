package bloodmatch.interfaces.rest.donation.gethistory;

import bloodmatch.application.usecase.donation.gethistory.GetDonorDonationHistoryUseCase;
import bloodmatch.application.usecase.donation.gethistory.GetDonorDonationHistoryUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Get Donor Donation History", description = "Get the donation history of a specific donor by personId.")
@RequestMapping("/donors")
public class GetDonorDonationHistoryController {

  private final GetDonorDonationHistoryUseCase useCase;

  public GetDonorDonationHistoryController(GetDonorDonationHistoryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/{personId}/donations")
  public ResponseEntity<List<GetDonorDonationHistoryResponseDto>> getByPath(
      @PathVariable String personId) {
    requireNotBlank(personId, "personId cannot be blank");
    requireSamePartyOrAdmin(personId);

    List<GetDonorDonationHistoryResponseDto> body = useCase.execute(new Input(personId)).stream()
        .map(GetDonorDonationHistoryResponseDto::from)
        .toList();

    return ResponseEntity.ok(body);
  }
}
