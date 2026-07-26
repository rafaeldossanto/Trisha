---
tags: [servico, backend]
porta: 8083
banco: trilha_midia
repo: midia
atualizado: 2026-07-25
---

# Serviço Mídia

**Porta 8083 · banco `trilha_midia` · pacote `com.trisha.midia`**

Guarda os binários (fotos) no **MinIO** e os metadados no Postgres. É o menor
serviço do sistema — uma entidade, quatro endpoints.

## Endpoints (`/arquivo`)

| Método | Rota | Observação |
|---|---|---|
| POST | `/upload` | multipart; `?tipo=FOTO`; devolve a URL |
| GET | `/{id}` | metadados |
| DELETE | `/{id}` | **só o dono** (403 caso contrário) |

Limite de upload: **50 MB** por arquivo e por requisição.

## Entidade

`MediaFile` — nome original, nome armazenado, tipo, content-type, tamanho, bucket,
url, **`proprietario_id`**, `criado_em`, `trace_id`.

## O front sobe direto aqui

> [!important] Este serviço é exposto ao front, fora do [[BFF]]
> O upload **não** passa pelo BFF (evita carregar o binário duas vezes). Duas
> consequências:
> 1. validar o Bearer aqui é essencial — não há borda protegendo;
> 2. o **CORS** precisa liberar a origem do front web, igual ao BFF. Ver
>    [[Segurança]].

## MinIO e as URLs presigned

O serviço devolve uma **presigned URL** com validade de 7 dias. Isso tem um efeito
colateral que já causou bug: a assinatura vai na *query string* e a URL passa de
255 caracteres. Por isso `Region.coverUrl` no [[Serviço APP]] é
`VARCHAR(1024)`.

## Tratamento de erro

O `MinioService` usa `@SneakyThrows` e **propaga o erro real** do MinIO. Antes ele
envelopava em `RuntimeException("Erro ao enviar arquivo ao MinIO")`, o que
mascarava a causa. Agora a exceção original chega ao `GlobalExceptionHandler`, que
loga o stack completo e devolve 500.

## Dependências

Postgres · MinIO

Consumido diretamente pelos fronts ([[App Mobile]], [[App Web]]) para upload, e pelo
[[BFF]] para listagem.
