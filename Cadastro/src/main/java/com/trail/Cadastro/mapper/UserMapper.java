package com.trail.Cadastro.mapper;

import com.trail.Cadastro.auth.ProviderUserData;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.utils.GenerateUtil;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class UserMapper {

    public User toEntity(UserCreateRequest request, String encodedPassword, Long sequence) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name())
                .email(request.email())
                .password(encodedPassword)
                .userCode(GenerateUtil.makeCode(request.name(), sequence))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public User mapToReactivated(User user, UserCreateRequest request, String encodedPassword) {
        user.setName(request.name());
        user.setPassword(encodedPassword);
        user.setStatus(RegistrationStatus.PENDENTE);
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public User toEntitySocial(ProviderUserData data, String name, Long sequence) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .email(data.email())
                .userCode(GenerateUtil.makeCode(name, sequence))
                .status(RegistrationStatus.ATIVO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public User toEntityDev(String email, String name, Long sequence) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .userCode(GenerateUtil.makeCode(name, sequence))
                .status(RegistrationStatus.ATIVO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserDTO toResponse(User user) {
        return UserDTO.builder()
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
