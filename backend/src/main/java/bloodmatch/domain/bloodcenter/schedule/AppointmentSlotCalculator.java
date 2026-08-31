package bloodmatch.domain.bloodcenter.schedule;

import bloodmatch.domain.donation.Donation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AppointmentSlotCalculator {

  private AppointmentSlotCalculator() {
  }

  public static List<AppointmentSlot> calculate(
      BloodCenterSchedule schedule,
      LocalDate date,
      List<Donation> pendingDonations) {
    if (schedule == null) {
      throw new IllegalArgumentException("schedule cannot be null");
    }
    if (date == null) {
      throw new IllegalArgumentException("date cannot be null");
    }

    Map<LocalTime, Integer> bookedByStart = countBookings(pendingDonations, date);
    return schedule.generateSlots(date).stream()
        .map(slot -> slot.withBookings(bookedByStart.getOrDefault(slot.startTime(), 0)))
        .toList();
  }

  private static Map<LocalTime, Integer> countBookings(List<Donation> pendingDonations, LocalDate date) {
    Map<LocalTime, Integer> bookedByStart = new HashMap<>();
    if (pendingDonations == null) {
      return bookedByStart;
    }
    for (Donation donation : pendingDonations) {
      if (donation == null || !donation.isPending()) {
        continue;
      }
      if (donation.getExpectedTime() == null) {
        continue;
      }
      if (date != null && donation.getIntendedDate() != null && !date.equals(donation.getIntendedDate())) {
        continue;
      }
      bookedByStart.merge(donation.getExpectedTime(), 1, Integer::sum);
    }
    return bookedByStart;
  }
}
