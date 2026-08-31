package bloodmatch.domain.bloodcenter.schedule;

import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodCenterScheduleTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 18);

  @Test
  void generatesSlotsFromWeeklyWindows() {
    BloodCenterSchedule schedule = BloodCenterSchedule.create(
        DomainID.generate(),
        List.of(window(DayOfWeek.MONDAY, "08:00", "12:00", 30, 4)),
        List.of());

    List<AppointmentSlot> slots = schedule.generateSlots(MONDAY);

    assertEquals(8, slots.size());
    assertEquals(LocalTime.of(8, 0), slots.get(0).startTime());
    assertEquals(LocalTime.of(8, 30), slots.get(0).endTime());
    assertEquals(4, slots.get(0).capacity());
    assertEquals(LocalTime.of(11, 30), slots.get(7).startTime());
    assertEquals(LocalTime.of(12, 0), slots.get(7).endTime());
    assertTrue(schedule.requiresTimeSlot(MONDAY));
  }

  @Test
  void returnsNoSlotsOnBlockedDate() {
    BloodCenterSchedule schedule = BloodCenterSchedule.create(
        DomainID.generate(),
        List.of(window(DayOfWeek.MONDAY, "08:00", "12:00", 30, 4)),
        List.of(MONDAY));

    List<AppointmentSlot> slots = schedule.generateSlots(MONDAY);

    assertTrue(slots.isEmpty());
    assertTrue(schedule.isBlocked(MONDAY));
    assertFalse(schedule.requiresTimeSlot(MONDAY));
  }

  @Test
  void returnsNoSlotsWhenWeekdayHasNoWindows() {
    BloodCenterSchedule schedule = BloodCenterSchedule.create(
        DomainID.generate(),
        List.of(window(DayOfWeek.MONDAY, "08:00", "12:00", 30, 4)),
        List.of());

    assertTrue(schedule.generateSlots(TUESDAY).isEmpty());
    assertFalse(schedule.requiresTimeSlot(TUESDAY));
  }

  @Test
  void rejectsOverlappingWindowsOnTheSameDay() {
    assertThrows(IllegalArgumentException.class, () -> BloodCenterSchedule.create(
        DomainID.generate(),
        List.of(
            window(DayOfWeek.MONDAY, "08:00", "12:00", 30, 4),
            window(DayOfWeek.MONDAY, "11:00", "14:00", 30, 2)),
        List.of()));
  }

  private static WeeklyWindow window(
      DayOfWeek dayOfWeek,
      String start,
      String end,
      int duration,
      int capacity) {
    return new WeeklyWindow(dayOfWeek, LocalTime.parse(start), LocalTime.parse(end), duration, capacity);
  }
}
