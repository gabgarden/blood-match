package bloodmatch.application.usecase.donation.create;

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
public class CreateDonationUseCase {
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRepositoryInterface donationRepository;
  private final BloodCenterScheduleRepositoryInterface scheduleRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final NotificationServiceInterface notificationService;

  public CreateDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository,
      BloodCenterScheduleRepositoryInterface scheduleRepository,
      UserAccountRepositoryInterface userAccountRepository,
      NotificationServiceInterface notificationService) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (scheduleRepository == null)
      throw new IllegalArgumentException("ScheduleRepository cannot be null");
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    if (notificationService == null)
      throw new IllegalArgumentException("NotificationService cannot be null");
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

    boolean hasIntendedDate = input.intendedDate() != null;
    boolean hasDonationDate = input.donationDate() != null;
    if (hasIntendedDate == hasDonationDate) {
      throw new ValidationException("Provide either intendedDate or donationDate");
    }

    DomainID donorId = DomainIdParser.parse(input.personId(), "personId");
    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));
    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    if (hasIntendedDate) {
      return schedule(input, donor, bloodCenter, organizationId, currentDate);
    }
    return recordCompleted(input, donor, bloodCenter, currentDate);
  }

  private Output schedule(
      Input input,
      Donor donor,
      BloodCenter bloodCenter,
      DomainID organizationId,
      LocalDate currentDate) {
    BloodCenterSchedule schedule = scheduleRepository.findByOrganizationId(organizationId).orElse(null);
    LocalTime expectedTime = input.expectedTime();
    if (schedule != null && schedule.requiresTimeSlot(input.intendedDate())) {
      if (expectedTime == null) {
        throw new ValidationException("expectedTime is required for this blood center");
      }
      List<Donation> pending = donationRepository.findPendingByOrganizationIdAndDate(
          organizationId, input.intendedDate());
      List<AppointmentSlot> slots = AppointmentSlotCalculator.calculate(schedule, input.intendedDate(), pending);
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

    Donation donation = Donation.create(
        donor, bloodCenter, input.intendedDate(), null, expectedTime, currentDate);
    donationRepository.save(donation);
    notifyBloodCenter(bloodCenter, donor, donation);
    return Output.from(donation);
  }

  private Output recordCompleted(
      Input input,
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate currentDate) {
    if (input.expectedTime() != null) {
      throw new ValidationException("expectedTime is only valid when intendedDate is provided");
    }

    Donation donation = Donation.create(
        donor, bloodCenter, null, input.donationDate(), currentDate);
    donor.registerDonation(input.donationDate(), currentDate);
    donorRepository.save(donor);
    donationRepository.save(donation);
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

  public record Input(
      String personId,
      String organizationId,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalTime expectedTime) {
  }

  public record Output(
      String id,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalTime expectedTime,
      String status) {

    public static Output from(Donation donation) {
      return new Output(
          donation.getId().getValue().toString(),
          donation.getIntendedDate(),
          donation.getDonationDate(),
          donation.getExpectedTime(),
          donation.status());
    }
  }
}
