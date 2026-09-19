package bloodmatch.shared.application.shared;

import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.UUID;

public final class DomainIdParser {

  private DomainIdParser() {
  }

  public static DomainID parse(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(fieldName + " cannot be blank");
    }
    try {
      return new DomainID(UUID.fromString(value.trim()));
    } catch (IllegalArgumentException e) {
      throw new ValidationException(fieldName + " must be a valid UUID");
    }
  }
}
