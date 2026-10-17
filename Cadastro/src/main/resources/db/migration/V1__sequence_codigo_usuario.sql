-- Sequence atomica para gerar o numero do codigoUsuario (nome + numero).
-- Substitui o antigo COUNT(u)+1, que tinha race condition: dois cadastros
-- simultaneos podiam gerar o mesmo numero e, portanto, codigoUsuario duplicado.
-- nextval('usuario_codigo_seq') e atomico no PostgreSQL.
CREATE SEQUENCE IF NOT EXISTS usuario_codigo_seq START WITH 1 INCREMENT BY 1;
