package com.trail.Cadastro.controller;

import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.request.UserUpdateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.exception.ForbiddenException;
import com.trail.Cadastro.service.RegistrationService;
import com.trail.Cadastro.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Objects.isNull;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RegistrationService registrationService;

    @PostMapping
    public UserDTO create(@RequestBody @Valid UserCreateRequest request) {
        return registrationService.register(request);
    }

    @PutMapping("/{id}")
    public UserDTO update(@AuthenticationPrincipal Jwt jwt,
                          @RequestBody @Valid UserUpdateRequest request,
                          @PathVariable String id) {
        ensureSelf(jwt, id);
        return userService.update(request, id);
    }

    @GetMapping("/{id}")
    public UserDTO getById(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable String id) {
        ensureSelf(jwt, id);
        return userService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt,
                       @PathVariable String id) {
        ensureSelf(jwt, id);
        userService.delete(id);
    }

    /**
     * O subject do token e o id interno da conta: qualquer id diferente na rota e
     * uma tentativa de operar a conta de outro usuario (IDOR). Recusa antes de
     * qualquer consulta, sem revelar se o id alvo existe.
     */
    private void ensureSelf(Jwt jwt, String id) {
        if (isNull(jwt) || !jwt.getSubject().equals(id)) {
            throw new ForbiddenException("Voce so pode acessar a propria conta");
        }
    }
}
