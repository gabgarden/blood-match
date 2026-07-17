package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.CancelDonationRequestUseCase;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelDonationRequestUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository = mock(
      DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);

  private final CancelDonationRequestUseCase useCase = new CancelDonationRequestUseCase(
      donationRequestRepository,
      donationRepository);

  @Test
  void shouldDeleteRequestWhenNoDonationsAreAssociated() {
    DonationRequest request = createRequest();
    DomainID requestId = request.getId();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(donationRepository.existsByRequestId(requestId)).thenReturn(false);

    useCase.execute(requestId);

    verify(donationRequestRepository).deleteById(requestId);
  }

  @Test
  void shouldThrowWhenRequestHasAssociatedDonations() {
    DonationRequest request = createRequest();
    DomainID requestId = request.getId();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(donationRepository.existsByRequestId(requestId)).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> useCase.execute(requestId));

    verify(donationRequestRepository, never()).deleteById(requestId);
  }

  @Test
  void shouldThrowWhenRequestIsNotFound() {
    DomainID requestId = DomainID.generate();

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> useCase.execute(requestId));

    verify(donationRepository, never()).existsByRequestId(requestId);
    verify(donationRequestRepository, never()).deleteById(requestId);
  }

  @Test
  void shouldThrowWhenRequestIdIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
  }

  private DonationRequest createRequest() {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
        new CNPJ("12345678000100")));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
        LocalDate.of(2026, 4, 30),
        Urgency.MEDIUM);
  }
}
