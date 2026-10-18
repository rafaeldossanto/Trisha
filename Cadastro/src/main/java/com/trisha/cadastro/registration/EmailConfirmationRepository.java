package com.trisha.cadastro.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, String> {
    Optional<EmailConfirmation> findByToken(String token);
    Optional<EmailConfirmation> findFirstByUserIdOrderBySentAtDesc(String userId);

    /** Metade da condicao de ativacao (a outra e o aceite dos termos). */
    boolean existsByUserIdAndStatus(String userId, ConfirmationStatus status);
}
