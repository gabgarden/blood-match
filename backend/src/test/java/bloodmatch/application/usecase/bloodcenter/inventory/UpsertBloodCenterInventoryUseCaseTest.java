package bloodmatch.application.usecase.bloodcenter.inventory;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.usecase.bloodcenter.inventory.UpsertBloodCenterInventoryUseCase.Input;
import bloodmatch.application.usecase.bloodcenter.inventory.UpsertBloodCenterInventoryUseCase.ItemInput;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpsertBloodCenterInventoryUseCaseTest {

  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final BloodCenterInventoryRepositoryInterface inventoryRepository =
      mock(BloodCenterInventoryRepositoryInterface.class);
  private final UpsertBloodCenterInventoryUseCase useCase = new UpsertBloodCenterInventoryUseCase(
      bloodCenterRepository,
      inventoryRepository);

  @Test
  void fillsMissingBloodTypesWithZeroAndClampsPercentage() {
    BloodCenter bloodCenter = bloodCenter();
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(inventoryRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.empty());

    LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 15, 10, 0);
    String organizationId = bloodCenter.getOrganization().getId().getValue().toString();
    BloodCenterInventoryOutput output = useCase.execute(
        new Input(organizationId, List.of(new ItemInput("O+", 140)), organizationId),
        updatedAt);

    ArgumentCaptor<BloodCenterInventory> captor = ArgumentCaptor.forClass(BloodCenterInventory.class);
    verify(inventoryRepository).save(captor.capture());
    assertEquals(8, captor.getValue().normalizedLevels().size());
    assertEquals(8, output.items().size());
    assertEquals("O+", output.items().get(6).bloodType());
    assertEquals(100, output.items().get(6).percentage());
    assertEquals("Adequado", output.items().get(6).label());
    assertEquals(0, output.items().get(0).percentage());
    assertEquals("Crítico", output.items().get(0).label());
    assertEquals(updatedAt, output.updatedAt());
  }

  @Test
  void shouldReturnNotFoundWhenBloodCenterDoesNotExist() {
    when(bloodCenterRepository.findByPartyId(any())).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> useCase.execute(
        new Input("550e8400-e29b-41d4-a716-446655440000", List.of(new ItemInput("O+", 40)),
            "550e8400-e29b-41d4-a716-446655440000")));
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(
        new Organization("Hemocentro Regional", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
