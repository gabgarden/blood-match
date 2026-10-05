package bloodmatch.party.application;

import bloodmatch.party.domain.Party;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.party.domain.Person;
import bloodmatch.shared.application.exception.ConcurrencyException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.services.GeocodingServiceInterface;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdatePartyUseCaseTest {

  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);
  private final GeocodingServiceInterface geocodingService = mock(GeocodingServiceInterface.class);
  private final UpdatePartyUseCase useCase = new UpdatePartyUseCase(partyRepository, geocodingService);

  @Test
  void shouldUpdateNamePhoneAndAddressSuccessfully() {
    Person person = person();
    person.setVersion(1L);
    DomainID partyId = person.getId();

    when(partyRepository.findById(partyId)).thenReturn(Optional.of(person));
    Address geocoded = new Address("Rua Nova, 50", "Curitiba", "PR", "80000-000", -25.4, -49.2);
    when(geocodingService.getCoordinatesFromAddress(any())).thenReturn(geocoded);

    var input = new UpdatePartyUseCase.Input(
        partyId.getValue().toString(),
        1L,
        "Nome Atualizado",
        "11911112222",
        new UpdatePartyUseCase.AddressInput("Rua Nova, 50", "Curitiba", "PR", "80000-000"));

    var output = useCase.execute(input);

    assertEquals(partyId.getValue().toString(), output.id());
    assertEquals("Nome Atualizado", output.name());
    assertEquals("11911112222", output.phoneNumber());
    assertNotNull(output.address());
    assertEquals("Rua Nova, 50", output.address().street());
    verify(partyRepository).save(person);
  }

  @Test
  void shouldUpdateWithoutAddressAndRetainNullAddressOutput() {
    Person person = person();
    person.setVersion(1L);
    DomainID partyId = person.getId();

    when(partyRepository.findById(partyId)).thenReturn(Optional.of(person));

    var input = new UpdatePartyUseCase.Input(
        partyId.getValue().toString(),
        1L,
        "Apenas Nome",
        null,
        null);

    var output = useCase.execute(input);
    assertEquals("Apenas Nome", output.name());
    assertNull(output.address());
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new UpdatePartyUseCase(null, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new UpdatePartyUseCase(partyRepository, null));
  }

  @Test
  void shouldValidateInputs() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));

    // Null version
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdatePartyUseCase.Input("some-id", null, "Nome", null, null)));

    // No fields provided
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdatePartyUseCase.Input("some-id", 1L, "", null, null)));

    // Invalid partyId
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdatePartyUseCase.Input("not-uuid", 1L, "Nome", null, null)));
  }

  @Test
  void shouldThrowNotFoundWhenPartyMissing() {
    DomainID partyId = DomainID.generate();
    when(partyRepository.findById(partyId)).thenReturn(Optional.empty());

    var input = new UpdatePartyUseCase.Input(partyId.getValue().toString(), 1L, "Nome", null, null);
    assertThrows(NotFoundException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldThrowConcurrencyExceptionOnVersionConflict() {
    Person person = person();
    person.setVersion(1L);
    DomainID partyId = person.getId();
    when(partyRepository.findById(partyId)).thenReturn(Optional.of(person));

    // Client passed version 2L, entity has 1L
    var input = new UpdatePartyUseCase.Input(partyId.getValue().toString(), 2L, "Nome", null, null);
    assertThrows(ConcurrencyException.class, () -> useCase.execute(input));
  }

  private Person person() {
    return new Person("Original", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1));
  }
}
