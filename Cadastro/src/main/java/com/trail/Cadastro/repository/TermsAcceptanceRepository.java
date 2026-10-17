package com.trail.Cadastro.repository;

import com.trail.Cadastro.entity.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, String> {
    void deleteByUserId(String userId);

    /** Metade da condicao de ativacao (a outra e a confirmacao do email). */
    boolean existsByUserIdAndAcceptedTrue(String userId);
}
