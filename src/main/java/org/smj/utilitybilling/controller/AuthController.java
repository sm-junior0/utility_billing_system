package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.smj.utilitybilling.dto.request.ForgotPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.LoginRequest;
import org.smj.utilitybilling.dto.request.ResendOtpRequest;
import org.smj.utilitybilling.dto.request.ResetPasswordRequest;
import org.smj.utilitybilling.dto.request.SignupRequest;
import org.smj.utilitybilling.dto.request.VerifyOtpRequest;
import org.smj.utilitybilling.dto.response.AuthResponse;
import org.smj.utilitybilling.dto.response.MessageResponse;
import org.smj.utilitybilling.dto.response.SignupResponse;
import org.smj.utilitybilling.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing Auth API endpoints.
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "01 - Authentication", description = "Public signup, OTP verification, resend OTP, and login APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Step 1: Register a public customer account",
            description = "Public endpoint. Creates a user as ROLE_CUSTOMER with PENDING_VERIFICATION status, encrypts the password with BCrypt, generates a 6-digit OTP, sends it to the email address, and returns the new userId. The billing customer profile is submitted after OTP verification.")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/verify-otp")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Step 2: Verify registration OTP",
            description = "Public endpoint. Activates a PENDING_VERIFICATION user account when the submitted OTP is correct, not expired, and not already used.")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Resend OTP for an unverified account",
            description = "Public endpoint. Expires old OTP records and sends a new OTP only if the user is still not verified.")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Request password reset OTP",
            description = "Public endpoint. Sends a one-time password reset OTP to an ACTIVE user's email address.")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Reset password with OTP",
            description = "Public endpoint. Verifies the password reset OTP and replaces the stored BCrypt password.")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            tags = "01 - Authentication",
            summary = "Step 3: Login and receive JWT",
            description = "Public endpoint. Authenticates only ACTIVE users and returns a JWT containing the username/email and role. After an admin role change, the user must log in again to receive a token with updated permissions.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            tags = "01 - Authentication",
            summary = "Logout current user",
            description = "Protected convenience endpoint. Since JWT authentication is stateless, the client must discard the token after calling this route.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(authService.logout());
    }
}
