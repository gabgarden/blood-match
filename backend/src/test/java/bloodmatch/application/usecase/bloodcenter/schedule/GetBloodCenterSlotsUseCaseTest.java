package bloodmatch.application.usecase.bloodcenter.schedule;

import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterSlotsUseCase.Input;
import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterSlotsUseCase.Output;
import bloodmatch.application.usecase.bloodcenter.schedule.GetBloodCenterSlotsUseCase.SlotOutput;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetBloodCenterSlotsUseCaseTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final BloodCenterScheduleRepositoryInterface scheduleRepository = mock(BloodCenterScheduleRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final GetBloodCenterSlotsUseCase useCase = new GetBloodCenterSlotsUseCase(
      bloodCenterRepository,
      scheduleRepository,
      donationRepository);

  @Test
  void shouldSubtractBookingsFromSlotCapacity() {
    BloodCenter bloodCenter = bloodCenter();
    BloodCenterSchedule schedule = mondayMorningSchedule(bloodCenter);
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(scheduleRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(schedule));
    when(donationRepository.findPendingByOrganizationIdAndDate(bloodCenter.getOrganization().getId(), MONDAY))
        .thenReturn(List.of(
            pendingAt(bloodCenter, LocalTime.of(8, 0)),
            pendingAt(bloodCenter, LocalTime.of(8, 0))));

    Output output = useCase.execute(new Input(
        bloodCenter.getOrganization().getId().getValue().toString(), MONDAY));

    assertTrue(output.hasSchedule());
    SlotOutput first = output.slots().get(0);
    assertEquals("08:00", first.startTime());
    assertEquals("08:30", first.endTime());
    assertEquals(4, first.capacity());
    assertEquals(2, first.booked());
    assertEquals(2, first.available());
  }

  @Test
  void shouldReturnEmptySlotsWhenDateIsBlocked() {
    BloodCenter bloodCenter = bloodCenter();
    BloodCenterSchedule schedule = BloodCenterSchedule.create(
        bloodCenter.getOrganization().getId(),
        List.of(new WeeklyWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 4)),
        List.of(MONDAY));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(scheduleRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(schedule));
    when(donationRepository.findPendingByOrganizationIdAndDate(bloodCenter.getOrganization().getId(), MONDAY))
        .thenReturn(List.of());

    Output output = useCase.execute(new Input(
        bloodCenter.getOrganization().getId().getValue().toString(), MONDAY));

    assertFalse(output.hasSchedule());
    assertTrue(output.slots().isEmpty());
  }

  @Test
  void shouldReturnHasScheduleFalseWhenNoScheduleIsSaved() {
    BloodCenter bloodCenter = bloodCenter();
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(scheduleRepository.findByOrganizationId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.empty());

    Output output = useCase.execute(new Input(
        bloodCenter.getOrganization().getId().getValue().toString(), MONDAY));

    assertFalse(output.hasSchedule());
    assertTrue(output.slots().isEmpty());
  }

  private BloodCenterSchedule mondayMorningSchedule(BloodCenter bloodCenter) {
    return BloodCenterSchedule.create(
        bloodCenter.getOrganization().getId(),
        List.of(new WeeklyWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 4)),
        List.of());
  }

  private Donation pendingAt(BloodCenter bloodCenter, LocalTime time) {
    return Donation.createPending(donor(), MONDAY, bloodCenter, MONDAY.minusDays(1), time);
  }

  private Donor donor() {
    return new Donor(
        new Person("Ana Silva", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O+"),
        70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Hemocentro Regional", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
