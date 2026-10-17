package com.trail.Cadastro.repository;

import com.trail.Cadastro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);

    @Query(value = "SELECT nextval('usuario_codigo_seq')", nativeQuery = true)
    Long nextSequence();

    /**
     * Ativa a conta em um unico UPDATE condicional, devolvendo quantas linhas
     * foram afetadas (0 ou 1). Confirmar email e aceitar termos podem chegar
     * simultaneos — de dispositivos diferentes, ja que o link do email costuma
     * ser aberto fora do app — e um ler-decidir-gravar na aplicacao ativaria
     * duas vezes ou nenhuma. Aqui quem arbitra e o banco: so a primeira
     * transacao encontra o status PENDENTE.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
               SET u.status = com.trail.Cadastro.model.enums.RegistrationStatus.ATIVO,
                   u.updatedAt = :now
             WHERE u.id = :id
               AND u.status = com.trail.Cadastro.model.enums.RegistrationStatus.PENDENTE
            """)
    int activateIfPending(@Param("id") String id, @Param("now") LocalDateTime now);

    /**
     * Cadastros pendentes que estouraram o prazo de confirmacao.
     *
     * O corte usa 'updatedAt', nao 'createdAt': um cadastro reaproveitado por
     * conta INATIVA (UserMapper.mapToReactivated) volta para PENDENTE mantendo
     * a data de criacao original, e por createdAt seria expirado no primeiro
     * ciclo apos o recadastro.
     */
    @Query("""
            SELECT u.id FROM User u
             WHERE u.status = com.trail.Cadastro.model.enums.RegistrationStatus.PENDENTE
               AND u.updatedAt < :limit
            """)
    List<String> findExpiredPendingIds(@Param("limit") LocalDateTime limit);
}
