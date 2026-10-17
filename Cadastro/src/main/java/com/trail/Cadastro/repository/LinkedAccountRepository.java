package com.trail.Cadastro.repository;

import com.trail.Cadastro.entity.LinkedAccount;
import com.trail.Cadastro.model.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, String> {

    Optional<LinkedAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    List<LinkedAccount> findByUserId(String userId);

    boolean existsByUserIdAndProvider(String userId, AuthProvider provider);
}
