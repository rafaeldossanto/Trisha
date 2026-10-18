package com.trisha.cadastro.authentication.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, String> {

    Optional<LinkedAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
