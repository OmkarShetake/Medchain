package com.medchain.service;

import com.medchain.config.JwtUtil;
import com.medchain.dto.request.LoginRequest;
import com.medchain.dto.request.RegisterRequest;
import com.medchain.dto.request.UpdateProfileRequest;
import com.medchain.dto.response.AuthResponse;
import com.medchain.dto.response.UserDto;
import com.medchain.entity.Manufacturer;
import com.medchain.entity.RefreshToken;
import com.medchain.entity.User;
import com.medchain.exception.DuplicateResourceException;
import com.medchain.exception.ResourceNotFoundException;
import com.medchain.exception.UnauthorizedException;
import com.medchain.exception.ValidationException;
import com.medchain.repository.ManufacturerRepository;
import com.medchain.repository.RefreshTokenRepository;
import com.medchain.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Validate manufacturer specific fields
        if ("MANUFACTURER".equals(request.getRole())) {
            if (request.getCompanyName() == null || request.getLicenseNumber() == null) {
                throw new ValidationException("Company name and license number are required for manufacturers");
            }
            if (manufacturerRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new DuplicateResourceException("License number already registered");
            }
        }

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.valueOf(request.getRole()))
                .phone(request.getPhone())
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create manufacturer record if role is MANUFACTURER
        if ("MANUFACTURER".equals(request.getRole())) {
            Manufacturer manufacturer = Manufacturer.builder()
                    .user(user)
                    .companyName(request.getCompanyName())
                    .licenseNumber(request.getLicenseNumber())
                    .gstNumber(request.getGstNumber())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .pincode(request.getPincode())
                    .isVerified(false)
                    .build();
            manufacturerRepository.save(manufacturer);
        }

        // Send welcome email (best-effort, never block registration)
        try {
            emailService.sendWelcomeEmail(user);
        } catch (Exception e) {
            log.warn("Welcome email could not be sent to {}: {}", user.getEmail(), e.getMessage());
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        // Find refresh token
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if expired
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Generate new access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(token.getUser().getEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(token.getUser()))
                .build();
    }

    @Transactional
    public void logout() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("User logged out: {}", email);
    }

    public UserDto getProfile() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToUserDto(user);
    }

    @Transactional
    public UserDto updateProfile(UpdateProfileRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", email);
        
        return mapToUserDto(user);
    }

    private void saveRefreshToken(User user, String token) {
        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.getId());

        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return authentication.getName();
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .isVerified(user.getIsVerified())
                .build();
    }

    @PostConstruct
    public void createDefaultUsers() {
        createUserIfNotExists("admin@medchain.com", "Admin User", "Admin@123", User.UserRole.ADMIN, true);
        createUserIfNotExists("patient@medchain.com", "Rahul Sharma", "Patient@123", User.UserRole.PATIENT, true);
        createUserIfNotExists("chemist@medchain.com", "Apollo Pharmacy", "Chemist@123", User.UserRole.CHEMIST, true);
        
        // Create manufacturer user with manufacturer record
        User mfrUser = createUserIfNotExists("manufacturer@medchain.com", "Cipla Pharmaceuticals", "Mfr@123", User.UserRole.MANUFACTURER, true);
        if (mfrUser != null && !manufacturerRepository.findByUserId(mfrUser.getId()).isPresent()) {
            Manufacturer manufacturer = Manufacturer.builder()
                    .user(mfrUser)
                    .companyName("Cipla Pharmaceuticals Ltd")
                    .licenseNumber("MH-MFG-2024-001")
                    .gstNumber("27AAACR5055K1Z4")
                    .isVerified(true)
                    .address("Cipla House, Peninsula Business Park")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400013")
                    .build();
            manufacturerRepository.save(manufacturer);
            log.info("Default manufacturer created");
        }
    }

    private User createUserIfNotExists(String email, String name, String password, User.UserRole role, boolean verified) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .isVerified(verified)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            log.info("Default user created: {}", email);
            return user;
        }
        return userRepository.findByEmail(email).orElse(null);
    }
}
