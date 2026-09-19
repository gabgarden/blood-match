package bloodmatch.shared.infrastructure.external.notification;

import bloodmatch.donation.domain.Donation;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.services.NotificationServiceInterface;
import bloodmatch.shared.domain.valueObjects.Address;

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

    @Override
    public void notifyBloodCenterAboutAppointment(
            BloodCenter bloodCenter,
            UserAccount bloodCenterAccount,
            Donor donor,
            Donation donation) {
        String centerName = bloodCenter.getOrganization().getName();
        String centerEmail = bloodCenterAccount.getEmail().getValue();
        String donorName = donor.getPerson().getName();
        String bloodType = donor.getBloodType().getType();
        String date = donation.getIntendedDate() == null ? "-" : donation.getIntendedDate().toString();
        String time = donation.getExpectedTime() == null
                ? "horário não informado"
                : donation.getExpectedTime().toString().substring(0, 5);

        System.out.println("=====================================================");
        System.out.println("[NOTIFICAÇÃO ENVIADA - NOVA MARCAÇÃO VIA BLOODMATCH]");
        System.out.println("Para: " + centerName + " (" + centerEmail + ")");
        System.out.println("Assunto: Nova marcação via BloodMatch");
        System.out.println("Doador: " + donorName);
        System.out.println("Tipo sanguíneo: " + bloodType);
        System.out.println("Data: " + date);
        System.out.println("Horário: " + time);
        System.out.println("=====================================================");
    }
}