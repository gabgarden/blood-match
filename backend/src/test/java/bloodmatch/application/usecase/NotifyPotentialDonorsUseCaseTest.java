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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bloqueio de notificação com o {@link DonationRequestFulfillmentService} real.
 */
class NotifyPotentialDonorsUseCaseTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

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
  void blocksNotificationWhenTheRealFulfillmentSnapshotHasReachedTheGoal() {
    BloodCenter center = center();
    DonationRequest request = request(center, 1);
    stubRequest(request);
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), eq(TODAY)))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(donation(center)));

    ValidationException error = assertThrows(
        ValidationException.class,
        () -> useCase.execute(input(request), TODAY));

    assertEquals(
        "Cannot notify donors. The goal for this request has already been reached.",
        error.getMessage());
    verify(donorRepository, never()).findAll();
    verify(notificationService, never()).notifyDonorAboutRequest(any(), any(), any());
  }

  @Test
  void continuesWhenTheRealFulfillmentSnapshotHasNotReachedTheGoal() {
    BloodCenter center = center();
    DonationRequest request = request(center, 2);
    stubRequest(request);
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), eq(TODAY)))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of());
    when(donorRepository.findAll()).thenReturn(List.of());

    NotifyPotentialDonorsUseCase.Output result = useCase.execute(input(request), TODAY);

    assertEquals("Notifications sent to eligible donors successfully.", result.message());
    verify(donorRepository).findAll();
    verify(notificationService, never()).notifyDonorAboutRequest(any(), any(), any());
  }

  private void stubRequest(DonationRequest request) {
    when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
  }

  private Input input(DonationRequest request) {
    return new Input(
        request.getId().getValue().toString(),
        request.getRequester().getParty().getId().getValue().toString());
  }

  private DonationRequest request(BloodCenter center, int goal) {
    return DonationRequest.reconstitute(
        id(1),
        new Requester(new Person(
            "Requester",
            new PhoneNumber("11999990000"),
            new CPF("12345678901"),
            LocalDate.of(1990, 1, 1))),
        center,
        BloodType.of("A+"),
        goal,
        TODAY.minusDays(1),
        TODAY.plusDays(5),
        true,
        Urgency.MEDIUM,
        null,
        null);
  }

  private Donation donation(BloodCenter center) {
    return Donation.reconstitute(
        id(10),
        new Donor(
            new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        TODAY,
        null,
        center);
  }

  private BloodCenter center() {
    return new BloodCenter(new Organization(
        "Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));
  }

  private DomainID id(long value) {
    return new DomainID(new UUID(0, value));
  }
}
