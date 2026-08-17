package bloodmatch.domain.donation;

import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;
import java.time.LocalTime;

public class Donation extends DomainObject {

  private Donor donor;
  private LocalDate donationDate;
  private LocalTime expectedTime;
  private BloodCenter bloodCenter;


  /// tirar essas propriedades, trabalhar com estado apenas nas operações booleanas.
  private boolean cancelled;
  private boolean completed;
  private boolean pending;

  // criar data cancelada, isCanceled() {
  // this.cancelDate dif nulo}


  // deixar apenas um fluxo para criar doação, a doação pendente é uma doação instanciada sem data de doação efetivada
  // parar de chamar de criar doação pendente e criar doação externa, começar a trabalhar com os estados
  // com funções booleanas. 

  private Donation(
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter) {
    this(donor, donationDate, bloodCenter, null);
  }

  private Donation(
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter,
      LocalTime expectedTime) {
    this.id = DomainID.generate();
    this.donor = donor;
    this.donationDate = donationDate;
    this.bloodCenter = bloodCenter;
    this.expectedTime = expectedTime;
  }

  public static Donation createPending(
      Donor donor,
      LocalDate expectedDate,
      BloodCenter bloodCenter,
      LocalDate currentDate) {
    return createPending(donor, expectedDate, bloodCenter, currentDate, null);
  }

  public static Donation createPending(
      Donor donor,
      LocalDate expectedDate,
      BloodCenter bloodCenter,
      LocalDate currentDate,
      LocalTime expectedTime) {

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
        bloodCenter,
        expectedTime);

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
    return reconstitute(id, donor, donationDate, bloodCenter, isCompleted, isPending, isCancelled, null);
  }

  public static Donation reconstitute(
      DomainID id,
      Donor donor,
      LocalDate donationDate,
      BloodCenter bloodCenter,
      boolean isCompleted,
      boolean isPending,
      boolean isCancelled,
      LocalTime expectedTime) {

    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    validateStatusFlags(isCompleted, isPending, isCancelled);

    Donation donation = new Donation(donor, donationDate, bloodCenter, expectedTime);

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

  public void reschedule(LocalDate newExpectedDate, LocalDate currentDate) {
    reschedule(newExpectedDate, currentDate, this.expectedTime);
  }

  public void reschedule(LocalDate newExpectedDate, LocalDate currentDate, LocalTime newExpectedTime) {
    if (newExpectedDate == null)
      throw new IllegalArgumentException("New expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be rescheduled");
    if (newExpectedDate.isBefore(currentDate))
      throw new IllegalArgumentException("New expected date cannot be in the past");

    this.donationDate = newExpectedDate;
    this.expectedTime = newExpectedTime;
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

  public LocalTime getExpectedTime() {
    return expectedTime;
  }

  public BloodCenter getBloodCenter() {
    return bloodCenter;
  }
}