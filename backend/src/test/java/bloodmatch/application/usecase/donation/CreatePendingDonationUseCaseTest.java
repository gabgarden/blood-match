package bloodmatch.application.usecase.donation;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase;
import bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase.Input;
import bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase.Output;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.bloodcenter.schedule.WeeklyWindow;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreatePendingDonationUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final BloodCenterScheduleRepositoryInterface scheduleRepository =
      mock(BloodCenterScheduleRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final NotificationServiceInterface notificationService = mock(NotificationServiceInterface.class);
  private final CreatePendingDonationUseCase useCase = new CreatePendingDonationUseCase(
      donorRepository,
      bloodCenterRepository,
      donationRepository,
      scheduleRepository,
      userAccountRepository,
      notificationService);

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

  @Test
  void createsDateOnlyBookingWhenBloodCenterHasNoSchedule() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(scheduleRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.empty());

    Output output = useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            MONDAY,
            null),
        MONDAY.minusDays(1));

    assertEquals("PENDING", output.status());
    assertEquals(MONDAY, output.expectedDate());
    assertNull(output.expectedTime());
    verify(donationRepository).save(any(Donation.class));
    verify(notificationService, never()).notifyBloodCenterAboutAppointment(any(), any(), any(), any());
  }

  @Test
  void rejectsBookingWhenTimeSlotIsFullyBooked() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    BloodCenterSchedule schedule = BloodCenterSchedule.create(
        bloodCenter.getOrganization().getId(),
        List.of(new WeeklyWindow(
            DayOfWeek.MONDAY,
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            30,
            1)),
        List.of());
    Donation alreadyBooked = Donation.createPending(
        donor, MONDAY, bloodCenter, MONDAY.minusDays(1), LocalTime.of(8, 0));

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(scheduleRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(schedule));
    when(donationRepository.findPendingByOrganizationIdAndDate(bloodCenter.getOrganization().getId(), MONDAY))
        .thenReturn(List.of(alreadyBooked));

    ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            MONDAY,
            LocalTime.of(8, 0)),
        MONDAY.minusDays(1)));

    assertEquals("Time slot is fully booked", exception.getMessage());
    verify(donationRepository, never()).save(any(Donation.class));
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
