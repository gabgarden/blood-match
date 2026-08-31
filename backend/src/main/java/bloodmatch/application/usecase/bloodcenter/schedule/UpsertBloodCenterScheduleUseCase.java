package bloodmatch.application.usecase.bloodcenter.schedule;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.application.shared.TimeFormats;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.bloodcenter.schedule.WeeklyWindow;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UpsertBloodCenterScheduleUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterScheduleRepositoryInterface scheduleRepository;

  public UpsertBloodCenterScheduleUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterScheduleRepositoryInterface scheduleRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.scheduleRepository = scheduleRepository;
  }

  public Output execute(Input input) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    PartyOwnership.requireSameParty(organizationId, input.actorPartyId());
    bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    List<WeeklyWindow> windows = parseWindows(input.weeklyWindows());
    List<LocalDate> blockedDates = parseBlockedDates(input.blockedDates());

    BloodCenterSchedule schedule = scheduleRepository.findByOrganizationId(organizationId)
        .orElse(null);
    try {
      if (schedule == null) {
        schedule = BloodCenterSchedule.create(organizationId, windows, blockedDates);
      } else {
        schedule.replace(windows, blockedDates);
      }
    } catch (IllegalArgumentException ex) {
      throw new ValidationException(ex.getMessage());
    }

    scheduleRepository.save(schedule);
    return Output.from(schedule);
  }

  private static List<WeeklyWindow> parseWindows(List<WindowInput> windowInputs) {
    if (windowInputs == null) {
      throw new ValidationException("weeklyWindows cannot be null");
    }

    List<WeeklyWindow> windows = new ArrayList<>();
    for (WindowInput windowInput : windowInputs) {
      if (windowInput == null) {
        throw new ValidationException("weeklyWindows cannot contain null");
      }
      DayOfWeek dayOfWeek = parseDayOfWeek(windowInput.dayOfWeek());
      LocalTime startTime = parseTime(windowInput.startTime(), "startTime");
      LocalTime endTime = parseTime(windowInput.endTime(), "endTime");
      if (windowInput.slotDurationMinutes() == null
          || !WeeklyWindow.ALLOWED_DURATIONS.contains(windowInput.slotDurationMinutes())) {
        throw new ValidationException("slotDurationMinutes must be 15, 20, 30, 45 or 60");
      }
      if (windowInput.capacity() == null || windowInput.capacity() < 1) {
        throw new ValidationException("capacity must be at least 1");
      }
      if (!startTime.isBefore(endTime)) {
        throw new ValidationException("startTime must be before endTime");
      }
      WeeklyWindow window;
      try {
        window = new WeeklyWindow(
            dayOfWeek,
            startTime,
            endTime,
            windowInput.slotDurationMinutes(),
            windowInput.capacity());
      } catch (IllegalArgumentException ex) {
        throw new ValidationException(ex.getMessage());
      }
      for (WeeklyWindow existing : windows) {
        if (existing.overlaps(window)) {
          throw new ValidationException("weeklyWindows cannot overlap on the same day");
        }
      }
      windows.add(window);
    }
    return windows;
  }

  private static List<LocalDate> parseBlockedDates(List<LocalDate> blockedDates) {
    if (blockedDates == null) {
      return List.of();
    }
    for (LocalDate date : blockedDates) {
      if (date == null) {
        throw new ValidationException("blockedDates cannot contain null");
      }
    }
    return blockedDates;
  }

  private static DayOfWeek parseDayOfWeek(String value) {
    if (value == null || value.isBlank()) {
      throw new ValidationException("dayOfWeek cannot be blank");
    }
    try {
      return DayOfWeek.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("dayOfWeek must be MONDAY through SUNDAY");
    }
  }

  private static LocalTime parseTime(String value, String fieldName) {
    return TimeFormats.parseHourMinute(value, fieldName);
  }

  public record Input(
      String organizationId,
      List<WindowInput> weeklyWindows,
      List<LocalDate> blockedDates,
      String actorPartyId) {
  }

  public record WindowInput(
      String dayOfWeek,
      String startTime,
      String endTime,
      Integer slotDurationMinutes,
      Integer capacity) {
  }

  public record Output(
      String organizationId,
      List<WindowOutput> weeklyWindows,
      List<LocalDate> blockedDates) {

    public static Output from(BloodCenterSchedule schedule) {
      return new Output(
          schedule.getOrganizationId().getValue().toString(),
          schedule.getWeeklyWindows().stream().map(WindowOutput::from).toList(),
          schedule.getBlockedDates());
    }
  }

  public record WindowOutput(
      String dayOfWeek,
      String startTime,
      String endTime,
      int slotDurationMinutes,
      int capacity) {

    public static WindowOutput from(WeeklyWindow window) {
      return new WindowOutput(
          window.dayOfWeek().name(),
          TimeFormats.format(window.startTime()),
          TimeFormats.format(window.endTime()),
          window.slotDurationMinutes(),
          window.capacity());
    }
  }
}
