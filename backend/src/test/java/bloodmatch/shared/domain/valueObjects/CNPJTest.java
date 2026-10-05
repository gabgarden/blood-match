package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CNPJTest {

  @Test
  void shouldCreateValidCnpjAndNormalizeDigits() {
    CNPJ cnpj = new CNPJ("12.345.678/0001-95");
    assertEquals("12345678000195", cnpj.getValue());

    CNPJ digitsOnly = new CNPJ("12345678000195");
    assertEquals("12345678000195", digitsOnly.getValue());
  }

  @Test
  void shouldRejectNullOrBlankCnpj() {
    assertThrows(IllegalArgumentException.class, () -> new CNPJ(null));
    assertThrows(IllegalArgumentException.class, () -> new CNPJ(""));
    assertThrows(IllegalArgumentException.class, () -> new CNPJ("   "));
  }

  @Test
  void shouldRejectInvalidLength() {
    assertThrows(IllegalArgumentException.class, () -> new CNPJ("1234567800019"));
    assertThrows(IllegalArgumentException.class, () -> new CNPJ("123456780001955"));
    assertThrows(IllegalArgumentException.class, () -> new CNPJ("cnpj"));
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    CNPJ cnpj1 = new CNPJ("12.345.678/0001-95");
    CNPJ cnpj2 = new CNPJ("12345678000195");
    CNPJ cnpj3 = new CNPJ("98765432000110");

    assertEquals(cnpj1, cnpj1);
    assertEquals(cnpj1, cnpj2);
    assertEquals(cnpj1.hashCode(), cnpj2.hashCode());
    assertNotEquals(cnpj1, cnpj3);
    assertNotEquals(cnpj1, null);
    assertNotEquals(cnpj1, "12345678000195");
  }
}
