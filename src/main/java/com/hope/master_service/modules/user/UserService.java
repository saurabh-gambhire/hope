package com.hope.master_service.modules.user;

import com.hope.master_service.dto.enums.RoleType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.UserStatus;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.LoginRequest;
import com.hope.master_service.dto.user.LoginResponse;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.dto.user.UserStatusSummary;
import com.hope.master_service.entity.AddressEntity;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.service.AppService;
import com.hope.master_service.service.EmailService;
import com.hope.master_service.service.IamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserService extends AppService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationTokenRepository invitationTokenRepository;

    @Autowired
    private IamService iamService;

    @Autowired
    private PasswordResetOtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${invitation.token-expiry-hours:72}")
    private int tokenExpiryHours;

    @Value("${otp.expiry-minutes:15}")
    private int otpExpiryMinutes;

    @Value("${otp.max-attempts:5}")
    private int otpMaxAttempts;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${invitation.portal-link:http://localhost:5173}")
    private String portalLink;

    public LoginResponse login(LoginRequest request) throws HopeException {
        return iamService.login(request);
    }

    public Page<User> getAll(Pageable pageable) {
        return userRepository.findByArchiveFalse(pageable)
                .map(UserEntity::toDto);
    }

    public Page<User> search(String search, UserStatus status, List<Roles> roles,
                             Instant lastLoginFrom, Instant lastLoginTo,
                             Boolean neverLoggedIn, Pageable pageable) {
        return userRepository.findAll(
                UserSpecification.withFilters(search, status, roles, lastLoginFrom, lastLoginTo, neverLoggedIn),
                pageable
        ).map(UserEntity::toDto);
    }

    public UserStatusSummary getStatusCounts() {
        long total = userRepository.count();
        long active = userRepository.count(UserSpecification.isActive());
        long inactive = userRepository.count(UserSpecification.isInactive());
        long pending = userRepository.count(UserSpecification.isPending());
        long suspended = userRepository.count(UserSpecification.isSuspended());

        return UserStatusSummary.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .pending(pending)
                .suspended(suspended)
                .build();
    }

    public User getByUuid(UUID uuid) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));
        return entity.toDto();
    }

    @Transactional
    public User create(User user) throws HopeException {
        if (userRepository.existsByEmail(user.getEmail())) {
            throwError(ResponseCode.DUPLICATE_EMAIL_ERROR);
        }

        String iamId = iamService.createUser(user);

        UserEntity entity = UserEntity.fromDto(user);
        entity.setIamId(iamId);
        entity.setRoleType(resolveRoleType(user.getRole()));
        entity = userRepository.save(entity);

        log.info("Created user: {} with role: {}", entity.getEmail(), entity.getRole());

//        sendInvitation(entity);

        return entity.toDto();
    }

    @Transactional
    public User update(UUID uuid, User user) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));

        String existingRole = entity.getRole() != null ? entity.getRole().name() : null;

        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setMiddleName(user.getMiddleName());
        entity.setPhone(user.getPhone());
        entity.setGender(user.getGender());
        entity.setJobTitle(user.getJobTitle());
        entity.setBirthDate(user.getBirthDate());
        entity.setAddress(AddressEntity.updateEntity(entity.getAddress(), user.getAddress()));

        if (user.getRole() != null && !user.getRole().equals(entity.getRole())) {
            iamService.updateUserRole(entity.getIamId(), existingRole, user.getRole());
            entity.setRole(user.getRole());
            entity.setRoleType(resolveRoleType(user.getRole()));
        }

        entity = userRepository.save(entity);
        return entity.toDto();
    }

    @Transactional
    public void updateStatus(UUID uuid, boolean active) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));

        iamService.updateUserStatus(entity.getIamId(), active);
        entity.setActive(active);
        userRepository.save(entity);
    }

    @Transactional
    public void updateArchiveStatus(UUID uuid, boolean archive) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));

        if (archive) {
            iamService.updateUserStatus(entity.getIamId(), false);
            entity.setActive(false);
        }
        entity.setArchive(archive);
        userRepository.save(entity);
    }

    @Transactional
    public void updateLastLogin(String iamId) {
        userRepository.findByIamId(iamId).ifPresent(user -> {
            user.setLastLogin(Instant.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public void resetPassword(UUID uuid, String newPassword) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));
        iamService.resetPassword(entity.getIamId(), newPassword);
    }

    @Transactional
    public UserEntity createUserEntity(User user) throws HopeException {
        if (userRepository.existsByEmail(user.getEmail())) {
            throwError(ResponseCode.DUPLICATE_EMAIL_ERROR);
        }

        String iamId = iamService.createUser(user);

        UserEntity entity = UserEntity.fromDto(user);
        entity.setIamId(iamId);
        entity.setRoleType(resolveRoleType(user.getRole()));
        entity = userRepository.save(entity);

//        sendInvitation(entity);

        return entity;
    }

    @Transactional
    public void resendInvitation(UUID uuid) throws HopeException {
        UserEntity entity = userRepository.findByUuid(uuid)
                .orElseThrow(() -> throwException(ResponseCode.USER_NOT_FOUND));

        if (entity.isEmailVerified()) {
            throwError(ResponseCode.BAD_REQUEST, "User is already activated");
        }

        // Invalidate old tokens and send a new one
        invitationTokenRepository.invalidateAllTokensForUser(entity.getId());
//        sendInvitation(entity);

        log.info("Resent invitation to {}", entity.getEmail());
    }

    @Transactional
    public void activateAccount(UUID token, String newPassword) throws HopeException {
        InvitationTokenEntity invitation = invitationTokenRepository.findByToken(token)
                .orElseThrow(() -> throwException(ResponseCode.INVALID_PASSWORD_LINK));

        if (!invitation.isValid()) {
            throwError(ResponseCode.INVALID_PASSWORD_LINK,
                    invitation.isUsed() ? "This link has already been used" : "This link has expired");
        }

        UserEntity user = invitation.getUser();

        // Set password in Keycloak
        iamService.resetPassword(user.getIamId(), newPassword);

        // Mark user as verified and active
        user.setEmailVerified(true);
        user.setActive(true);
        userRepository.save(user);

        // Mark token as used
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);

        log.info("User {} activated their account", user.getEmail());
    }

    @Transactional
    public String forgotPassword(String email) throws HopeException {
        UserEntity entity = userRepository.findByEmail(email)
                .orElseThrow(() -> throwException(ResponseCode.USER_EMAIL_NOT_FOUND));

        if (!entity.isActive()) {
            throwError(ResponseCode.USER_INACTIVE);
        }

        // Invalidate any existing OTPs
        otpRepository.invalidateAllOtpsForUser(entity.getId());

        // Generate 6-digit OTP
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        PasswordResetOtpEntity otpEntity = PasswordResetOtpEntity.builder()
                .otp(otp)
                .user(entity)
                .expiresAt(Instant.now().plus(otpExpiryMinutes, ChronoUnit.MINUTES))
                .used(false)
                .verified(false)
                .attempts(0)
                .build();
        otpRepository.save(otpEntity);

        emailService.sendOtpEmail(entity.getEmail(), entity.getFirstName(), otp);
        log.info("OTP sent to {} with OTP : {}", entity.getEmail(), otp);
        return  otp;
    }

    @Transactional
    public UUID verifyOtp(String email, String otp) throws HopeException {
        UserEntity entity = userRepository.findByEmail(email)
                .orElseThrow(() -> throwException(ResponseCode.USER_EMAIL_NOT_FOUND));

        PasswordResetOtpEntity otpEntity = otpRepository.findLatestActiveOtpByUserId(entity.getId())
                .orElseThrow(() -> throwException(ResponseCode.OTP_EXPIRED));

        if (otpEntity.getAttempts() >= otpMaxAttempts) {
            otpEntity.setUsed(true);
            otpRepository.save(otpEntity);
            throwError(ResponseCode.OTP_MAX_ATTEMPTS);
        }

        if (otpEntity.isExpired()) {
            throwError(ResponseCode.OTP_EXPIRED);
        }

        if (!otpEntity.getOtp().equals(otp)) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            otpRepository.save(otpEntity);
            throwError(ResponseCode.OTP_INVALID);
        }

        // Mark OTP as verified (but not used yet — used after password is set)
        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);

        log.info("OTP verified for {}", email);
        return otpEntity.getResetToken();
    }

    @Transactional
    public void setPasswordWithToken(UUID resetToken, String newPassword, String confirmPassword) throws HopeException {
        if (!newPassword.equals(confirmPassword)) {
            throwError(ResponseCode.PASSWORDS_DO_NOT_MATCH);
        }

        PasswordResetOtpEntity otpEntity = otpRepository.findByResetToken(resetToken)
                .orElseThrow(() -> throwException(ResponseCode.OTP_RESET_TOKEN_INVALID));

        if (otpEntity.isUsed() || !otpEntity.isVerified()) {
            throwError(ResponseCode.OTP_RESET_TOKEN_INVALID);
        }

        if (otpEntity.isExpired()) {
            throwError(ResponseCode.OTP_EXPIRED);
        }

        UserEntity user = otpEntity.getUser();
        iamService.resetPassword(user.getIamId(), newPassword);

        // Mark OTP as used
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        log.info("Password reset for user {}", user.getEmail());
    }

    public void logout(String refreshToken) throws HopeException {
        iamService.logout(refreshToken);
    }

    private void sendInvitation(UserEntity entity) {
        try {
            UUID token = UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS);

            InvitationTokenEntity invitation = InvitationTokenEntity.builder()
                    .token(token)
                    .user(entity)
                    .expiresAt(expiresAt)
                    .used(false)
                    .build();
            invitationTokenRepository.save(invitation);

            String activationLink = portalLink + "/activate?token=" + token;
            String role = entity.getRole() != null ? entity.getRole().name() : "";

            emailService.sendInvitationEmail(entity.getEmail(), entity.getFirstName(), role, activationLink);

        } catch (Exception e) {
            // Email failure should not block user creation
            log.error("Failed to send invitation email to {}: {}", entity.getEmail(), e.getMessage());
        }
    }

    private RoleType resolveRoleType(Roles role) {
        if (role == null) return null;
        if (Roles.getProviderRoles().contains(role)) return RoleType.PROVIDER;
        if (role == Roles.PATIENT) return RoleType.PATIENT;
        return RoleType.STAFF;
    }
}
