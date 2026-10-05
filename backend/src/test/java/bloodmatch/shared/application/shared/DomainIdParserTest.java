package bloodmatch.shared.application.shared;

import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainIdParserTest {

  @Test
  void shouldParseValidUuid() {
    UUID uuid = UUID.randomUUID();
    DomainID parsed = DomainIdParser.parse(uuid.toString(), "id");
    assertEquals(uuid, parsed.getValue());

    DomainID parsedWithSpaces = DomainIdParser.parse("  " + uuid + "  ", "id");
    assertEquals(uuid, parsedWithSpaces.getValue());
  }

  @Test
  void shouldThrowWhenNullOrBlank() {
    ValidationException nullEx = assertThrows(ValidationException.class, () -> DomainIdParser.parse(null, "donorId"));
    assertEquals("donorId cannot be blank", nullEx.getMessage());

    ValidationException blankEx = assertThrows(ValidationException.class, () -> DomainIdParser.parse("   ", "donorId"));
    assertEquals("donorId cannot be blank", blankEx.getMessage());
  }

  @Test
  void shouldThrowWhenInvalidUuid() {
    ValidationException invalidEx = assertThrows(ValidationException.class, () -> DomainIdParser.parse("not-a-uuid", "donorId"));
    assertEquals("donorId must be a valid UUID", invalidEx.getMessage());
  }
}
