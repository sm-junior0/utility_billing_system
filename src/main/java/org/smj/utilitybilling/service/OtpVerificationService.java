package org.smj.utilitybilling.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.entity.OtpVerification;
import org.smj.utilitybilling.enums.OtpPurpose;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing OtpVerification business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OtpVerificationService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Transactional
    public void createAndSendOtp(String email, OtpPurpose purpose) {
        expireOldOtps(email, purpose);

        String otpCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .build();
        otpVerificationRepository.save(otpVerification);
        sendOtpEmail(email, otpCode, purpose);
    }

    @Transactional
    public void expireOldOtps(String email, OtpPurpose purpose) {
        List<OtpVerification> activeOtps = otpVerificationRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
        activeOtps.forEach(otp -> otp.setUsed(true));
    }

    @Transactional
    public void verifyOtp(String email, String otpCode, OtpPurpose purpose) {
        OtpVerification otpVerification = otpVerificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new BadRequestException("No OTP was found for this email. Please request a new OTP."));

        if (otpVerification.isUsed()) {
            throw new BadRequestException("This OTP has already been used. Please request a new OTP.");
        }
        if (!otpVerification.getOtpCode().equals(otpCode)) {
            throw new BadRequestException("Invalid OTP code.");
        }
        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        otpVerification.setUsed(true);
    }

    private void sendOtpEmail(String email, String otpCode, OtpPurpose purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailUsername != null && !mailUsername.isBlank()) {
                message.setFrom(mailUsername);
            }
            message.setTo(email);
            message.setSubject(resolveEmailSubject(purpose));
            message.setText(resolveEmailBody(otpCode, purpose));
            mailSender.send(message);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to send OTP email. Please check Gmail app password, internet access, and SMTP settings. Reason: " + ex.getMessage());
        }
    }

    private String resolveEmailSubject(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> "Utility Billing Registration OTP";
            case PASSWORD_RESET -> "Utility Billing Password Reset OTP";
        };
    }

    private String resolveEmailBody(String otpCode, OtpPurpose purpose) {
        String action = purpose == OtpPurpose.REGISTRATION
                ? "registration"
                : "password reset";
        return "Your " + action + " OTP is: " + otpCode + ". It expires in " + otpExpirationMinutes + " minutes.";
    }
}
