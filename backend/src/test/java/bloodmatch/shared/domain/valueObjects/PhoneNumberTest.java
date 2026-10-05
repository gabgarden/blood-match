package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberTest {

  @Test
  void shouldCreateValidPhoneNumber() {
    PhoneNumber phone10 = new PhoneNumber("1133334444");
    assertEquals("1133334444", phone10.getValue());

    PhoneNumber phone11 = new PhoneNumber("11988887777");
    assertEquals("11988887777", phone11.getValue());

    PhoneNumber phone13 = new PhoneNumber("5511988887777");
    assertEquals("5511988887777", phone13.getValue());
  }

  @Test
  void shouldRejectNullOrBlankPhoneNumber() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(null));
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(""));
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("   "));
  }

  @Test
  void shouldRejectInvalidFormat() {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("123456789")); // 9 digits
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("12345678901234")); // 14 digits
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("(11) 98888-7777")); // has non-digits
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    PhoneNumber p1 = new PhoneNumber("11988887777");
    PhoneNumber p2 = new PhoneNumber("11988887777");
    PhoneNumber p3 = new PhoneNumber("1133334444");

    assertEquals(p1, p1);
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
    assertNotEquals(p1, p3);
    assertNotEquals(p1, null);
    assertNotEquals(p1, "11988887777");
  }
}
