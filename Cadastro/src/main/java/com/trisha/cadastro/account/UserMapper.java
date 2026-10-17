package com.trisha.cadastro.account;

import com.trisha.cadastro.account.dto.UserResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class UserMapper {

    /**
     * Unico ponto que monta um usuario novo. O cadastro por senha nasce PENDENTE;
     * o social e o dev-login nascem ATIVOS e sem senha ({@code encodedPassword} nulo).
     */
    public User toEntity(String name, String email, String encodedPassword,
                         RegistrationStatus status, Long sequence) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .password(encodedPassword)
                .userCode(UserCode.of(name, sequence))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
