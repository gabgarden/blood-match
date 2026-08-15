package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;

public interface NotificationServiceInterface {
    void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request);

    void notifyBloodCenterAboutAppointment(
            BloodCenter bloodCenter,
            UserAccount bloodCenterAccount,
            Donor donor,
            Donation donation);
}
