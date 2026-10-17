package com.trail.Cadastro.mapper;

import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthenticationMapper {

    public AuthenticationResponse toResponse(User user, IssuedToken token) {
        return AuthenticationResponse.builder()
                .user(UserMapper.toResponse(user))
                .accessToken(token.value())
                .expiresInSeconds(token.expiresInSeconds())
                .build();
    }
}
