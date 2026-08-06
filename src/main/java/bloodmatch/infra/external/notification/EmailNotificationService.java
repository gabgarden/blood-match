package bloodmatch.infra.external.notification;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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

    @Async
    @Override
    public void notifyDonorAboutRequest(Donor donor, UserAccount account, DonationRequest request) {
        String donorName = donor.getPerson().getName();
        String donorEmail = account.getEmail().getValue();
        String bloodCenterName = request.getBloodCenter().getOrganization().getName();
        String bloodType = donor.getBloodType().getType();

        Address addr1 = donor.getPerson().getAddress();
        Address addr2 = request.getBloodCenter().getOrganization().getAddress();
        double distance = Math.round(addr1.distanceTo(addr2) * 10.0) / 10.0;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(donorEmail);
            helper.setSubject("🩸 Urgente: Precisamos de doadores de sangue " + bloodType + "!");

            String htmlBody = String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                        <div style="background-color: #d32f2f; color: white; padding: 20px; text-align: center;">
                            <h2 style="margin: 0;">Alerta BloodMatch</h2>
                        </div>
                        <div style="padding: 30px; color: #333; line-height: 1.6;">
                            <p style="font-size: 16px;">Olá, <strong>%s</strong>!</p>
                            <p style="font-size: 16px;">O <strong>%s</strong> está precisando urgentemente de doações de sangue do tipo <strong>%s</strong>.</p>
                            <p style="font-size: 16px;">Como você está próximo(a) à nossa unidade, você foi selecionado(a) para este alerta. Sua doação pode salvar até 4 vidas!</p>
                            <p style="font-size: 16px;">Distância até o local: <strong>%s Km</strong>.</p>
                            
                            <div style="text-align: center; margin-top: 30px; margin-bottom: 30px;">
                                <a href="" style="background-color: #d32f2f; color: white; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: bold; display: inline-block; font-size: 16px;">Agendar Minha Doação</a>
                            </div>
                            
                            <p style="font-size: 14px; color: #666; border-top: 1px solid #eee; padding-top: 20px;">
                                Agradecemos por ser um herói!<br>
                                <em>Equipe BloodMatch</em>
                            </p>
                        </div>
                    </div>
                    """, donorName, bloodCenterName, bloodType, distance);

            helper.setText(htmlBody, true); 

            mailSender.send(message);
            System.out.println("E-mail HTML enviado em background para: " + donorEmail);
            
        } catch (MessagingException e) {
            System.err.println("Erro ao montar/enviar e-mail para " + donorEmail + ": " + e.getMessage());
        }
    }
}