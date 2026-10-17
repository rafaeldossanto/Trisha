package com.trail.Cadastro.repository;

import com.trail.Cadastro.entity.EmailConfirmation;
import com.trail.Cadastro.model.enums.ConfirmationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, String> {
    Optional<EmailConfirmation> findByToken(String token);
    Optional<EmailConfirmation> findFirstByUserIdOrderBySentAtDesc(String userId);
    void deleteByUserId(String userId);

    /** Metade da condicao de ativacao (a outra e o aceite dos termos). */
    boolean existsByUserIdAndStatus(String userId, ConfirmationStatus status);
}
