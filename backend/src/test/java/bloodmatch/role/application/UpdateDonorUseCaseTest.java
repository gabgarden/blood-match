package bloodmatch.role.application;

import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.shared.application.exception.ConcurrencyException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateDonorUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final UpdateDonorUseCase useCase = new UpdateDonorUseCase(donorRepository);

  @Test
  void shouldUpdateDonorSuccessfully() {
    Donor donor = donor();
    DomainID personId = donor.getPerson().getId();

    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.of(donor));

    var input = new UpdateDonorUseCase.Input(
        personId.getValue().toString(),
        1L,
        "A+",
        75.5,
        40.0);

    var output = useCase.execute(input);

    assertEquals(personId.getValue().toString(), output.personId());
    assertEquals("A+", output.bloodType());
    assertEquals(75.5, output.weight());
    assertEquals(40.0, output.maxDistanceInKm());
    verify(donorRepository).save(donor);
  }

  @Test
  void shouldRejectNullConstructorArg() {
    assertThrows(IllegalArgumentException.class, () -> new UpdateDonorUseCase(null));
  }

  @Test
  void shouldValidateInputFields() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));

    // Null version
    var missingVersion = new UpdateDonorUseCase.Input("some-id", null, "A+", 70.0, 30.0);
    assertThrows(ValidationException.class, () -> useCase.execute(missingVersion));

    // No mutable fields provided
    var noFields = new UpdateDonorUseCase.Input("some-id", 1L, null, null, null);
    assertThrows(ValidationException.class, () -> useCase.execute(noFields));

    // Invalid personId
    var invalidId = new UpdateDonorUseCase.Input("not-uuid", 1L, "A+", null, null);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidId));
  }

  @Test
  void shouldThrowNotFoundWhenDonorMissing() {
    DomainID personId = DomainID.generate();
    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.empty());

    var input = new UpdateDonorUseCase.Input(personId.getValue().toString(), 1L, "A+", null, null);
    assertThrows(NotFoundException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldThrowConcurrencyExceptionOnVersionConflict() {
    Donor donor = donor(); // version is 1L
    DomainID personId = donor.getPerson().getId();
    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.of(donor));

    // Request sends version 2L instead of 1L
    var input = new UpdateDonorUseCase.Input(personId.getValue().toString(), 2L, "A+", null, null);
    assertThrows(ConcurrencyException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldValidateBusinessRulesDuringUpdate() {
    Donor donor = donor();
    DomainID personId = donor.getPerson().getId();
    when(donorRepository.findByPartyId(personId)).thenReturn(Optional.of(donor));

    // Invalid blood type
    var invalidBloodType = new UpdateDonorUseCase.Input(personId.getValue().toString(), 1L, "INVALID", null, null);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidBloodType));

    // Invalid weight (< 50)
    var invalidWeight = new UpdateDonorUseCase.Input(personId.getValue().toString(), 1L, null, 45.0, null);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidWeight));

    // Invalid maxDistance (<= 0)
    var invalidDist = new UpdateDonorUseCase.Input(personId.getValue().toString(), 1L, null, null, 0.0);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidDist));
  }

  private Donor donor() {
    Person person = new Person("Doador", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1));
    return Donor.reconstitute(
        person,
        BloodType.of("O+"),
        70.0,
        null,
        30.0,
        DomainID.generate(),
        LocalDate.now(),
        1L);
  }
}
