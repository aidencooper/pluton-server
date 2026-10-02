package net.aidencooper.pluton_server.security.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class EmailSenderServiceTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> greenmail = new GenericContainer<>("greenmail/standalone:2.1.3")
        .withExposedPorts(3025, 3143)
        .withEnv("GREENMAIL_OPTS",
            "-Dgreenmail.setup.test.smtp -Dgreenmail.setup.test.imap " +
            "-Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled");

    private EmailSenderService emailSenderService;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(greenmail.getHost());
        mailSender.setPort(greenmail.getMappedPort(3025));

        // Adjust if your EmailProperties record has more components than fromAddress
        EmailProperties emailProperties = new EmailProperties("test@pluton.dev");

        this.emailSenderService = new EmailSenderService(mailSender, emailProperties);
    }

    @Test
    void sendVerificationCode_deliversEmailWithCodeInBody() throws Exception {
        String toEmail = "test@test.com";
        this.emailSenderService.sendVerificationCode(toEmail, "123456");

        assertThat(fetchLastMessage(toEmail).getContent().toString()).contains("123456");
    }

    @Test
    void sendVerificationCode_setsCorrectRecipientAndSubject() throws Exception {
        String toEmail = "test@test.com";
        this.emailSenderService.sendVerificationCode(toEmail, "654321");

        Message last = fetchLastMessage(toEmail);
        assertThat(last.getSubject()).contains("verification code");
        assertThat(last.getAllRecipients()[0].toString()).isEqualTo(toEmail);
    }

    private Message fetchLastMessage(String toEmail) throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        Session session = Session.getInstance(props);

        try (Store store = session.getStore("imap")) {
            store.connect(greenmail.getHost(), greenmail.getMappedPort(3143), toEmail, "password");
            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_ONLY);
                Message[] messages = inbox.getMessages();
                assertThat(messages).isNotEmpty();

                // Copy the message so it stays readable after the folder closes
                return new MimeMessage((MimeMessage) messages[messages.length - 1]);
            }
        }
    }
}