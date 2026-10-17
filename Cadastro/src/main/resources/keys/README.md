# Chaves RSA — Cadastro

## dev-private-key.pem

Par RSA 2048-bit em formato PKCS#8, **exclusivo para desenvolvimento local**.
Este arquivo está intencionalmente versionado num repositório **público** — ou seja,
**não é um segredo**: qualquer pessoa pode baixá-lo e forjar tokens de qualquer
usuário em uma instância que o esteja usando.

Por isso o `JwtKeyConfig` é **fail closed**: esta chave só é carregada quando um
profile de desenvolvimento (`dev` ou `test`) está **explicitamente ativo**. Em
qualquer outro caso — inclusive quando nenhum profile está ativo — o boot aborta
exigindo `JWT_RSA_PRIVATE_KEY_PATH`.

A regra está travada por teste em `JwtKeyConfigTest`. Se você rodar o serviço
localmente sem profile, ele vai falhar de propósito: use
`SPRING_PROFILES_ACTIVE=dev`.

## Produção

1. Gere um par RSA seguro (ex.: `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out prod-key.pem`)
2. Armazene em um Secret Manager (AWS Secrets Manager, GCP Secret Manager, Kubernetes Secret, etc.)
3. Monte o arquivo no container e defina `JWT_RSA_PRIVATE_KEY_PATH=/caminho/para/prod-key.pem`

O `kid` é derivado do SHA-256 da chave pública e permanece estável entre restarts —
desde que a mesma chave seja usada, tokens em circulação continuam válidos após deploys.
