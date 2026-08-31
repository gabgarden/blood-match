package bloodmatch.application.usecase;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase;
import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase.Input;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotifyPotentialDonorsUseCaseTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);
  private final DonationRequestRepositoryInterface requestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository =
      mock(DonationRepositoryInterface.class);
  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository =
      mock(UserAccountRepositoryInterface.class);
  private final NotificationServiceInterface notificationService =
      mock(NotificationServiceInterface.class);
  private final NotifyPotentialDonorsUseCase useCase = new NotifyPotentialDonorsUseCase(
      requestRepository,
      donorRepository,
      userAccountRepository,
      notificationService,
      new DonationRequestFulfillmentService(requestRepository, donationRepository));

  @Test
  void shouldRejectNotificationWhenRealFulfillmentSnapshotAlreadyReachedTheGoal() {
    DonationRequest request = request();
    stubRequest(request);
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(completedDonation(request.getBloodCenter(), currentDate)));

    ValidationException error = assertThrows(
        ValidationException.class,
        () -> useCase.execute(input(request), currentDate));

    assertEquals(
        "Cannot notify donors. The goal for this request has already been reached.",
        error.getMessage());
    verify(donorRepository, never()).findAll();
    verify(notificationService, never()).notifyDonorAboutRequest(any(), any(), any());
  }

  @Test
  void shouldProceedWhenRealFulfillmentSnapshotHasNotReachedTheGoal() {
    DonationRequest request = request();
    stubRequest(request);
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of());
    when(donorRepository.findAll()).thenReturn(List.of());

    NotifyPotentialDonorsUseCase.Output result = useCase.execute(input(request), currentDate);

    assertEquals("Notifications sent to eligible donors successfully.", result.message());
    verify(notificationService, never()).notifyDonorAboutRequest(any(), any(), any());
  }

  private void stubRequest(DonationRequest request) {
    when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), any()))
        .thenReturn(List.of(request));
  }

  private Input input(DonationRequest request) {
    return new Input(
        request.getId().getValue().toString(),
        request.getRequester().getParty().getId().getValue().toString());
  }

  private DonationRequest request() {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));
    BloodCenter center = new BloodCenter(new Organization(
        "Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));
    return DonationRequest.create(
        requester,
        center,
        BloodType.of("A+"),
        1,
        currentDate.plusDays(10),
        currentDate.minusDays(2),
        Urgency.MEDIUM,
        null);
  }

  private Donation completedDonation(BloodCenter center, LocalDate date) {
    return Donation.reconstitute(
        new DomainID(new UUID(0, 1)),
        new Donor(
            new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        date,
        null,
        center);
  }
}
