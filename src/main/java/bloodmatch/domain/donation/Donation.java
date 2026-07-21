package bloodmatch.domain.donation;

import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;

public class Donation extends DomainObject {

  private Donor donor;
  private LocalDate donationDate;
  private BloodCenter bloodCenter;
  private boolean cancelled;
  private boolean completed;
  private boolean pending;

  private Donation(
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter) {
    this.id = DomainID.generate();
    this.donor = donor;
    this.donationDate = donationDate;
    this.bloodCenter = bloodCenter;
  }

  public static Donation createPending(
      Donor donor,
      LocalDate expectedDate,
      BloodCenter bloodCenter,
      LocalDate currentDate) {

    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (expectedDate == null)
      throw new IllegalArgumentException("Expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (expectedDate.isBefore(currentDate))
      throw new IllegalArgumentException("Expected date cannot be in the past");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    Donation donation = new Donation(
        donor,
        expectedDate,
        bloodCenter);

    donation.pending = true;
    donation.completed = false;
    donation.cancelled = false;

    return donation;
  }

  public static Donation registerExternalDonation(
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter,
      LocalDate currentDate) {

    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (donationDate.isAfter(currentDate))
      throw new IllegalArgumentException("Donation date cannot be in the future");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    Donation donation = new Donation(donor, donationDate, bloodCenter);
    donation.completed = true;
    donation.pending = false;
    donation.cancelled = false;

    return donation;
  }

  public static Donation reconstitute(
      DomainID id,
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter,
      boolean isCompleted,
      boolean isPending,
      boolean isCancelled) {

    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    validateStatusFlags(isCompleted, isPending, isCancelled);

    Donation donation = new Donation(donor, donationDate, bloodCenter);

    donation.setId(id);
    donation.completed = isCompleted;
    donation.pending = isPending;
    donation.cancelled = isCancelled;

    return donation;
  }

  private static void validateStatusFlags(
      boolean isCompleted,
      boolean isPending,
      boolean isCancelled) {
    int activeStates = 0;
    if (isCompleted) activeStates++;
    if (isPending) activeStates++;
    if (isCancelled) activeStates++;

    if (activeStates != 1) {
      throw new IllegalArgumentException(
          "Donation status must be exactly one of completed, pending or cancelled");
    }
  }

  // behavior

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
    this.completed = true;
    this.pending = false;
  }

  public void cancel() {
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be cancelled");

    this.cancelled = true;
    this.pending = false;
  }

  // queries

  public boolean isPending() {
    return pending;
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public Donor getDonor() {
    return donor;
  }

  public LocalDate getDonationDate() {
    return donationDate;
  }

  public BloodCenter getBloodCenter() {
    return bloodCenter;
  }
}