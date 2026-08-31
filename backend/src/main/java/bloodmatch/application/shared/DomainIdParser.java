package bloodmatch.application.shared;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.domain.shared.valueObjects.DomainID;

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
