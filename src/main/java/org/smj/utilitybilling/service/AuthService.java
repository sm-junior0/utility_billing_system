package org.smj.utilitybilling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.ForgotPasswordRequest;
import org.smj.utilitybilling.dto.request.LoginRequest;
import org.smj.utilitybilling.dto.request.ResendOtpRequest;
import org.smj.utilitybilling.dto.request.ResetPasswordRequest;
import org.smj.utilitybilling.dto.request.SignupRequest;
import org.smj.utilitybilling.dto.request.VerifyOtpRequest;
import org.smj.utilitybilling.dto.response.AuthResponse;
import org.smj.utilitybilling.dto.response.MessageResponse;
import org.smj.utilitybilling.dto.response.SignupResponse;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.OtpPurpose;
import org.smj.utilitybilling.enums.Role;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.smj.utilitybilling.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Auth business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpVerificationService otpVerificationService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered. If you have not verified the account, please request a new OTP.");
        }
        if (userAccountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        }

        UserAccount user = UserAccount.builder()
                .fullNames(request.getFullNames())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING_VERIFICATION)
                .role(Role.ROLE_CUSTOMER)
                .build();

        UserAccount saved = userAccountRepository.save(user);
        otpVerificationService.createAndSendOtp(saved.getEmail(), OtpPurpose.REGISTRATION);
        log.info("Signup completed userId={} email={}", saved.getId(), saved.getEmail());
        return new SignupResponse(
                saved.getId(),
                saved.getEmail(),
                "Signup successful. An OTP has been sent to your email for account verification. Submit your customer profile after verification.",
                LocalDateTime.now()
        );
    }

    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found for the provided email"));
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("This account is already verified. You can log in directly.");
        }

        otpVerificationService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.REGISTRATION);
        user.setStatus(UserStatus.ACTIVE);
        return MessageResponse.of("OTP verified successfully. Your account is now active and you can log in.");
    }

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found for the provided email"));
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("This account is already verified. Resending OTP is not allowed.");
        }

        otpVerificationService.createAndSendOtp(request.getEmail(), OtpPurpose.REGISTRATION);
        return MessageResponse.of("A new OTP has been sent to your email.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found for the provided email"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Only active users can request password reset");
        }

        otpVerificationService.createAndSendOtp(request.getEmail(), OtpPurpose.PASSWORD_RESET);
        return MessageResponse.of("A password reset OTP has been sent to your email.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found for the provided email"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Only active users can reset their password");
        }

        otpVerificationService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpPurpose.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        return MessageResponse.of("Password reset successfully. You can now log in with the new password.");
    }

    public MessageResponse logout() {
        return MessageResponse.of("Logout successful. Remove the JWT token from the client to complete logout.");
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            UserAccount disabledUser = userAccountRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));
            if (disabledUser.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new BadRequestException("Your account is not yet verified. Please verify your OTP before logging in.");
            }
            throw new BadRequestException("Inactive users cannot login");
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid credentials");
        }

        UserAccount user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Your account is not yet verified. Please verify your OTP before logging in.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("Inactive users cannot login");
        }

        String token = jwtService.generateToken(user);
        log.info("Login successful userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getRole());
    }
}
