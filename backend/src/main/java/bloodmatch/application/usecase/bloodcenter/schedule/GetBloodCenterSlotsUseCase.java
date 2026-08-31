package bloodmatch.application.usecase.bloodcenter.schedule;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.TimeFormats;
import bloodmatch.domain.bloodcenter.schedule.AppointmentSlot;
import bloodmatch.domain.bloodcenter.schedule.AppointmentSlotCalculator;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GetBloodCenterSlotsUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterScheduleRepositoryInterface scheduleRepository;
  private final DonationRepositoryInterface donationRepository;

  public GetBloodCenterSlotsUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterScheduleRepositoryInterface scheduleRepository,
      DonationRepositoryInterface donationRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.scheduleRepository = scheduleRepository;
    this.donationRepository = donationRepository;
  }

  public Output execute(Input input) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }
    if (input.date() == null) {
      throw new ValidationException("date cannot be null");
    }

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    BloodCenterSchedule schedule = scheduleRepository.findByOrganizationId(organizationId).orElse(null);
    if (schedule == null || !schedule.requiresTimeSlot(input.date())) {
      return Output.withoutSchedule(organizationId.getValue().toString(), input.date());
    }

    List<Donation> pending = donationRepository.findPendingByOrganizationIdAndDate(organizationId, input.date());
    List<AppointmentSlot> slots = AppointmentSlotCalculator.calculate(schedule, input.date(), pending);
    return Output.withSchedule(organizationId.getValue().toString(), input.date(), slots);
  }

  public record Input(String organizationId, LocalDate date) {
  }

  public record Output(
      String organizationId,
      LocalDate date,
      boolean hasSchedule,
      List<SlotOutput> slots) {

    public static Output withoutSchedule(String organizationId, LocalDate date) {
      return new Output(organizationId, date, false, List.of());
    }

    public static Output withSchedule(String organizationId, LocalDate date, List<AppointmentSlot> slots) {
      return new Output(
          organizationId,
          date,
          true,
          slots.stream().map(SlotOutput::from).toList());
    }
  }

  public record SlotOutput(
      String startTime,
      String endTime,
      int capacity,
      int booked,
      int available) {

    public static SlotOutput from(AppointmentSlot slot) {
      return new SlotOutput(
          TimeFormats.format(slot.startTime()),
          TimeFormats.format(slot.endTime()),
          slot.capacity(),
          slot.booked(),
          slot.available());
    }
  }
}
