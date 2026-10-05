package bloodmatch.shared.domain.entity;

import bloodmatch.shared.domain.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainObjectTest {

  static class ConcreteDomainObject extends DomainObject {
    void assignId(DomainID id) {
      setId(id);
    }
  }

  @Test
  void shouldSetAndGetIdAndVersion() {
    ConcreteDomainObject obj = new ConcreteDomainObject();
    assertNull(obj.getId());
    assertNull(obj.getVersion());

    DomainID id = DomainID.generate();
    obj.assignId(id);
    assertEquals(id, obj.getId());

    obj.setVersion(2L);
    assertEquals(2L, obj.getVersion());
  }

  @Test
  void shouldRejectNullId() {
    ConcreteDomainObject obj = new ConcreteDomainObject();
    assertThrows(IllegalArgumentException.class, () -> obj.assignId(null));
  }
}
