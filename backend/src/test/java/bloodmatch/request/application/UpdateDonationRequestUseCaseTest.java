package bloodmatch.request.application;

import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.request.domain.Urgency;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.application.exception.ConcurrencyException;
import bloodmatch.shared.application.exception.ForbiddenException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
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

class UpdateDonationRequestUseCaseTest {

  private final DonationRequestRepositoryInterface repository = mock(DonationRequestRepositoryInterface.class);
  private final UpdateDonationRequestUseCase useCase = new UpdateDonationRequestUseCase(repository);

  @Test
  void shouldUpdateGoalAndDateLimitSuccessfully() {
    DonationRequest request = request();
    request.setVersion(1L);
    DomainID requestId = request.getId();
    String actorId = request.getRequester().getParty().getId().getValue().toString();

    when(repository.findById(requestId)).thenReturn(Optional.of(request));

    LocalDate newLimit = LocalDate.now().plusDays(20);
    var input = new UpdateDonationRequestUseCase.Input(
        requestId.getValue().toString(),
        1L,
        15,
        newLimit,
        actorId);

    var output = useCase.execute(input);

    assertEquals(requestId.getValue().toString(), output.id());
    assertEquals(15, output.goalBloodBags());
    assertEquals(newLimit, output.dateLimit());
    assertEquals(1L, output.version());
    verify(repository).save(request);
  }

  @Test
  void shouldRejectNullConstructorArg() {
    assertThrows(IllegalArgumentException.class, () -> new UpdateDonationRequestUseCase(null));
  }

  @Test
  void shouldValidateInputFields() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));

    // Null version
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdateDonationRequestUseCase.Input("id", null, 5, LocalDate.now().plusDays(5), "actor")));

    // No mutable fields
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdateDonationRequestUseCase.Input("id", 1L, null, null, "actor")));

    // Invalid requestId
    assertThrows(ValidationException.class, () -> useCase.execute(
        new UpdateDonationRequestUseCase.Input("not-uuid", 1L, 5, null, "actor")));
  }

  @Test
  void shouldThrowNotFoundWhenRequestMissing() {
    DomainID id = DomainID.generate();
    when(repository.findById(id)).thenReturn(Optional.empty());

    var input = new UpdateDonationRequestUseCase.Input(id.getValue().toString(), 1L, 5, null, "actor");
    assertThrows(NotFoundException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldThrowConcurrencyExceptionOnVersionConflict() {
    DonationRequest request = request();
    request.setVersion(1L);
    DomainID requestId = request.getId();
    when(repository.findById(requestId)).thenReturn(Optional.of(request));

    var input = new UpdateDonationRequestUseCase.Input(
        requestId.getValue().toString(), 2L, 10, null, "actor");
    assertThrows(ConcurrencyException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldThrowForbiddenWhenActorMismatch() {
    DonationRequest request = request();
    request.setVersion(1L);
    DomainID requestId = request.getId();
    when(repository.findById(requestId)).thenReturn(Optional.of(request));

    var input = new UpdateDonationRequestUseCase.Input(
        requestId.getValue().toString(), 1L, 10, null, DomainID.generate().getValue().toString());
    assertThrows(ForbiddenException.class, () -> useCase.execute(input));
  }

  @Test
  void shouldValidateDomainRulesOnUpdate() {
    DonationRequest request = request();
    request.setVersion(1L);
    DomainID requestId = request.getId();
    String actorId = request.getRequester().getParty().getId().getValue().toString();
    when(repository.findById(requestId)).thenReturn(Optional.of(request));

    // Goal <= 0
    var invalidGoal = new UpdateDonationRequestUseCase.Input(
        requestId.getValue().toString(), 1L, -5, null, actorId);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidGoal));

    // Limit in past
    var invalidLimit = new UpdateDonationRequestUseCase.Input(
        requestId.getValue().toString(), 1L, null, LocalDate.now().minusDays(1), actorId);
    assertThrows(ValidationException.class, () -> useCase.execute(invalidLimit));
  }

  private DonationRequest request() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    BloodCenter bloodCenter = new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
    return DonationRequest.create(
        requester, bloodCenter, BloodType.of("O+"), 5, LocalDate.now().plusDays(10), Urgency.MEDIUM, null);
  }
}
