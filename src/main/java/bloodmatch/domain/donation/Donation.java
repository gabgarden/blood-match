package bloodmatch.domain.donation;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;

public class Donation extends DomainObject {

 

  private Donor donor;
  private DonationRequest request;
  private LocalDate donationDate;
  private BloodCenter bloodCenter;
  private boolean isCancelled;
  private boolean isCompleted;
  private boolean isPending;
  

  private Donation(
      DomainID id,
      Donor donor,
      DonationRequest request,
      LocalDate donationDate,
      BloodCenter bloodCenter
       ) {
    this.id = id;
    this.donor = donor;
    this.request = request;
    this.donationDate = donationDate;
    this.bloodCenter = bloodCenter;
  }

  private Donation(
      Donor donor,
      DonationRequest request,
      LocalDate donationDate,
      BloodCenter bloodCenter) {
    this(DomainID.generate(), donor, request, donationDate, bloodCenter);
  }

  //  factory methods 

  static Donation scheduleFromRequest(
      Donor donor,
      DonationRequest request,
      LocalDate expectedDate,
      LocalDate currentDate) {

    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (request == null)
      throw new IllegalArgumentException("Request cannot be null");
    if (expectedDate == null)
      throw new IllegalArgumentException("Expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (!request.isActive())
      throw new IllegalStateException("Request is not active");
    if (!donor.getBloodType().canDonateTo(request.getBloodTypeNeeded()))
      throw new IllegalStateException("Incompatible blood type");
    if (expectedDate.isBefore(currentDate))
      throw new IllegalArgumentException("Expected date cannot be in the past");

    Donation donation  = new Donation(
        donor,
        request,
        expectedDate,
        request.getBloodCenter());

    donation.isPending = true;
    
    return donation;

  }

  static Donation registerExternalDonation(
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

    Donation donation = new Donation(donor, null, donationDate, bloodCenter);
    donation.isCompleted = true;
    return donation;
  }

  public static Donation reconstitute(
      DomainID id,
      Donor donor,
      DonationRequest request,
      LocalDate donationDate,
      BloodCenter bloodCenter) {

    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");

    Donation donation = new Donation(id, donor, request, donationDate, bloodCenter);

    if (donationDate.isBefore(LocalDate.now())) {
      donation.isCompleted = true;
    }

    if (donationDate.isAfter(LocalDate.now())) {
      donation.isPending = true;
    }
    

    return donation;
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
    this.isCompleted = true;
    this.isPending = false;
  }

  public void cancel() {
    if (!isPending())
      throw new IllegalStateException("Only pending donations can be cancelled");
    this.isCancelled = true;
    this.isPending = false;
  }





  
  //  queries 

  public boolean isFromRequest() {
    return request != null;
  }

  public boolean isPending() {
    return isPending;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public Donor getDonor() { return donor; }
  public LocalDate getDonationDate() { return donationDate; }
  public DonationRequest getRequest() { return request; }
  public BloodCenter getBloodCenter() { return bloodCenter; }
}