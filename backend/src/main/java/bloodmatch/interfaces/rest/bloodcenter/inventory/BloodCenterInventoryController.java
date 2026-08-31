package bloodmatch.interfaces.rest.bloodcenter.inventory;

import bloodmatch.application.shared.TimeFormats;
import bloodmatch.application.usecase.bloodcenter.inventory.BloodCenterInventoryOutput;
import bloodmatch.application.usecase.bloodcenter.inventory.GetBloodCenterInventoryUseCase;
import bloodmatch.application.usecase.bloodcenter.inventory.GetBloodCentersInventoryOverviewUseCase;
import bloodmatch.application.usecase.bloodcenter.inventory.UpsertBloodCenterInventoryUseCase;
import bloodmatch.application.usecase.bloodcenter.inventory.UpsertBloodCenterInventoryUseCase.ItemInput;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.authenticatedPartyId;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Blood Center Inventory", description = "Publish and view blood-center stock levels.")
@RequestMapping("/blood-centers")
public class BloodCenterInventoryController {

  private final GetBloodCentersInventoryOverviewUseCase overviewUseCase;
  private final GetBloodCenterInventoryUseCase getUseCase;
  private final UpsertBloodCenterInventoryUseCase upsertUseCase;

  public BloodCenterInventoryController(
      GetBloodCentersInventoryOverviewUseCase overviewUseCase,
      GetBloodCenterInventoryUseCase getUseCase,
      UpsertBloodCenterInventoryUseCase upsertUseCase) {
    this.overviewUseCase = overviewUseCase;
    this.getUseCase = getUseCase;
    this.upsertUseCase = upsertUseCase;
  }

  @GetMapping("/inventory")
  public ResponseEntity<List<InventoryOverviewResponseDto>> overview() {
    List<InventoryOverviewResponseDto> body = overviewUseCase.execute().stream()
        .map(InventoryOverviewResponseDto::from)
        .toList();
    return ResponseEntity.ok(body);
  }

  @GetMapping("/{organizationId}/inventory")
  public ResponseEntity<InventoryResponseDto> getByOrganization(
      @PathVariable String organizationId) {
    requireNotBlank(organizationId, "organizationId cannot be blank");
    return ResponseEntity.ok(InventoryResponseDto.from(
        getUseCase.execute(new GetBloodCenterInventoryUseCase.Input(organizationId))));
  }

  @PutMapping("/inventory")
  public ResponseEntity<InventoryResponseDto> upsert(@RequestBody UpsertInventoryDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    String organizationId = authenticatedPartyId();
    requireSamePartyOrAdmin(organizationId);

    BloodCenterInventoryOutput output = upsertUseCase.execute(
        new UpsertBloodCenterInventoryUseCase.Input(
            organizationId,
            payload.items() == null
                ? List.of()
                : payload.items().stream()
                    .map(item -> new ItemInput(item.bloodType(), item.percentage()))
                    .toList(),
            actorPartyIdForOwnership()));

    return ResponseEntity.ok(InventoryResponseDto.from(output));
  }

  @Schema(description = "Inventory for a single blood center.")
  public record InventoryResponseDto(
      String organizationId,
      String updatedAt,
      List<InventoryItemDto> items) {

    public static InventoryResponseDto from(BloodCenterInventoryOutput output) {
      return new InventoryResponseDto(
          output.organizationId(),
          TimeFormats.format(output.updatedAt()),
          output.items().stream().map(InventoryItemDto::from).toList());
    }
  }

  @Schema(description = "Inventory directory entry for a blood center.")
  public record InventoryOverviewResponseDto(
      String organizationId,
      String name,
      String city,
      String state,
      String updatedAt,
      List<InventoryItemDto> items) {

    public static InventoryOverviewResponseDto from(BloodCenterInventoryOutput output) {
      return new InventoryOverviewResponseDto(
          output.organizationId(),
          output.name(),
          output.city(),
          output.state(),
          TimeFormats.format(output.updatedAt()),
          output.items().stream().map(InventoryItemDto::from).toList());
    }
  }

  @Schema(description = "Stock percentage and status label for a blood type.")
  public record InventoryItemDto(String bloodType, int percentage, String label) {

    public static InventoryItemDto from(BloodCenterInventoryOutput.ItemOutput item) {
      return new InventoryItemDto(item.bloodType(), item.percentage(), item.label());
    }
  }

  @Schema(description = "Payload to publish inventory for the authenticated blood center.")
  public record UpsertInventoryDto(List<UpsertInventoryItemDto> items) {
  }

  @Schema(description = "Partial stock update. Missing blood types default to 0.")
  public record UpsertInventoryItemDto(String bloodType, Integer percentage) {
  }
}
