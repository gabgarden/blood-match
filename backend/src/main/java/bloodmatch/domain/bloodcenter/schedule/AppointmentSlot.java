package bloodmatch.domain.bloodcenter.schedule;

import java.time.LocalTime;

public record AppointmentSlot(
    LocalTime startTime,
    LocalTime endTime,
    int capacity,
    int booked,
    int available) {

  public static AppointmentSlot open(LocalTime startTime, LocalTime endTime, int capacity) {
    return new AppointmentSlot(startTime, endTime, capacity, 0, capacity);
  }

  public AppointmentSlot withBookings(int bookedCount) {
    int safeBooked = Math.max(0, bookedCount);
    return new AppointmentSlot(
        startTime,
        endTime,
        capacity,
        safeBooked,
        Math.max(0, capacity - safeBooked));
  }
}
