package bloodmatch.shared.domain.services;

import bloodmatch.donation.domain.Donation;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.auth.domain.UserAccount;

public interface NotificationServiceInterface {
    void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request);

    void notifyBloodCenterAboutAppointment(
            BloodCenter bloodCenter,
            UserAccount bloodCenterAccount,
            Donor donor,
            Donation donation);
}
