package bloodmatch.infra.external.notification;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationServiceInterface {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    @Override
    public void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request) {
        String donorName = donor.getPerson().getName();
        String donorEmail = account.getEmail().getValue();
        String bloodCenterName = request.getBloodCenter().getOrganization().getName();
        String bloodType = donor.getBloodType().getType();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail); 
        message.setTo(donorEmail);
        message.setSubject("Precisamos de doadores de sangue " + bloodType + "!");
        
        String body = String.format(
            "Olá %s,\n\nO %s está precisando urgentemente de doações de sangue do tipo %s.\n\n" +
            "Acesse o sistema BloodMatch para agendar sua doação e ajudar a salvar vidas!\n\n" +
            "Equipe BloodMatch", 
            donorName, bloodCenterName, bloodType
        );
            
        message.setText(body);

        try {
            mailSender.send(message);
            System.out.println("E-mail enviado com sucesso para: " + donorEmail);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail para " + donorEmail + ": " + e.getMessage());
        }
    }
}