package com.medchain.service;

import com.medchain.entity.FakeReport;
import com.medchain.entity.Manufacturer;
import com.medchain.entity.Medicine;
import com.medchain.entity.Recall;
import com.medchain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to MedChain!");
            helper.setText(buildWelcomeEmail(user), true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendReportVerifiedEmail(Manufacturer manufacturer, Medicine medicine, FakeReport report) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(manufacturer.getUser().getEmail());
            helper.setSubject("⚠️ Fake Medicine Report Verified - " + medicine.getName());
            helper.setText(buildReportVerifiedEmail(manufacturer, medicine, report), true);

            mailSender.send(message);
            log.info("Report verified email sent to: {}", manufacturer.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send report verified email", e);
        }
    }

    @Async
    public void sendRecallAlertEmail(List<User> users, Recall recall) {
        for (User user : users) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(user.getEmail());
                helper.setSubject("🚨 URGENT: Medicine Recall Alert - " + recall.getMedicine().getName());
                helper.setText(buildRecallAlertEmail(user, recall), true);

                mailSender.send(message);
            } catch (MessagingException e) {
                log.error("Failed to send recall alert email to: {}", user.getEmail(), e);
            }
        }
        log.info("Recall alert emails sent to {} users", users.size());
    }

    @Async
    public void sendMedicineExpiryAlert(Manufacturer manufacturer, List<Medicine> medicines) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(manufacturer.getUser().getEmail());
            helper.setSubject("⏰ Medicine Expiry Alert - Action Required");
            helper.setText(buildExpiryAlertEmail(manufacturer, medicines), true);

            mailSender.send(message);
            log.info("Expiry alert email sent to: {}", manufacturer.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send expiry alert email", e);
        }
    }

    @Async
    public void sendManufacturerVerifiedEmail(Manufacturer manufacturer) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(manufacturer.getUser().getEmail());
            helper.setSubject("✅ Manufacturer Account Verified - MedChain");
            helper.setText(buildManufacturerVerifiedEmail(manufacturer), true);

            mailSender.send(message);
            log.info("Manufacturer verified email sent to: {}", manufacturer.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send manufacturer verified email", e);
        }
    }

    private String buildWelcomeEmail(User user) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #2563eb; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f9fafb; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to MedChain!</h1>
                        </div>
                        <div class="content">
                            <h2>Hello %s,</h2>
                            <p>Thank you for joining MedChain - India's AI-powered fake medicine detection platform.</p>
                            <p>Your account has been created successfully with the role: <strong>%s</strong></p>
                            <p>You can now:</p>
                            <ul>
                                <li>Verify medicines by scanning QR codes</li>
                                <li>Report suspicious medicines</li>
                                <li>Access AI-powered symptom checker</li>
                                <li>Stay updated on medicine recalls</li>
                            </ul>
                            <p>Visit <a href="http://localhost:3000">MedChain Platform</a> to get started.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 MedChain. Fighting fake medicines in rural India.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(user.getName(), user.getRole());
    }

    private String buildReportVerifiedEmail(Manufacturer manufacturer, Medicine medicine, FakeReport report) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #dc2626;">⚠️ Fake Medicine Report Verified</h2>
                        <p>Dear %s,</p>
                        <p>A fake medicine report for your product has been verified by our admin team.</p>
                        <h3>Medicine Details:</h3>
                        <ul>
                            <li><strong>Name:</strong> %s</li>
                            <li><strong>Batch Number:</strong> %s</li>
                            <li><strong>Location:</strong> %s, %s</li>
                        </ul>
                        <h3>Report Details:</h3>
                        <p>%s</p>
                        <p><strong>AI Confidence Score:</strong> %d%%</p>
                        <p>Please take immediate action to investigate this matter.</p>
                        <p>Best regards,<br>MedChain Team</p>
                    </div>
                </body>
                </html>
                """.formatted(
                manufacturer.getCompanyName(),
                medicine.getName(),
                medicine.getBatchNumber(),
                report.getCity(),
                report.getState(),
                report.getDescription(),
                report.getAiConfidenceScore()
        );
    }

    private String buildRecallAlertEmail(User user, Recall recall) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 3px solid #dc2626;">
                        <h2 style="color: #dc2626;">🚨 URGENT: Medicine Recall Alert</h2>
                        <p>Dear %s,</p>
                        <p>A medicine recall has been issued with <strong>%s</strong> severity.</p>
                        <h3>Medicine Details:</h3>
                        <ul>
                            <li><strong>Name:</strong> %s</li>
                            <li><strong>Generic Name:</strong> %s</li>
                            <li><strong>Batch Number:</strong> %s</li>
                        </ul>
                        <h3>Recall Reason:</h3>
                        <p>%s</p>
                        <p style="background: #fee2e2; padding: 15px; border-left: 4px solid #dc2626;">
                            <strong>Action Required:</strong> If you have this medicine, please stop using it immediately and return it to your nearest pharmacy.
                        </p>
                        <p>For more information, visit the MedChain platform.</p>
                        <p>Stay safe,<br>MedChain Team</p>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getName(),
                recall.getSeverity(),
                recall.getMedicine().getName(),
                recall.getMedicine().getGenericName(),
                recall.getMedicine().getBatchNumber(),
                recall.getReason()
        );
    }

    private String buildExpiryAlertEmail(Manufacturer manufacturer, List<Medicine> medicines) {
        StringBuilder medicineList = new StringBuilder();
        for (Medicine medicine : medicines) {
            medicineList.append(String.format("<li>%s (Batch: %s) - Expires: %s</li>",
                    medicine.getName(), medicine.getBatchNumber(), medicine.getExpiryDate()));
        }

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #d97706;">⏰ Medicine Expiry Alert</h2>
                        <p>Dear %s,</p>
                        <p>The following medicines are expiring soon:</p>
                        <ul>%s</ul>
                        <p>Please take necessary action to manage inventory and prevent distribution of expired medicines.</p>
                        <p>Best regards,<br>MedChain Team</p>
                    </div>
                </body>
                </html>
                """.formatted(manufacturer.getCompanyName(), medicineList.toString());
    }

    private String buildManufacturerVerifiedEmail(Manufacturer manufacturer) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #16a34a;">✅ Manufacturer Account Verified</h2>
                        <p>Dear %s,</p>
                        <p>Congratulations! Your manufacturer account has been verified by MedChain.</p>
                        <p>You can now:</p>
                        <ul>
                            <li>Register medicines</li>
                            <li>Generate QR codes</li>
                            <li>Track medicine distribution</li>
                            <li>Receive fake medicine reports</li>
                        </ul>
                        <p>Login to your dashboard to get started.</p>
                        <p>Best regards,<br>MedChain Team</p>
                    </div>
                </body>
                </html>
                """.formatted(manufacturer.getCompanyName());
    }
}
