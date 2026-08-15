package bloodmatch.application.usecase.donation.createpending;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.bloodcenter.schedule.AppointmentSlot;
import bloodmatch.domain.bloodcenter.schedule.AppointmentSlotCalculator;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class CreatePendingDonationUseCase {
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRepositoryInterface donationRepository;
  private final BloodCenterScheduleRepositoryInterface scheduleRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final NotificationServiceInterface notificationService;

  public CreatePendingDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository,
      BloodCenterScheduleRepositoryInterface scheduleRepository,
      UserAccountRepositoryInterface userAccountRepository,
      NotificationServiceInterface notificationService) {
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
    this.scheduleRepository = scheduleRepository;
    this.userAccountRepository = userAccountRepository;
    this.notificationService = notificationService;
  }

  public Output execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public Output execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID donorId = DomainIdParser.parse(input.personId(), "personId");
    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    if (input.expectedDate() == null) {
      throw new ValidationException("expectedDate cannot be null");
    }

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    BloodCenterSchedule schedule = scheduleRepository.findByOrganizationId(organizationId).orElse(null);
    LocalTime expectedTime = input.expectedTime();
    if (schedule != null && schedule.requiresTimeSlot(input.expectedDate())) {
      if (expectedTime == null) {
        throw new ValidationException("expectedTime is required for this blood center");
      }
      List<Donation> pending = donationRepository.findPendingByOrganizationIdAndDate(
          organizationId, input.expectedDate());
      List<AppointmentSlot> slots = AppointmentSlotCalculator.calculate(schedule, input.expectedDate(), pending);
      AppointmentSlot selected = slots.stream()
          .filter(slot -> expectedTime.equals(slot.startTime()))
          .findFirst()
          .orElse(null);
      if (selected == null) {
        throw new ValidationException("expectedTime does not match a valid slot");
      }
      if (selected.available() == 0) {
        throw new ValidationException("Time slot is fully booked");
      }
    }

    Donation donation = Donation.createPending(
        donor, input.expectedDate(), bloodCenter, currentDate, expectedTime);
    donationRepository.save(donation);
    notifyBloodCenter(bloodCenter, donor, donation);

    return Output.from(donation);
  }

  private void notifyBloodCenter(BloodCenter bloodCenter, Donor donor, Donation donation) {
    try {
      UserAccount account = userAccountRepository
          .findByPartyId(bloodCenter.getOrganization().getId())
          .orElse(null);
      if (account == null) {
        return;
      }
      notificationService.notifyBloodCenterAboutAppointment(bloodCenter, account, donor, donation);
    } catch (RuntimeException ex) {
      System.err.println("Failed to notify blood center about appointment: " + ex.getMessage());
    }
  }

  public record Input(String personId, String organizationId, LocalDate expectedDate, LocalTime expectedTime) {
  }

  public record Output(String id, LocalDate expectedDate, LocalTime expectedTime, String status) {
    public static Output from(Donation donation) {
      return new Output(
          donation.getId().getValue().toString(),
          donation.getDonationDate(),
          donation.getExpectedTime(),
          statusOf(donation));
    }

    private static String statusOf(Donation donation) {
      if (donation.isCompleted()) {
        return "COMPLETED";
      }
      if (donation.isPending()) {
        return "PENDING";
      }
      if (donation.isCancelled()) {
        return "CANCELLED";
      }
      return "UNKNOWN";
    }
  }
}
