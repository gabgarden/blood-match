package bloodmatch.interfaces.rest.bloodcenter.appointments;

import bloodmatch.application.usecase.bloodcenter.appointments.GetBloodCenterAppointmentsUseCase;
import bloodmatch.application.usecase.bloodcenter.appointments.GetBloodCenterAppointmentsUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.actorPartyIdForOwnership;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.authenticatedPartyId;
import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;

@RestController
@Tag(name = "Blood Center Appointments", description = "List pending donations scheduled at the authenticated blood center.")
@RequestMapping("/blood-centers")
public class BloodCenterAppointmentsController {

  private final GetBloodCenterAppointmentsUseCase useCase;

  public BloodCenterAppointmentsController(GetBloodCenterAppointmentsUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/appointments")
  public ResponseEntity<List<AppointmentResponseDto>> list(
      @RequestParam(value = "from", required = false) LocalDate from,
      @RequestParam(value = "to", required = false) LocalDate to) {
    String organizationId = authenticatedPartyId();
    requireSamePartyOrAdmin(organizationId);
    List<AppointmentResponseDto> body = useCase
        .execute(new GetBloodCenterAppointmentsUseCase.Input(
            organizationId, from, to, actorPartyIdForOwnership()))
        .stream()
        .map(AppointmentResponseDto::from)
        .toList();
    return ResponseEntity.ok(body);
  }

  @Schema(description = "Pending donation scheduled at this blood center.")
  public record AppointmentResponseDto(
      String donationId,
      String expectedDate,
      String expectedTime,
      String status,
      String donorName,
      String donorBloodType,
      String donorPhone) {

    public static AppointmentResponseDto from(OutputItem item) {
      return new AppointmentResponseDto(
          item.donationId(),
          item.expectedDate() == null ? null : item.expectedDate().toString(),
          item.expectedTime(),
          item.status(),
          item.donorName(),
          item.donorBloodType(),
          item.donorPhone());
    }
  }
}
