package com.trisha.cadastro.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ConfirmationController {

    private final ActivationService activationService;
    private final ConfirmationEmailService confirmationEmailService;

    @GetMapping("/confirmar-email")
    public String confirmEmail(@RequestParam String token) {
        activationService.confirmEmail(token);
        return "Email confirmado com sucesso";
    }

    @PostMapping("/reenviar-email")
    public String resendEmail(@RequestParam("usuarioId") String userId) {
        confirmationEmailService.resendConfirmation(userId);
        return "Email reenviado com sucesso";
    }

    @PostMapping("/aceitar-termos")
    public String acceptTerms(@RequestParam("usuarioId") String userId) {
        activationService.acceptTerms(userId);
        return "Termos aceitos com sucesso";
    }
}
