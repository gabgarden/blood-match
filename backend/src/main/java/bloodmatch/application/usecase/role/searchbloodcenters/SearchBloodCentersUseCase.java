package bloodmatch.application.usecase.role.searchbloodcenters;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterDirectoryEntry;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchBloodCentersUseCase {

  public static final int DEFAULT_LIMIT = 10;
  public static final int MAX_LIMIT = 20;
  public static final int MIN_QUERY_LENGTH = 2;

  private final BloodCenterRepositoryInterface bloodCenterRepository;

  public SearchBloodCentersUseCase(BloodCenterRepositoryInterface bloodCenterRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
  }

  public List<OutputItem> execute(Input input) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }

    String query = input.query() == null ? "" : input.query().trim();
    if (query.length() < MIN_QUERY_LENGTH) {
      throw new ValidationException("q must have at least " + MIN_QUERY_LENGTH + " characters");
    }

    int limit = input.limit() == null ? DEFAULT_LIMIT : input.limit();
    if (limit < 1) {
      throw new ValidationException("limit must be at least 1");
    }
    if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }

    return bloodCenterRepository.searchByOrganizationName(query, limit).stream()
        .map(this::toOutput)
        .toList();
  }

  private OutputItem toOutput(BloodCenterDirectoryEntry entry) {
    return new OutputItem(
        entry.organizationId(),
        entry.name(),
        entry.city(),
        entry.state());
  }

  public record Input(String query, Integer limit) {
  }

  public record OutputItem(
      String organizationId,
      String name,
      String city,
      String state) {
  }
}
