package bloodmatch.infra.external.notification;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificationService implements NotificationServiceInterface {

    @Override
    public void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request) {
        String donorName = donor.getPerson().getName();
        String donorEmail = account.getEmail().getValue();
        String bloodCenterName = request.getBloodCenter().getOrganization().getName();
        String bloodType = donor.getBloodType().getType();

        System.out.println("=====================================================");
        System.out.println("[NOTIFICAÇÃO ENVIADA - SIMULAÇÃO PUSH/EMAIL]");
        System.out.println("Para: " + donorName + " (" + donorEmail + ")");
        System.out.println("Assunto: Precisamos de doadores de sangue " + bloodType + "!");
        System.out.println("Mensagem: Olá " + donorName + ", o " + bloodCenterName + 
                           " está precisando de doações. Acesse o sistema para agendar!");
        System.out.println("=====================================================");
    }
}