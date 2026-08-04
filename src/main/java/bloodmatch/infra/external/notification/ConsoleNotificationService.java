package bloodmatch.infra.external.notification;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;

import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificationService implements NotificationServiceInterface {

    @Override
    public void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request) {
        String donorName = donor.getPerson().getName();
        String donorEmail = account.getEmail().getValue();
        String bloodCenterName = request.getBloodCenter().getOrganization().getName();
        String bloodType = donor.getBloodType().getType();

        Address addr1 = donor.getPerson().getAddress();
        Address addr2 = request.getBloodCenter().getOrganization().getAddress();
        double distance = Math.round(addr1.distanceTo(addr2) * 10.0) / 10.0;

        System.out.println("=====================================================");
        System.out.println("[NOTIFICAÇÃO ENVIADA - SIMULAÇÃO PUSH/EMAIL]");
        System.out.println("Para: " + donorName + " (" + donorEmail + ")");
        System.out.println("Assunto: Precisamos de doadores de sangue " + bloodType + "!");
        System.out.println("Mensagem: Olá " + donorName + ", o " + bloodCenterName + " está precisando de doações. Acesse o sistema para agendar!");
        System.out.println("Distância até o local: " + distance + "km.");
        System.out.println("=====================================================");
    }
}