package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CPFTest {

  @Test
  void shouldCreateValidCpfAndNormalizeDigits() {
    CPF cpf = new CPF("123.456.789-01");
    assertEquals("12345678901", cpf.getValue());

    CPF digitsOnly = new CPF("12345678901");
    assertEquals("12345678901", digitsOnly.getValue());
  }

  @Test
  void shouldRejectNullOrBlankCpf() {
    assertThrows(IllegalArgumentException.class, () -> new CPF(null));
    assertThrows(IllegalArgumentException.class, () -> new CPF(""));
    assertThrows(IllegalArgumentException.class, () -> new CPF("   "));
  }

  @Test
  void shouldRejectInvalidLength() {
    assertThrows(IllegalArgumentException.class, () -> new CPF("1234567890"));
    assertThrows(IllegalArgumentException.class, () -> new CPF("123456789012"));
    assertThrows(IllegalArgumentException.class, () -> new CPF("abc"));
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    CPF cpf1 = new CPF("123.456.789-01");
    CPF cpf2 = new CPF("12345678901");
    CPF cpf3 = new CPF("98765432100");

    assertEquals(cpf1, cpf1);
    assertEquals(cpf1, cpf2);
    assertEquals(cpf1.hashCode(), cpf2.hashCode());
    assertNotEquals(cpf1, cpf3);
    assertNotEquals(cpf1, null);
    assertNotEquals(cpf1, "12345678901");
  }
}
