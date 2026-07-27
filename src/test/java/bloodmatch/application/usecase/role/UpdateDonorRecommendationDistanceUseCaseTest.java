package bloodmatch.application.usecase.role;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateDonorRecommendationDistanceUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final UpdateDonorRecommendationDistanceUseCase useCase =
      new UpdateDonorRecommendationDistanceUseCase(donorRepository);

  @Test
  void shouldUseThirtyKilometersAsTheDefaultRecommendationDistance() {
    Donor donor = donor();

    assertEquals(30.0, donor.getMaxRecommendationDistanceKm());
  }

  @Test
  void shouldUpdateTheMaximumRecommendationDistance() {
    Donor donor = donor();
    DomainID personId = donor.getPerson().getId();
    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.of(donor));

    Donor updatedDonor = useCase.execute(personId, 45.5);

    assertEquals(45.5, updatedDonor.getMaxRecommendationDistanceKm());
    verify(donorRepository).save(donor);
  }

  @Test
  void shouldRejectANonPositiveRecommendationDistance() {
    Donor donor = donor();
    DomainID personId = donor.getPerson().getId();
    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.of(donor));

    assertThrows(IllegalArgumentException.class, () -> useCase.execute(personId, 0));
  }

  private Donor donor() {
    return new Donor(
        new Person("Donor", new CPF("12345678901"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O+"),
        70);
  }
}
