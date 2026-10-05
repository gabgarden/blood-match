package bloodmatch.request.application;

import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.request.domain.Urgency;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.requester.Requester;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloseDonationRequestUseCaseTest {

  private final DonationRequestRepositoryInterface repository = mock(DonationRequestRepositoryInterface.class);
  private final CloseDonationRequestUseCase useCase = new CloseDonationRequestUseCase(repository);

  @Test
  void shouldCloseDonationRequestSuccessfully() {
    DonationRequest request = request();
    DomainID requestId = request.getId();

    when(repository.findById(requestId)).thenReturn(Optional.of(request));

    useCase.execute(new CloseDonationRequestUseCase.Input(requestId.getValue().toString()));

    assertFalse(request.isActive());
    verify(repository).save(request);
  }

  @Test
  void shouldRejectNullConstructorArg() {
    assertThrows(IllegalArgumentException.class, () -> new CloseDonationRequestUseCase(null));
  }

  @Test
  void shouldValidateInputAndNotFound() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));
    assertThrows(ValidationException.class, () -> useCase.execute(new CloseDonationRequestUseCase.Input("not-a-uuid")));

    DomainID id = DomainID.generate();
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> useCase.execute(new CloseDonationRequestUseCase.Input(id.getValue().toString())));
  }

  private DonationRequest request() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    BloodCenter bloodCenter = new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
    return DonationRequest.create(
        requester, bloodCenter, BloodType.of("O+"), 5, LocalDate.now().plusDays(10), Urgency.MEDIUM, null);
  }
}
