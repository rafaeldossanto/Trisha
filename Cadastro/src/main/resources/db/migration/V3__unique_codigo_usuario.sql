-- codigoUsuario ja e unico por construcao (nome + sequence atomica), mas faltava
-- a trava no banco. Agora que amizade/seguidor resolvem codigo -> id, garantimos
-- a unicidade (o unique index tambem serve aos lookups por codigo). Idempotente
-- e guardado por to_regclass: em banco novo o V4 ja cria a constraint junto.
DO $$
BEGIN
    IF to_regclass('usuario') IS NOT NULL
            AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_usuario_codigo') THEN
        ALTER TABLE usuario ADD CONSTRAINT uk_usuario_codigo UNIQUE (codigo_usuario);
    END IF;
END $$;
