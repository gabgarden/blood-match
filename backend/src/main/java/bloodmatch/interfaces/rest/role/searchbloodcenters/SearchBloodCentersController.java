package bloodmatch.interfaces.rest.role.searchbloodcenters;

import bloodmatch.application.usecase.role.searchbloodcenters.SearchBloodCentersUseCase;
import bloodmatch.application.usecase.role.searchbloodcenters.SearchBloodCentersUseCase.Input;
import bloodmatch.application.usecase.role.searchbloodcenters.SearchBloodCentersUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Search Blood Centers", description = "Search registered blood centers by organization name.")
@RequestMapping("/blood-centers")
public class SearchBloodCentersController {

  private final SearchBloodCentersUseCase useCase;

  public SearchBloodCentersController(SearchBloodCentersUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/search")
  public ResponseEntity<List<BloodCenterSearchResponseDto>> search(
      @RequestParam("q") String query,
      @RequestParam(value = "limit", required = false) Integer limit) {
    List<BloodCenterSearchResponseDto> body = useCase.execute(new Input(query, limit)).stream()
        .map(BloodCenterSearchResponseDto::from)
        .toList();

    return ResponseEntity.ok(body);
  }

  @Schema(description = "Blood center directory entry matched by name search.")
  public record BloodCenterSearchResponseDto(
      @Schema(description = "Organization UUID used as organizationId in write APIs") String organizationId,
      @Schema(description = "Organization display name") String name,
      @Schema(description = "City") String city,
      @Schema(description = "State UF") String state) {

    public static BloodCenterSearchResponseDto from(OutputItem item) {
      return new BloodCenterSearchResponseDto(
          item.organizationId(),
          item.name(),
          item.city(),
          item.state());
    }
  }
}
