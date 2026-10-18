package com.trisha.cadastro.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, String> {
    /** Metade da condicao de ativacao (a outra e a confirmacao do email). */
    boolean existsByUserIdAndAcceptedTrue(String userId);
}
