package bloodmatch.infra.external.notification;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailConfirmationMailer {

  private final JavaMailSender mailSender;
  private final String senderEmail;

  public EmailConfirmationMailer(
      ObjectProvider<JavaMailSender> mailSender,
      @Value("${spring.mail.username:}") String senderEmail) {
    this.mailSender = mailSender.getIfAvailable();
    this.senderEmail = senderEmail;
  }

  @Async
  public void sendConfirmationEmail(String recipientEmail, String recipientName, String confirmationUrl) {
    if (recipientEmail == null || recipientEmail.isBlank()
        || confirmationUrl == null || confirmationUrl.isBlank()) {
      return;
    }
    if (mailSender == null) {
      System.err.println("Skipping confirmation e-mail: JavaMailSender is not available");
      return;
    }

    String greetingName = (recipientName == null || recipientName.isBlank()) ? "" : recipientName.trim();

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(senderEmail);
      helper.setTo(recipientEmail);
      helper.setSubject("Confirme sua conta no BloodMatch");

      String htmlBody = String.format("""
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
              <div style="background-color: #d32f2f; color: white; padding: 20px; text-align: center;">
                  <h2 style="margin: 0;">BloodMatch</h2>
              </div>
              <div style="padding: 30px; color: #333; line-height: 1.6;">
                  <p style="font-size: 16px;">Olá%s!</p>
                  <p style="font-size: 16px;">Obrigado por se cadastrar no BloodMatch. Confirme seu e-mail para ativar sua conta e começar a usar a plataforma.</p>
                  <div style="text-align: center; margin-top: 30px; margin-bottom: 30px;">
                      <a href="%s" style="background-color: #d32f2f; color: white; text-decoration: none; padding: 14px 28px; border-radius: 6px; font-weight: bold; display: inline-block; font-size: 16px;">Confirmar e-mail</a>
                  </div>
                  <p style="font-size: 14px; color: #666;">Se o botão não funcionar, copie e cole este link no navegador:</p>
                  <p style="font-size: 14px; color: #666; word-break: break-all;">%s</p>
                  <p style="font-size: 14px; color: #666; border-top: 1px solid #eee; padding-top: 20px;">
                      Este link expira em 24 horas. Se você não criou uma conta, ignore este e-mail.<br>
                      <em>Equipe BloodMatch</em>
                  </p>
              </div>
          </div>
          """, greetingName.isEmpty() ? "" : ", <strong>" + greetingName + "</strong>", confirmationUrl, confirmationUrl);

      helper.setText(htmlBody, true);
      mailSender.send(message);
    } catch (Exception e) {
      System.err.println("Erro ao montar/enviar e-mail de confirmação para " + recipientEmail + ": " + e.getMessage());
    }
  }
}
