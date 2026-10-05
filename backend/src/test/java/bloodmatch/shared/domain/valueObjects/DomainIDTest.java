package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainIDTest {

  @Test
  void shouldGenerateRandomDomainId() {
    DomainID id1 = new DomainID();
    DomainID id2 = DomainID.generate();

    assertNotNull(id1.getValue());
    assertNotNull(id2.getValue());
    assertNotEquals(id1, id2);
  }

  @Test
  void shouldCreateFromExistingUuid() {
    UUID uuid = UUID.randomUUID();
    DomainID domainID = new DomainID(uuid);

    assertEquals(uuid, domainID.getValue());
  }

  @Test
  void shouldRejectNullUuid() {
    assertThrows(IllegalArgumentException.class, () -> new DomainID(null));
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    UUID uuid = UUID.randomUUID();
    DomainID id1 = new DomainID(uuid);
    DomainID id2 = new DomainID(uuid);
    DomainID id3 = DomainID.generate();

    assertEquals(id1, id1);
    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
    assertNotEquals(id1, id3);
    assertNotEquals(id1, null);
    assertNotEquals(id1, "string");
  }
}
