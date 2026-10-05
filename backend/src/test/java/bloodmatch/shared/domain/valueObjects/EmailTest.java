package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

  @Test
  void shouldCreateValidEmailAndNormalizeToLowerCase() {
    Email email = new Email("User@Example.COM");
    assertEquals("user@example.com", email.getValue());
  }

  @Test
  void shouldRejectNullOrBlankEmail() {
    assertThrows(IllegalArgumentException.class, () -> new Email(null));
    assertThrows(IllegalArgumentException.class, () -> new Email(""));
    assertThrows(IllegalArgumentException.class, () -> new Email("   "));
  }

  @Test
  void shouldRejectInvalidEmailFormat() {
    assertThrows(IllegalArgumentException.class, () -> new Email("plainaddress"));
    assertThrows(IllegalArgumentException.class, () -> new Email("@missingusername.com"));
    assertThrows(IllegalArgumentException.class, () -> new Email("missingdomain@"));
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    Email email1 = new Email("test@domain.com");
    Email email2 = new Email("TEST@DOMAIN.COM");
    Email email3 = new Email("other@domain.com");

    assertEquals(email1, email1);
    assertEquals(email1, email2);
    assertEquals(email1.hashCode(), email2.hashCode());
    assertNotEquals(email1, email3);
    assertNotEquals(email1, null);
    assertNotEquals(email1, "test@domain.com");
  }
}
