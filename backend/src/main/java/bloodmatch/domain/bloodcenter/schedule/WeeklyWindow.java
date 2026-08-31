package bloodmatch.domain.bloodcenter.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record WeeklyWindow(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int slotDurationMinutes,
    int capacity) {

  public static final Set<Integer> ALLOWED_DURATIONS = Set.of(15, 20, 30, 45, 60);

  public WeeklyWindow {
    if (dayOfWeek == null) {
      throw new IllegalArgumentException("dayOfWeek cannot be null");
    }
    if (startTime == null) {
      throw new IllegalArgumentException("startTime cannot be null");
    }
    if (endTime == null) {
      throw new IllegalArgumentException("endTime cannot be null");
    }
    if (!startTime.isBefore(endTime)) {
      throw new IllegalArgumentException("startTime must be before endTime");
    }
    if (!ALLOWED_DURATIONS.contains(slotDurationMinutes)) {
      throw new IllegalArgumentException("slotDurationMinutes must be 15, 20, 30, 45 or 60");
    }
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be at least 1");
    }
  }

  public boolean overlaps(WeeklyWindow other) {
    if (other == null || dayOfWeek != other.dayOfWeek) {
      return false;
    }
    return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
  }
}
