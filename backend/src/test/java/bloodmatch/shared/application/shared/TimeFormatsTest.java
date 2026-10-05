package bloodmatch.shared.application.shared;

import bloodmatch.shared.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeFormatsTest {

  @Test
  void shouldFormatLocalTime() {
    assertNull(TimeFormats.format((LocalTime) null));
    assertEquals("08:30", TimeFormats.format(LocalTime.of(8, 30)));
    assertEquals("14:05", TimeFormats.format(LocalTime.of(14, 5, 23)));
  }

  @Test
  void shouldFormatLocalDateTime() {
    assertNull(TimeFormats.format((LocalDateTime) null));
    LocalDateTime dt = LocalDateTime.of(2026, 4, 1, 15, 30, 45, 123456);
    assertEquals("2026-04-01T15:30:45", TimeFormats.format(dt));
  }

  @Test
  void shouldParseOptionalHourMinute() {
    assertNull(TimeFormats.parseOptionalHourMinute(null, "time"));
    assertNull(TimeFormats.parseOptionalHourMinute("", "time"));
    assertNull(TimeFormats.parseOptionalHourMinute("   ", "time"));
    assertEquals(LocalTime.of(9, 15), TimeFormats.parseOptionalHourMinute("09:15", "time"));
  }

  @Test
  void shouldParseHourMinute() {
    assertEquals(LocalTime.of(10, 45), TimeFormats.parseHourMinute("10:45", "time"));
    assertEquals(LocalTime.of(10, 45), TimeFormats.parseHourMinute(" 10:45 ", "time"));
    assertEquals(LocalTime.of(10, 45, 30), TimeFormats.parseHourMinute("10:45:30", "time"));

    ValidationException blankEx = assertThrows(ValidationException.class, () -> TimeFormats.parseHourMinute("  ", "time"));
    assertEquals("time cannot be blank", blankEx.getMessage());

    ValidationException invalidEx = assertThrows(ValidationException.class, () -> TimeFormats.parseHourMinute("invalid", "time"));
    assertEquals("time must be a time in HH:mm format", invalidEx.getMessage());
  }
}
