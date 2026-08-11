package bloodmatch.domain.services;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;

public interface NotificationServiceInterface {
    void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request);
}