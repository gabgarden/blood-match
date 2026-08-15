package bloodmatch.application.shared;

import bloodmatch.application.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeFormats {

  public static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
  public static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private TimeFormats() {
  }

  public static String format(LocalTime time) {
    return time == null ? null : time.format(HOUR_MINUTE);
  }

  public static String format(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.withNano(0).format(LOCAL_DATE_TIME);
  }

  public static LocalTime parseOptionalHourMinute(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseHourMinute(value, fieldName);
  }

  public static LocalTime parseHourMinute(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(fieldName + " cannot be blank");
    }
    try {
      return LocalTime.parse(value.trim(), HOUR_MINUTE);
    } catch (DateTimeParseException ex) {
      try {
        return LocalTime.parse(value.trim());
      } catch (DateTimeParseException ignored) {
        throw new ValidationException(fieldName + " must be a time in HH:mm format");
      }
    }
  }
}
