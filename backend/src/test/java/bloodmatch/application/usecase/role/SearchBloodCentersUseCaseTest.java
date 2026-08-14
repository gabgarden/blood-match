package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.role.searchbloodcenters.SearchBloodCentersUseCase;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterDirectoryEntry;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchBloodCentersUseCaseTest {

  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final SearchBloodCentersUseCase useCase = new SearchBloodCentersUseCase(bloodCenterRepository);

  @Test
  void shouldRejectBlankOrShortQuery() {
    assertThrows(ValidationException.class,
        () -> useCase.execute(new SearchBloodCentersUseCase.Input(" ", 10)));
    assertThrows(ValidationException.class,
        () -> useCase.execute(new SearchBloodCentersUseCase.Input("a", 10)));
  }

  @Test
  void shouldClampLimitAndReturnMappedResults() {
    when(bloodCenterRepository.searchByOrganizationName(eq("Hemo"), eq(20)))
        .thenReturn(List.of(
            new BloodCenterDirectoryEntry("org-1", "Hemocentro Regional", "Campos", "RJ")));

    List<SearchBloodCentersUseCase.OutputItem> result = useCase.execute(
        new SearchBloodCentersUseCase.Input("Hemo", 50));

    assertEquals(1, result.size());
    assertEquals("org-1", result.get(0).organizationId());
    assertEquals("Hemocentro Regional", result.get(0).name());
    assertEquals("Campos", result.get(0).city());
    assertEquals("RJ", result.get(0).state());
    verify(bloodCenterRepository).searchByOrganizationName("Hemo", 20);
  }

  @Test
  void shouldUseDefaultLimitWhenNull() {
    when(bloodCenterRepository.searchByOrganizationName(eq("Hospital"), eq(10)))
        .thenReturn(List.of());

    useCase.execute(new SearchBloodCentersUseCase.Input("Hospital", null));

    verify(bloodCenterRepository).searchByOrganizationName("Hospital", 10);
  }
}
