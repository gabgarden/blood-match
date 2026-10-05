package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressTest {

  @Test
  void shouldCreateAddressWithoutCoordinates() {
    Address address = new Address("Rua das Flores, 123", "São Paulo", "SP", "01234-567");

    assertEquals("Rua das Flores, 123", address.getStreet());
    assertEquals("São Paulo", address.getCity());
    assertEquals("SP", address.getState());
    assertEquals("01234-567", address.getZipCode());
    assertNull(address.getLatitude());
    assertNull(address.getLongitude());
    assertFalse(address.hasCoordinates());
  }

  @Test
  void shouldCreateAddressWithCoordinates() {
    Address address = new Address("Av. Paulista, 1000", "São Paulo", "SP", "01310-100", -23.5615, -46.6559);

    assertEquals(-23.5615, address.getLatitude());
    assertEquals(-46.6559, address.getLongitude());
    assertTrue(address.hasCoordinates());
  }

  @Test
  void shouldRejectNullMandatoryFields() {
    assertThrows(IllegalArgumentException.class, () -> new Address(null, "City", "SP", "00000-000"));
    assertThrows(IllegalArgumentException.class, () -> new Address("Street", null, "SP", "00000-000"));
    assertThrows(IllegalArgumentException.class, () -> new Address("Street", "City", null, "00000-000"));
  }

  @Test
  void shouldFormatFullAndFallbackAddresses() {
    Address withZip = new Address("Rua A, 10", "Curitiba", "PR", "80000-000");
    assertEquals("Rua A, 10, Curitiba, PR, 80000-000, Brasil", withZip.getFullAddressAsString());
    assertEquals("Rua A, 10, Curitiba, PR, Brasil", withZip.getFallbackAddressAsString());

    Address withoutZip = new Address("Rua A, 10", "Curitiba", "PR", null);
    assertEquals("Rua A, 10, Curitiba, PR, Brasil", withoutZip.getFullAddressAsString());

    Address blankZip = new Address("Rua A, 10", "Curitiba", "PR", "   ");
    assertEquals("Rua A, 10, Curitiba, PR, Brasil", blankZip.getFullAddressAsString());
  }

  @Test
  void shouldCalculateDistanceBetweenCoordinates() {
    // São Paulo center ~ (-23.5505, -46.6333)
    Address sp = new Address("Marco Zero", "São Paulo", "SP", "01000-000", -23.5505, -46.6333);
    // Rio de Janeiro center ~ (-22.9068, -43.1729)
    Address rj = new Address("Candelária", "Rio de Janeiro", "RJ", "20000-000", -22.9068, -43.1729);

    Double distance = sp.distanceTo(rj);
    // SP to RJ is roughly 350-370 km straight line
    assertTrue(distance != null && distance > 340 && distance < 380);

    // Distance to self is 0
    assertEquals(0.0, sp.distanceTo(sp), 0.001);
  }

  @Test
  void shouldReturnNullDistanceWhenCoordinatesAreMissing() {
    Address withCoords = new Address("Street", "City", "UF", "00000-000", -23.0, -46.0);
    Address withoutCoords = new Address("Street", "City", "UF", "00000-000");

    assertNull(withCoords.distanceTo(null));
    assertNull(withCoords.distanceTo(withoutCoords));
    assertNull(withoutCoords.distanceTo(withCoords));
    assertNull(withoutCoords.distanceTo(withoutCoords));
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    Address addr1 = new Address("Street", "City", "UF", "123", -20.0, -40.0);
    Address addr2 = new Address("Street", "City", "UF", "123", -20.0, -40.0);
    Address addrDiff = new Address("Other", "City", "UF", "123", -20.0, -40.0);

    assertEquals(addr1, addr1);
    assertEquals(addr1, addr2);
    assertEquals(addr1.hashCode(), addr2.hashCode());
    assertNotEquals(addr1, addrDiff);
    assertNotEquals(addr1, null);
    assertNotEquals(addr1, "some string");
  }
}
