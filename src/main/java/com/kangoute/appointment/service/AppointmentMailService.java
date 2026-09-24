package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.Appointment;
import com.kangoute.appointment.enums.AppointmentStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentMailService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final ApplicationEventPublisher events;
    private final ObjectProvider<JavaMailSender> senderProvider;
    private final QrCodeService qrCodeService;

    @Value("${app.mail.enabled:false}") private boolean enabled;
    @Value("${app.mail.from:}") private String from;
    @Value("${app.frontend.url:}") private String frontendUrl;

    public void schedule(Appointment appointment) {
        if (appointment.getPublicReference() == null || appointment.getVerificationToken() == null) return;
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.CANCELLED) return;
        events.publishEvent(new MailEvent(
                appointment.getContactEmail(), appointment.getContactFirstName(),
                appointment.getPublicReference(), appointment.getVerificationToken(),
                appointment.getStartDateTime(), appointment.getReason(), appointment.getStatus()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(MailEvent event) {
        if (!enabled) {
            log.info("Appointment email not sent: mail is disabled");
            return;
        }
        try {
            JavaMailSender sender = senderProvider.getIfAvailable();
            if (sender == null || from == null || from.isBlank() || frontendUrl == null || frontendUrl.isBlank()) {
                throw new IllegalStateException("Mail configuration is incomplete");
            }
            String verifyUrl = frontendUrl.replaceAll("/+$", "")
                    + "/verify-appointment?token=" + event.token();
            String subject = switch (event.status()) {
                case PENDING -> "Demande de rendez-vous enregistrée — " + event.reference();
                case CONFIRMED -> "Rendez-vous confirmé — " + event.reference();
                case CANCELLED -> "Rendez-vous annulé — " + event.reference();
                default -> throw new IllegalStateException("Unsupported mail status");
            };
            String statusText = switch (event.status()) {
                case PENDING -> "En attente de confirmation";
                case CONFIRMED -> "Confirmé";
                case CANCELLED -> "Annulé";
                default -> "";
            };
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(event.contactEmail());
            helper.setSubject(subject);
            String body = "<p>Bonjour " + HtmlUtils.htmlEscape(event.firstName()) + ",</p>"
                    + "<p>" + (event.status() == AppointmentStatus.PENDING
                    ? "Votre demande de rendez-vous a bien été enregistrée."
                    : event.status() == AppointmentStatus.CONFIRMED
                    ? "Votre rendez-vous est confirmé." : "Votre rendez-vous a été annulé.") + "</p>"
                    + "<p>Référence : " + HtmlUtils.htmlEscape(event.reference()) + "<br>Date et heure : "
                    + event.startDateTime().format(DATE_FORMAT) + "<br>Motif : "
                    + HtmlUtils.htmlEscape(event.reason()) + "<br>Statut : " + statusText + "</p>"
                    + "<p><a href=\"" + HtmlUtils.htmlEscape(verifyUrl) + "\">Consulter mon rendez-vous</a></p>"
                    + "<p><img src=\"cid:appointmentQr\" alt=\"QR code du rendez-vous\"></p>";
            helper.setText(body, true);
            helper.addInline("appointmentQr", new ByteArrayResource(qrCodeService.generatePng(verifyUrl)), "image/png");
            sender.send(message);
        } catch (Exception exception) {
            // The transaction has already committed. Never log recipient data or message contents.
            log.warn("Appointment email could not be sent: {}", exception.getClass().getSimpleName());
        }
    }

    public record MailEvent(String contactEmail, String firstName, String reference, String token,
                            LocalDateTime startDateTime, String reason, AppointmentStatus status) { }
}
