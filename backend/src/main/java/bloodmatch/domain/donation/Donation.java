package bloodmatch.domain.donation;

import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;
import java.time.LocalTime;

public class Donation extends DomainObject {

  private Donor donor;
  private BloodCenter bloodCenter;
  private LocalDate intendedDate;
  private LocalDate donationDate;
  private LocalDate cancelledAt;
  private LocalTime expectedTime;

  private Donation(
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalTime expectedTime) {
    this.id = DomainID.generate();
    this.donor = donor;
    this.bloodCenter = bloodCenter;
    this.intendedDate = intendedDate;
    this.donationDate = donationDate;
    this.expectedTime = expectedTime;
  }

  public static Donation create(
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalDate currentDate) {
    return create(donor, bloodCenter, intendedDate, donationDate, null, currentDate);
  }

  public static Donation create(
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalTime expectedTime,
      LocalDate currentDate) {
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    boolean hasIntendedDate = intendedDate != null;
    boolean hasDonationDate = donationDate != null;
    if (hasIntendedDate == hasDonationDate) {
      throw new IllegalArgumentException("Provide either intendedDate or donationDate");
    }

    if (hasIntendedDate) {
      if (intendedDate.isBefore(currentDate))
        throw new IllegalArgumentException("Intended date cannot be in the past");
    } else {
      if (donationDate.isAfter(currentDate))
        throw new IllegalArgumentException("Donation date cannot be in the future");
      if (expectedTime != null)
        throw new IllegalArgumentException("expectedTime is only valid for pending donations");
    }

    return new Donation(donor, bloodCenter, intendedDate, donationDate, expectedTime);
  }

  public static Donation reconstitute(
      DomainID id,
      Donor donor,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalDate cancelledAt,
      BloodCenter bloodCenter) {
    return reconstitute(id, donor, intendedDate, donationDate, cancelledAt, bloodCenter, null);
  }

  public static Donation reconstitute(
      DomainID id,
      Donor donor,
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalDate cancelledAt,
      BloodCenter bloodCenter,
      LocalTime expectedTime) {

    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    validateDates(intendedDate, donationDate, cancelledAt);

    Donation donation = new Donation(donor, bloodCenter, intendedDate, donationDate, expectedTime);
    donation.setId(id);
    donation.cancelledAt = cancelledAt;
    return donation;
  }

  private static void validateDates(
      LocalDate intendedDate,
      LocalDate donationDate,
      LocalDate cancelledAt) {
    if (donationDate != null && cancelledAt != null) {
      throw new IllegalArgumentException("Donation cannot be completed and cancelled");
    }
    if (cancelledAt != null && intendedDate == null) {
      throw new IllegalArgumentException("Cancelled donation requires intendedDate");
    }
    if (donationDate == null && cancelledAt == null && intendedDate == null) {
      throw new IllegalArgumentException("Donation requires intendedDate or donationDate");
    }
  }

  public void complete(LocalDate completionDate, LocalDate currentDate) {
    if (completionDate == null)
      throw new IllegalArgumentException("Completion date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be completed");
    if (completionDate.isAfter(currentDate))
      throw new IllegalArgumentException("Completion date cannot be in the future");

    this.donationDate = completionDate;
  }

  public void reschedule(LocalDate newIntendedDate, LocalDate currentDate) {
    reschedule(newIntendedDate, currentDate, this.expectedTime);
  }

  public void reschedule(LocalDate newIntendedDate, LocalDate currentDate, LocalTime newExpectedTime) {
    if (newIntendedDate == null)
      throw new IllegalArgumentException("New intended date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be rescheduled");
    if (newIntendedDate.isBefore(currentDate))
      throw new IllegalArgumentException("New intended date cannot be in the past");

    this.intendedDate = newIntendedDate;
    this.expectedTime = newExpectedTime;
  }

  public void cancel(LocalDate cancellationDate) {
    if (cancellationDate == null)
      throw new IllegalArgumentException("Cancellation date cannot be null");
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be cancelled");

    this.cancelledAt = cancellationDate;
  }

  public boolean isPending() {
    return donationDate == null && cancelledAt == null;
  }

  public boolean isCompleted() {
    return donationDate != null;
  }

  public boolean isCancelled() {
    return cancelledAt != null;
  }

  public String status() {
    if (isCompleted()) {
      return "COMPLETED";
    }
    if (isCancelled()) {
      return "CANCELLED";
    }
    return "PENDING";
  }

  public Donor getDonor() {
    return donor;
  }

  public BloodCenter getBloodCenter() {
    return bloodCenter;
  }

  public LocalDate getIntendedDate() {
    return intendedDate;
  }

  public LocalDate getDonationDate() {
    return donationDate;
  }

  public LocalDate getCancelledAt() {
    return cancelledAt;
  }

  public LocalDate getReferenceDate() {
    return donationDate != null ? donationDate : intendedDate;
  }

  public LocalTime getExpectedTime() {
    return expectedTime;
  }
}
