package bloodmatch.infra.external.notification;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class MasterNotificationService implements NotificationServiceInterface {

    private final NotificationServiceInterface consoleService;
    private final NotificationServiceInterface emailService;

    public MasterNotificationService(
            @Qualifier("consoleNotificationService") NotificationServiceInterface consoleService, 
            @Qualifier("emailNotificationService") NotificationServiceInterface emailService) {
        this.consoleService = consoleService;
        this.emailService = emailService;
    }

    @Override
    public void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request) {
        consoleService.notifyDonorAboutRequest(donor, account, request);
        
        emailService.notifyDonorAboutRequest(donor, account, request);
    }

    @Override
    public void notifyBloodCenterAboutAppointment(
            BloodCenter bloodCenter,
            UserAccount bloodCenterAccount,
            Donor donor,
            Donation donation) {
        consoleService.notifyBloodCenterAboutAppointment(bloodCenter, bloodCenterAccount, donor, donation);
        emailService.notifyBloodCenterAboutAppointment(bloodCenter, bloodCenterAccount, donor, donation);
    }
}