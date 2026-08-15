package bloodmatch.interfaces.rest.bloodcenter.schedule;

import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterScheduleUseCase;
import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterSlotsUseCase;
import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterSlotsUseCase.SlotOutput;
import bloodmatch.application.usecase.bloodcenter.schedule.UpsertBloodCenterScheduleUseCase;
import bloodmatch.application.usecase.bloodcenter.schedule.UpsertBloodCenterScheduleUseCase.WindowInput;
import bloodmatch.application.usecase.bloodcenter.schedule.UpsertBloodCenterScheduleUseCase.WindowOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.authenticatedPartyId;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Blood Center Schedule", description = "Publish weekly opening windows and list available slots.")
@RequestMapping("/blood-centers")
public class BloodCenterScheduleController {

  private final UpsertBloodCenterScheduleUseCase upsertUseCase;
  private final GetBloodCenterSlotsUseCase slotsUseCase;
  private final GetBloodCenterScheduleUseCase getScheduleUseCase;

  public BloodCenterScheduleController(
      UpsertBloodCenterScheduleUseCase upsertUseCase,
      GetBloodCenterSlotsUseCase slotsUseCase,
      GetBloodCenterScheduleUseCase getScheduleUseCase) {
    this.upsertUseCase = upsertUseCase;
    this.slotsUseCase = slotsUseCase;
    this.getScheduleUseCase = getScheduleUseCase;
  }

  @GetMapping("/schedule")
  public ResponseEntity<ScheduleResponseDto> getOwn() {
    String organizationId = authenticatedPartyId();
    requireSamePartyOrAdmin(organizationId);
    return ResponseEntity.ok(ScheduleResponseDto.from(
        getScheduleUseCase.execute(new GetBloodCenterScheduleUseCase.Input(
            organizationId,
            actorPartyIdForOwnership()))));
  }

  @PutMapping("/schedule")
  public ResponseEntity<ScheduleResponseDto> upsert(@RequestBody UpsertScheduleDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNonNull(payload.weeklyWindows(), "weeklyWindows cannot be null");
    String organizationId = authenticatedPartyId();
    requireSamePartyOrAdmin(organizationId);

    UpsertBloodCenterScheduleUseCase.Output output = upsertUseCase.execute(
        new UpsertBloodCenterScheduleUseCase.Input(
            organizationId,
            payload.weeklyWindows().stream()
                .map(window -> new WindowInput(
                    window.dayOfWeek(),
                    window.startTime(),
                    window.endTime(),
                    window.slotDurationMinutes(),
                    window.capacity()))
                .toList(),
            payload.blockedDates(),
            actorPartyIdForOwnership()));

    return ResponseEntity.ok(ScheduleResponseDto.from(output));
  }

  @GetMapping("/{organizationId}/slots")
  public ResponseEntity<SlotsResponseDto> slots(
      @PathVariable String organizationId,
      @RequestParam("date") LocalDate date) {
    requireNotBlank(organizationId, "organizationId cannot be blank");
    requireNonNull(date, "date cannot be null");

    return ResponseEntity.ok(SlotsResponseDto.from(
        slotsUseCase.execute(new GetBloodCenterSlotsUseCase.Input(organizationId, date))));
  }

  @Schema(description = "Payload to publish the blood center weekly schedule.")
  public record UpsertScheduleDto(
      List<WindowDto> weeklyWindows,
      List<LocalDate> blockedDates) {
  }

  @Schema(description = "Weekly opening window.")
  public record WindowDto(
      String dayOfWeek,
      String startTime,
      String endTime,
      Integer slotDurationMinutes,
      Integer capacity) {
  }

  @Schema(description = "Saved blood center schedule.")
  public record ScheduleResponseDto(
      String organizationId,
      List<WindowOutput> weeklyWindows,
      List<LocalDate> blockedDates) {

    public static ScheduleResponseDto from(UpsertBloodCenterScheduleUseCase.Output output) {
      return new ScheduleResponseDto(
          output.organizationId(),
          output.weeklyWindows(),
          output.blockedDates());
    }
  }

  @Schema(description = "Computed appointment slots for a date.")
  public record SlotsResponseDto(
      String organizationId,
      LocalDate date,
      boolean hasSchedule,
      List<SlotOutput> slots) {

    public static SlotsResponseDto from(GetBloodCenterSlotsUseCase.Output output) {
      return new SlotsResponseDto(
          output.organizationId(),
          output.date(),
          output.hasSchedule(),
          output.slots());
    }
  }
}
