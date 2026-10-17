package com.trisha.cadastro.authentication;

import com.trisha.cadastro.account.User;
import com.trisha.cadastro.account.UserMapper;
import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.token.IssuedToken;
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
