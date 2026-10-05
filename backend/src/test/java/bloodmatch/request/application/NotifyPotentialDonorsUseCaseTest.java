package bloodmatch.request.application;

import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.request.application.notification.NotifyPotentialDonorsUseCase;
import bloodmatch.request.application.notification.NotifyPotentialDonorsUseCase.Input;
import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.request.domain.Urgency;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.request.domain.DonationRequestFulfillmentService;
import bloodmatch.shared.domain.services.NotificationServiceInterface;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

  @Test
  void shouldValidateInputAndNullChecks() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));
    assertThrows(ValidationException.class, () -> useCase.execute(new Input("id", "id"), null));
  }

  @Test
  void shouldThrowNotFoundWhenRequestDoesNotExist() {
    when(requestRepository.findById(any())).thenReturn(Optional.empty());
    assertThrows(bloodmatch.shared.application.exception.NotFoundException.class, () -> useCase.execute(
        new Input(DomainID.generate().getValue().toString(), DomainID.generate().getValue().toString()), currentDate));
  }

  @Test
  void shouldThrowWhenRequestIsExpired() {
    DonationRequest request = request();
    stubRequest(request);
    LocalDate pastDate = request.getDateLimit().plusDays(1);

    assertThrows(ValidationException.class, () -> useCase.execute(input(request), pastDate));
  }

  @Test
  void shouldNotifyEligibleDonorsWhenAccountIsEnabledAndSkipWhenDisabledOrNull() {
    DonationRequest request = request();
    bloodmatch.shared.domain.valueObjects.Address addr = new bloodmatch.shared.domain.valueObjects.Address("Rua A, 1", "SP", "SP", "01001-000", -23.55, -46.63);
    request.getBloodCenter().getOrganization().changeAddress(addr);
    stubRequest(request);
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of());

    Donor eligibleDonor1 = new Donor(
        new Person("Doador 1", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O-"),
        70.0);
    eligibleDonor1.getPerson().changeAddress(addr);

    Donor eligibleDonor2 = new Donor(
        new Person("Doador 2", new PhoneNumber("11977776666"), new CPF("98765432101"), LocalDate.of(1992, 1, 1)),
        BloodType.of("O-"),
        75.0);
    eligibleDonor2.getPerson().changeAddress(addr);

    when(donorRepository.findAll()).thenReturn(List.of(eligibleDonor1, eligibleDonor2));

    bloodmatch.auth.domain.UserAccount activeAccount = new bloodmatch.auth.domain.UserAccount(
        eligibleDonor1.getPerson().getId(),
        new bloodmatch.shared.domain.valueObjects.Email("d1@example.com"),
        "hash",
        java.util.Set.of(bloodmatch.auth.domain.SecurityRole.DONOR));

    when(userAccountRepository.findByPartyId(eligibleDonor1.getPerson().getId())).thenReturn(Optional.of(activeAccount));
    when(userAccountRepository.findByPartyId(eligibleDonor2.getPerson().getId())).thenReturn(Optional.empty());

    var result = useCase.execute(input(request), currentDate);
    assertEquals("Notifications sent to eligible donors successfully.", result.message());
    verify(notificationService).notifyDonorAboutRequest(eq(eligibleDonor1), eq(activeAccount), eq(request));

    // Also test execute(Input) overload
    DonationRequest futureRequest = DonationRequest.create(
        request.getRequester(),
        request.getBloodCenter(),
        BloodType.of("A+"),
        1,
        LocalDate.now().plusDays(10),
        LocalDate.now(),
        Urgency.MEDIUM,
        null);
    stubRequest(futureRequest);
    assertNotNull(useCase.execute(input(futureRequest)));
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
