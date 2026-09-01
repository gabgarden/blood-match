package bloodmatch.domain.bloodcenter.schedule;

import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BloodCenterSchedule extends DomainObject {

  private DomainID organizationId;
  private List<WeeklyWindow> weeklyWindows;
  private List<LocalDate> blockedDates;

  private BloodCenterSchedule() {
  }

  public static BloodCenterSchedule create(
      DomainID organizationId,
      List<WeeklyWindow> weeklyWindows,
      List<LocalDate> blockedDates) {
    BloodCenterSchedule schedule = new BloodCenterSchedule();
    schedule.setId(requireOrganizationId(organizationId));
    schedule.organizationId = organizationId;
    schedule.weeklyWindows = requireWindows(weeklyWindows);
    schedule.blockedDates = copyBlockedDates(blockedDates);
    return schedule;
  }

  public static BloodCenterSchedule reconstitute(
      DomainID organizationId,
      List<WeeklyWindow> weeklyWindows,
      List<LocalDate> blockedDates) {
    return reconstitute(organizationId, weeklyWindows, blockedDates, null);
  }

  public static BloodCenterSchedule reconstitute(
      DomainID organizationId,
      List<WeeklyWindow> weeklyWindows,
      List<LocalDate> blockedDates,
      Long version) {
    BloodCenterSchedule schedule = new BloodCenterSchedule();
    schedule.setId(requireOrganizationId(organizationId));
    schedule.organizationId = organizationId;
    schedule.weeklyWindows = weeklyWindows == null ? List.of() : List.copyOf(weeklyWindows);
    schedule.blockedDates = copyBlockedDates(blockedDates);
    schedule.setVersion(version);
    return schedule;
  }

  public void replace(
      List<WeeklyWindow> weeklyWindows,
      List<LocalDate> blockedDates) {
    this.weeklyWindows = requireWindows(weeklyWindows);
    this.blockedDates = copyBlockedDates(blockedDates);
  }

  public DomainID getOrganizationId() {
    return organizationId;
  }

  public List<WeeklyWindow> getWeeklyWindows() {
    return weeklyWindows == null ? List.of() : List.copyOf(weeklyWindows);
  }

  public List<LocalDate> getBlockedDates() {
    return blockedDates == null ? List.of() : List.copyOf(blockedDates);
  }

  public boolean isBlocked(LocalDate date) {
    if (date == null) {
      return false;
    }
    return getBlockedDates().contains(date);
  }

  public boolean hasWindowsOn(DayOfWeek dayOfWeek) {
    if (dayOfWeek == null) {
      return false;
    }
    return getWeeklyWindows().stream().anyMatch(window -> window.dayOfWeek() == dayOfWeek);
  }

  public boolean requiresTimeSlot(LocalDate date) {
    return date != null && !isBlocked(date) && hasWindowsOn(date.getDayOfWeek());
  }

  public List<AppointmentSlot> generateSlots(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("date cannot be null");
    }
    if (isBlocked(date) || !hasWindowsOn(date.getDayOfWeek())) {
      return List.of();
    }

    List<AppointmentSlot> slots = new ArrayList<>();
    for (WeeklyWindow window : getWeeklyWindows()) {
      if (window.dayOfWeek() != date.getDayOfWeek()) {
        continue;
      }
      LocalTime cursor = window.startTime();
      while (Duration.between(cursor, window.endTime()).toMinutes() >= window.slotDurationMinutes()) {
        LocalTime end = cursor.plusMinutes(window.slotDurationMinutes());
        slots.add(AppointmentSlot.open(cursor, end, window.capacity()));
        cursor = end;
      }
    }
    slots.sort(Comparator.comparing(AppointmentSlot::startTime));
    return List.copyOf(slots);
  }

  private static DomainID requireOrganizationId(DomainID organizationId) {
    if (organizationId == null) {
      throw new IllegalArgumentException("organizationId cannot be null");
    }
    return organizationId;
  }

  private static List<WeeklyWindow> requireWindows(List<WeeklyWindow> weeklyWindows) {
    if (weeklyWindows == null) {
      throw new IllegalArgumentException("weeklyWindows cannot be null");
    }
    List<WeeklyWindow> copy = new ArrayList<>();
    for (WeeklyWindow window : weeklyWindows) {
      if (window == null) {
        throw new IllegalArgumentException("weeklyWindows cannot contain null");
      }
      copy.add(window);
    }
    for (int i = 0; i < copy.size(); i++) {
      for (int j = i + 1; j < copy.size(); j++) {
        if (copy.get(i).overlaps(copy.get(j))) {
          throw new IllegalArgumentException("weeklyWindows cannot overlap on the same day");
        }
      }
    }
    return List.copyOf(copy);
  }

  private static List<LocalDate> copyBlockedDates(List<LocalDate> blockedDates) {
    if (blockedDates == null || blockedDates.isEmpty()) {
      return List.of();
    }
    Set<LocalDate> unique = new HashSet<>();
    for (LocalDate date : blockedDates) {
      if (date == null) {
        throw new IllegalArgumentException("blockedDates cannot contain null");
      }
      unique.add(date);
    }
    return unique.stream().sorted().toList();
  }
}
