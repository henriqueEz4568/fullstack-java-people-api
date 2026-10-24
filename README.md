# people-api

API de cadastro de pessoas em Java 21 + Spring Boot, com PostgreSQL.

## Pré-requisitos

- Java 21
- Docker e Docker Compose
- `make` (para os atalhos abaixo)

## Banco de dados

O banco roda em um container PostgreSQL definido no [`docker-compose.yml`](docker-compose.yml).

### Subir o banco

```bash
make docker:db:up
```

Ou, sem `make`:

```bash
docker compose up -d --wait db
```

O comando só termina quando o banco estiver pronto para receber conexões.

### Dados de conexão

| Campo    | Valor                                     |
|----------|-------------------------------------------|
| Host     | `localhost`                               |
| Porta    | `5432`                                    |
| Banco    | `people`                                  |
| Usuário  | `people`                                  |
| Senha    | `people`                                  |
| JDBC URL | `jdbc:postgresql://localhost:5432/people` |

Para abrir um terminal SQL dentro do container:

```bash
docker exec -it people-api-db psql -U people -d people
```

### Migrations

Os scripts em [`db/migrations`](db/migrations) são montados no volume `/docker-entrypoint-initdb.d` do container e executados em ordem alfabética **apenas na primeira vez** que o banco é criado (quando o volume `db-data` ainda está vazio).

Por isso, ao adicionar ou alterar uma migration, é preciso recriar o banco:

```bash
make docker:db:reset
```

> Atenção: `docker:db:reset` apaga o volume e todos os dados do banco.

### Comandos disponíveis

| Comando                | O que faz                                         |
|------------------------|---------------------------------------------------|
| `make docker:db:up`    | Sobe o banco e aguarda ficar pronto               |
| `make docker:db:down`  | Para o banco (os dados são mantidos no volume)    |
| `make docker:db:reset` | Apaga o volume e sobe o banco rodando as migrations do zero |

## Rodando a API

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Rotas

| Método | Rota                                  |
|--------|---------------------------------------|
| POST   | `/registrarName`                      |
| GET    | `/list`                               |
| GET    | `/list/{document}`                    |
| DELETE | `/list/{id}`                          |
| GET    | `/findNacionalityByPerson/{document}` |
