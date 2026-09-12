package bloodmatch.application.usecase;

import bloodmatch.application.exception.ForbiddenException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase;
import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase.Input;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelDonationRequestUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository = mock(
      DonationRequestRepositoryInterface.class);

  private final CancelDonationRequestUseCase useCase = new CancelDonationRequestUseCase(
      donationRequestRepository);

  @Test
  void shouldCloseRequestWhenCancelled() {
    DonationRequest request = createRequest();
    DomainID requestId = request.getId();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    useCase.execute(new Input(requestId.getValue().toString(), null, requesterPartyId(request)));

    verify(donationRequestRepository).save(request);
  }

  @Test
  void shouldThrowWhenRequestIsNotFound() {
    DomainID requestId = DomainID.generate();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
        () -> useCase.execute(new Input(requestId.getValue().toString(), null, UUID.randomUUID().toString())));

    verify(donationRequestRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenRequestIdIsNull() {
    assertThrows(ValidationException.class, () -> useCase.execute(new Input(null, null, null)));
  }

  @Test
  void shouldThrowWhenActorDoesNotOwnRequest() {
    DonationRequest request = createRequest();
    DomainID requestId = request.getId();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    assertThrows(ForbiddenException.class,
        () -> useCase.execute(new Input(requestId.getValue().toString(), null, UUID.randomUUID().toString())));

    verify(donationRequestRepository, never()).save(any());
  }

  @Test
  void shouldAllowAdminBypassWhenActorPartyIdIsNull() {
    DonationRequest request = createRequest();
    DomainID requestId = request.getId();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    useCase.execute(new Input(requestId.getValue().toString(), null, null));

    verify(donationRequestRepository).save(request);
  }

  private static String requesterPartyId(DonationRequest request) {
    return request.getRequester().getParty().getId().getValue().toString();
  }

  private DonationRequest createRequest() {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
        1,
        LocalDate.now().plusDays(30),
        Urgency.MEDIUM,
        null);
  }
}
