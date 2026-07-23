package bloodmatch.domain.donationrequest;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;


public class DonationRequest extends DomainObject {

  private Requester requester;
  private BloodCenter bloodCenter;
  private BloodType bloodTypeNeeded;
  private int goalBloodBags;
  private LocalDate dateRequested;
  private LocalDate dateLimit;
  private boolean active;






  private Urgency urgency;

  private DonationRequest(
      Requester requester,
      BloodCenter bloodCenter,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateLimit,
      LocalDate currentDate,
      Urgency urgency) {
    this.id = DomainID.generate();
    this.requester = requester;
    this.bloodCenter = bloodCenter;
    this.bloodTypeNeeded = bloodTypeNeeded;
    this.goalBloodBags = goalBloodBags;
    this.dateRequested = currentDate;
    this.dateLimit = dateLimit;
    this.active = true;
    this.urgency = urgency;
  }

  public static DonationRequest create(
      Requester requester,
      BloodCenter bloodCenter,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateLimit,
      Urgency urgency) {
    return create(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        goalBloodBags,
        dateLimit,
        LocalDate.now(),
        urgency);
  }

  public static DonationRequest create(
      Requester requester,
      BloodCenter bloodCenter,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateLimit,
      LocalDate currentDate,
      Urgency urgency) {
    if (requester == null)
      throw new IllegalArgumentException("Requester cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");
    if (bloodTypeNeeded == null)
      throw new IllegalArgumentException("Blood type cannot be null");
    if (goalBloodBags <= 0)
      throw new IllegalArgumentException("Goal blood bags must be greater than zero");
    if (dateLimit == null)
      throw new IllegalArgumentException("Limit date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (urgency == null)
      throw new IllegalArgumentException("Urgency cannot be null");
    if (dateLimit.isBefore(currentDate))
      throw new IllegalArgumentException("Limit date invalid");
    return new DonationRequest(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        goalBloodBags,
        dateLimit,
        currentDate,
        urgency);
  }

  public static DonationRequest reconstitute(
      DomainID id,
      Requester requester,
      BloodCenter bloodCenter,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateRequested,
      LocalDate dateLimit,
      boolean isActive,
      Urgency urgency) {

    if (id == null)
      throw new IllegalArgumentException("Id cannot be null");
    if (requester == null)
      throw new IllegalArgumentException("Requester cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");
    if (bloodTypeNeeded == null)
      throw new IllegalArgumentException("Blood type cannot be null");
    if (goalBloodBags <= 0)
      throw new IllegalArgumentException("Goal blood bags must be greater than zero");
    if (dateRequested == null)
      throw new IllegalArgumentException("Requested date cannot be null");
    if (dateLimit == null)
      throw new IllegalArgumentException("Limit date cannot be null");
    if (urgency == null)
      throw new IllegalArgumentException("Urgency cannot be null");

    DonationRequest request = new DonationRequest(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        goalBloodBags,
        dateLimit,
        dateRequested,
        urgency);

    request.setId(id);
    request.dateRequested = dateRequested;
    request.active = isActive;

    return request;
  }

  public void close() {
    if (!active)
      throw new IllegalStateException("Request already closed");

    this.active = false;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isExpired() {
    return isExpired(LocalDate.now());
  }

  public boolean isExpired(LocalDate currentDate) {
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    return currentDate.isAfter(dateLimit);
  }

  public boolean canBeFulfilledBy(BloodType candidateBloodType) {
    return canBeFulfilledBy(candidateBloodType, LocalDate.now());
  }

  public boolean canBeFulfilledBy(
      BloodType candidateBloodType,
      LocalDate currentDate) {

    if (candidateBloodType == null)
      throw new IllegalArgumentException("Candidate blood type cannot be null");

    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    if (!isActive())
      return false;

    if (isExpired(currentDate))
      return false;

    return candidateBloodType.canDonateTo(bloodTypeNeeded);
  }

  
  public BloodType getBloodTypeNeeded() {
    return bloodTypeNeeded;
  }

  public int getGoalBloodBags() {
    return goalBloodBags;
  }


  public void setGoalBloodBags(int goalBloodBags) {
    if (goalBloodBags <= 0)
      throw new IllegalArgumentException("Goal blood bags must be greater than zero");
    this.goalBloodBags = goalBloodBags;
  }

  
  public BloodCenter getBloodCenter() {
    return bloodCenter;
  }

  public LocalDate getDateRequested() {
    return dateRequested;
  }

  public LocalDate getDateLimit() {
    return dateLimit;
  }

  public void setDateLimit(LocalDate dateLimit) {
    if (dateLimit == null)
      throw new IllegalArgumentException("Limit date cannot be null");
    if (dateLimit.isBefore(LocalDate.now()))
      throw new IllegalArgumentException("Limit date cannot be in the past");
    this.dateLimit = dateLimit;
  }

  public Requester getRequester() {
    return requester;
  }

  public Urgency getUrgency() {
    return urgency;
  }
  public boolean acceptsDonation(
        Donation donation,
        LocalDate currentDate) {

    if (donation == null)
        throw new IllegalArgumentException("Donation cannot be null");

    if (currentDate == null)
        throw new IllegalArgumentException("Current date cannot be null");

    if (!isActive())
        return false;

    if (isExpired(currentDate))
        return false;

    if (!donation.isCompleted())
        return false;

    if (donation.getDonationDate().isBefore(dateRequested))
        return false;

    if (donation.getDonationDate().isAfter(dateLimit))
        return false;

    return donation.getDonor()
            .getBloodType()
            .canDonateTo(bloodTypeNeeded);
}
  
}