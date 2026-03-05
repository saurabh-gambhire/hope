package com.hope.master_service.service;

import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.user.LoginRequest;
import com.hope.master_service.dto.user.LoginResponse;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.exception.HopeException;

import java.util.Optional;

public interface IamService {

    String createUser(User user) throws HopeException;

    Optional<User> findByEmail(String email) throws HopeException;

    LoginResponse login(LoginRequest request) throws HopeException;

    boolean resetPassword(String iamId, String newPassword) throws HopeException;

    void updateUserRole(String iamId, String existingRole, Roles newRole) throws HopeException;

    void updateUserStatus(String iamId, boolean enabled) throws HopeException;

    void deleteUser(String iamId) throws HopeException;

    void logout(String refreshToken) throws HopeException;

    void createRealm(String realmName);

    void updateRealmStatus(String realmName, boolean enabled);

    void deleteRealm(String realmName) throws HopeException;
}
