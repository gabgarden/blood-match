package bloodmatch.donation.application;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.donation.application.create.CreateDonationUseCase;
import bloodmatch.donation.application.create.CreateDonationUseCase.Input;
import bloodmatch.donation.application.create.CreateDonationUseCase.Output;
import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.shared.domain.services.NotificationServiceInterface;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateDonationUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final NotificationServiceInterface notificationService = mock(NotificationServiceInterface.class);
  private final CreateDonationUseCase useCase = new CreateDonationUseCase(
      donorRepository,
      bloodCenterRepository,
      donationRepository,
      userAccountRepository,
      notificationService);

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

  @Test
  void createsPendingDonationWhenIntendedDateIsProvided() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(userAccountRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.empty());

    Output output = useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            MONDAY,
            null,
            null),
        MONDAY.minusDays(1));

    assertEquals("PENDING", output.status());
    assertEquals(MONDAY, output.intendedDate());
    assertNull(output.donationDate());
    assertNull(output.expectedTime());
    verify(donationRepository).save(any(Donation.class));
    verify(donorRepository, never()).save(any());
    verify(notificationService, never()).notifyBloodCenterAboutAppointment(any(), any(), any(), any());
  }

  @Test
  void createsCompletedDonationWhenDonationDateIsProvided() {
    LocalDate currentDate = LocalDate.now();
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));

    Output result = useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            null,
            currentDate,
            null),
        currentDate);

    verify(donorRepository).save(donor);
    verify(donationRepository).save(any(Donation.class));
    assertEquals("COMPLETED", result.status());
    assertEquals(currentDate, result.donationDate());
    assertNull(result.intendedDate());
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationUseCase(null, bloodCenterRepository, donationRepository, userAccountRepository, notificationService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationUseCase(donorRepository, null, donationRepository, userAccountRepository, notificationService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationUseCase(donorRepository, bloodCenterRepository, null, userAccountRepository, notificationService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationUseCase(donorRepository, bloodCenterRepository, donationRepository, null, notificationService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationUseCase(donorRepository, bloodCenterRepository, donationRepository, userAccountRepository, null));
  }

  @Test
  void shouldValidateInputAndDates() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));
    assertThrows(ValidationException.class, () -> useCase.execute(new Input("id", "id", MONDAY, null, null), null));
  }

  @Test
  void shouldThrowWhenDonorOrBloodCenterNotFound() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();

    when(donorRepository.findByPartyId(any())).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), MONDAY, null, null),
        MONDAY.minusDays(1)));

    when(donorRepository.findByPartyId(any())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(any())).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), MONDAY, null, null),
        MONDAY.minusDays(1)));
  }

  @Test
  void shouldHandleNotificationsWhenSchedulingPendingDonation() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId())).thenReturn(Optional.of(bloodCenter));

    UserAccount account = new UserAccount(
        bloodCenter.getOrganization().getId(),
        new bloodmatch.shared.domain.valueObjects.Email("center@example.com"),
        "hash123",
        java.util.Set.of(bloodmatch.auth.domain.SecurityRole.BLOOD_CENTER));
    when(userAccountRepository.findByPartyId(bloodCenter.getOrganization().getId())).thenReturn(Optional.of(account));

    Output output = useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), MONDAY, null, LocalTime.of(8, 0)),
        MONDAY.minusDays(1));
    assertEquals("PENDING", output.status());
    verify(notificationService).notifyBloodCenterAboutAppointment(any(), any(), any(), any());

    // When account is null
    when(userAccountRepository.findByPartyId(bloodCenter.getOrganization().getId())).thenReturn(Optional.empty());
    Output outputNoAccount = useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), MONDAY, null, LocalTime.of(8, 0)),
        MONDAY.minusDays(1));
    assertNotNull(outputNoAccount);

    // When notificationService throws RuntimeException
    when(userAccountRepository.findByPartyId(bloodCenter.getOrganization().getId())).thenReturn(Optional.of(account));
    org.mockito.Mockito.doThrow(new RuntimeException("mail failed")).when(notificationService).notifyBloodCenterAboutAppointment(any(), any(), any(), any());
    Output outputNotificationFail = useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), MONDAY, null, LocalTime.of(8, 0)),
        MONDAY.minusDays(1));
    assertNotNull(outputNotificationFail);

    // Overload execute(Input)
    Output outputOverload = useCase.execute(
        new Input(donor.getPerson().getId().getValue().toString(), bloodCenter.getOrganization().getId().getValue().toString(), LocalDate.now().plusDays(7), null, null));
    assertNotNull(outputOverload);
  }

  @Test
  void shouldRejectExpectedTimeWhenDonationDateProvided() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId())).thenReturn(Optional.of(bloodCenter));

    assertThrows(ValidationException.class, () -> useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            null,
            LocalDate.now(),
            LocalTime.of(10, 0)),
        LocalDate.now()));
  }

  @Test
  void rejectsWhenBothOrNeitherDateIsProvided() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();

    assertThrows(ValidationException.class, () -> useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            MONDAY,
            MONDAY,
            null),
        MONDAY));
    assertThrows(ValidationException.class, () -> useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            null,
            null,
            null),
        MONDAY));
  }

  private Donor donor() {
    return new Donor(
        new Person("Ana Silva", new PhoneNumber("22999990001"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O+"),
        70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Hemocentro Regional", new PhoneNumber("2222222222"), new CNPJ("12345678000100")));
  }
}
