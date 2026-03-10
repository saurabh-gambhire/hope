package com.hope.master_service.modules.user;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.USState;
import com.hope.master_service.dto.enums.UserStatus;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.*;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/master/users")
@RequiredArgsConstructor
public class UserController extends AppController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Response> login(@Valid @RequestBody LoginRequest request) throws HopeException {
        LoginResponse loginResponse = userService.login(request);
        return data(ResponseCode.OK, "Login successful", loginResponse);
    }

    @GetMapping
    public ResponseEntity<Response> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ACTIVE") UserStatus status,
            @RequestParam(required = false) List<Roles> roles,
            @RequestParam(required = false) Instant lastLoginFrom,
            @RequestParam(required = false) Instant lastLoginTo,
            @RequestParam(required = false) Boolean neverLoggedIn) {
        Page<User> users = userService.search(
                search, status, roles, lastLoginFrom, lastLoginTo, neverLoggedIn,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(users);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<Response> getStatusCounts() {
        return data(userService.getStatusCounts());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(userService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody User user) throws HopeException {
        User created = userService.create(user);
        return data(ResponseCode.USER_CREATED, messageService.getMessage(ResponseCode.USER_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid, @Valid @RequestBody User user) throws HopeException {
        User updated = userService.update(uuid, user);
        return data(ResponseCode.UPDATE_USER_PROFILE_RESPONSE,
                messageService.getMessage(ResponseCode.UPDATE_USER_PROFILE_RESPONSE), updated);
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID uuid, @RequestParam boolean active) throws HopeException {
        userService.updateStatus(uuid, active);
        return success(active ? ResponseCode.USER_ENABLED : ResponseCode.USER_DISABLED);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid, @RequestParam boolean archive) throws HopeException {
        userService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.USER_ARCHIVED : ResponseCode.USER_UNARCHIVED);
    }

    @PatchMapping("/{uuid}/reset-password")
    public ResponseEntity<Response> resetPassword(@PathVariable UUID uuid,
                                                  @Valid @RequestBody ChangePasswordRequest request) throws HopeException {
        userService.resetPassword(uuid, request.getNewPassword());
        return success(ResponseCode.CHANGE_PASSWORD_RESPONSE);
    }

    @PostMapping("/{uuid}/resend-invite")
    public ResponseEntity<Response> resendInvitation(@PathVariable UUID uuid) throws HopeException {
        userService.resendInvitation(uuid);
        return success(ResponseCode.RESEND_INVITE_EMAIL_RESPONSE);
    }

    @PostMapping("/activate")
    public ResponseEntity<Response> activateAccount(@RequestParam UUID token,
                                                    @Valid @RequestBody ChangePasswordRequest request) throws HopeException {
        userService.activateAccount(token, request.getNewPassword());
        return success(ResponseCode.SET_PASSWORD_RESPONSE);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Response> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) throws HopeException {
        String otp = userService.forgotPassword(request.getEmail());
        return data(ResponseCode.OTP_SENT, messageService.getMessage(ResponseCode.OTP_SENT), otp);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Response> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) throws HopeException {
        UUID resetToken = userService.verifyOtp(request.getEmail(), request.getOtp());
        return data(ResponseCode.OTP_VERIFIED,
                messageService.getMessage(ResponseCode.OTP_VERIFIED),
                VerifyOtpResponse.builder().resetToken(resetToken).build());
    }

    @PostMapping("/set-password")
    public ResponseEntity<Response> setPassword(@Valid @RequestBody SetPasswordRequest request) throws HopeException {
        userService.setPasswordWithToken(request.getResetToken(), request.getNewPassword(), request.getConfirmPassword());
        return success(ResponseCode.SET_PASSWORD_RESPONSE);
    }

    @PostMapping("/logout")
    public ResponseEntity<Response> logout(@Valid @RequestBody LogoutRequest request) throws HopeException {
        userService.logout(request.getRefreshToken());
        return success(ResponseCode.LOGOUT_RESPONSE);
    }

    @GetMapping("/us-states")
    public ResponseEntity<Response> getUSStates() {
        List<Map<String, String>> states = Arrays.stream(USState.values())
                .map(s -> Map.of("state", s.getStateName(), "stateCode", s.name()))
                .collect(Collectors.toList());
        return data(states);
    }

}
