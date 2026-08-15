package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.WeeklyWindow;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "blood_center_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloodCenterScheduleSchema {

  @Id
  private String organizationId;
  private List<WeeklyWindowDocument> weeklyWindows;
  private List<LocalDate> blockedDates;

  public BloodCenterScheduleSchema(BloodCenterSchedule schedule) {
    if (schedule == null) {
      throw new IllegalArgumentException("Schedule cannot be null");
    }
    this.organizationId = schedule.getOrganizationId().getValue().toString();
    this.weeklyWindows = schedule.getWeeklyWindows().stream()
        .map(window -> new WeeklyWindowDocument(
            window.dayOfWeek().name(),
            window.startTime(),
            window.endTime(),
            window.slotDurationMinutes(),
            window.capacity()))
        .toList();
    this.blockedDates = schedule.getBlockedDates();
  }

  public BloodCenterSchedule toDomain() {
    DomainID orgId = new DomainID(UUID.fromString(this.organizationId));
    List<WeeklyWindow> windows = new ArrayList<>();
    if (this.weeklyWindows != null) {
      for (WeeklyWindowDocument window : this.weeklyWindows) {
        windows.add(new WeeklyWindow(
            DayOfWeek.valueOf(window.dayOfWeek),
            window.startTime,
            window.endTime,
            window.slotDurationMinutes,
            window.capacity));
      }
    }
    return BloodCenterSchedule.reconstitute(orgId, windows, this.blockedDates);
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WeeklyWindowDocument {
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes;
    private int capacity;
  }
}
