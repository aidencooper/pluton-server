package net.aidencooper.pluton_server.security.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service 
public class EmailSenderService {
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailSenderService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(this.emailProperties.fromAddress());
        email.setTo(to);
        email.setSubject("Your Pluton verification code");
        email.setText("Your verification code is: " + code);

        this.mailSender.send(email);
    }
}
