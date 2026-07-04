/* package bloodmatch.domain.donation;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;




// passar tudo isso para a donation, use case chamar direto os facthory methods e eliminar essa classe.
@Component
public class DonationFactory {

  public Donation createExternalDonation(
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate date) {
    return createExternalDonation(donor, bloodCenter, date, LocalDate.now());
  }

  public Donation createExternalDonation(
      Donor donor,
      BloodCenter bloodCenter,
      LocalDate date,
      LocalDate currentDate) {

    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");
    if (date == null)
      throw new IllegalArgumentException("Date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (!donor.isEligibleToDonate(currentDate))
      throw new IllegalStateException("Donor not eligible");

    Donation donation = Donation.registerExternalDonation(donor, date, bloodCenter, currentDate);
    donor.registerDonation(date, currentDate);
    return donation;
  }

  public Donation createPendingDonationFromRequest(
      Donor donor,
      DonationRequest request,
      LocalDate expectedDate) {
    return createPendingDonationFromRequest(donor, request, expectedDate, LocalDate.now());
  }

  public Donation createPendingDonationFromRequest(
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

    return Donation.scheduleFromRequest(donor, request, expectedDate, currentDate);
  }






  public Donation completePendingDonation(
      Donation pendingDonation,
      LocalDate completionDate) {
    return completePendingDonation(pendingDonation, completionDate, LocalDate.now());
  }

  public Donation completePendingDonation(
      Donation pendingDonation,
      LocalDate completionDate,
      LocalDate currentDate) {

    if (pendingDonation == null)
      throw new IllegalArgumentException("Donation cannot be null");
    if (completionDate == null)
      throw new IllegalArgumentException("Completion date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Donor donor = pendingDonation.getDonor();
    if (!donor.isEligibleToDonate(currentDate))
      throw new IllegalStateException("Donor not eligible");

    pendingDonation.complete(completionDate, currentDate);
    donor.registerDonation(completionDate, currentDate);
    return pendingDonation;
  }

  public Donation cancelPendingDonation(Donation pendingDonation) {
    if (pendingDonation == null)
      throw new IllegalArgumentException("Donation cannot be null");

    pendingDonation.cancel();
    return pendingDonation;
  }
} */