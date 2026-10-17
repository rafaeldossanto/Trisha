package com.app.APP.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;


@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor
@Immutable
public class User {

    @Id
    private String id;

    @Column(name = "nome")
    private String name;

    private String email;

    @Column(name = "codigo_usuario")
    private String userCode;
}
