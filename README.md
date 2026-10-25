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

Com o banco no ar:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Variáveis de ambiente

A persistência usa Spring Data JPA (Hibernate). A conexão é configurada no [`application.properties`](src/main/resources/application.properties) com valores padrão iguais aos do `docker-compose.yml`, então para desenvolvimento local não é preciso definir nada.

| Variável      | Padrão                                    |
|---------------|-------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/people` |
| `DB_USER`     | `people`                                  |
| `DB_PASSWORD` | `people`                                  |
| `API_KEY`     | `dev-api-key`                             |

Para sobrescrever:

```bash
DB_URL=jdbc:postgresql://outro-host:5432/people DB_PASSWORD=segredo ./mvnw spring-boot:run
```

O schema é criado pelas migrations; o Hibernate roda com `ddl-auto=validate` e apenas confere se as entidades batem com as tabelas.

## Repository

O acesso ao banco é feito pela interface [`PersonRepository`](src/main/java/com/example/peopleapi/repository/PersonRepository.java), implementada com Spring Data JPA em [`PersonRepositoryImpl`](src/main/java/com/example/peopleapi/repository/PersonRepositoryImpl.java).

| Método                         | O que faz                                                                 |
|--------------------------------|---------------------------------------------------------------------------|
| `getAll()`                     | Lista todas as pessoas, ordenadas por id                                  |
| `getOne(PersonFilter where)`   | Retorna a primeira pessoa que atende ao filtro (campos nulos são ignorados) |
| `delete(Long id)`              | Exclui pelo id e retorna `true` se a pessoa existia                       |
| `create(Person person)`        | Persiste a pessoa e retorna com o id gerado; documento duplicado lança `DataIntegrityViolationException` |

```java
repository.getOne(PersonFilter.where().withDocument("12345678901"));
repository.getOne(PersonFilter.where().withFirstName("Ana").withLastName("Silva"));
```

**Transações:** seguindo o padrão do Spring, a transação não é passada como parâmetro. Os métodos de escrita são `@Transactional` com propagação `REQUIRED`: se quem chamou já tem uma transação aberta (por exemplo, um service anotado com `@Transactional`), o `delete`/`create` participa dela e é desfeito junto em caso de rollback; se não houver, abrem uma própria.

## Testes

Os testes usam [Testcontainers](https://testcontainers.com/): cada execução sobe um PostgreSQL descartável em Docker, aplica as migrations de `db/migrations` e o remove ao final. Por isso **é preciso ter o Docker rodando**, mas **não** é preciso subir o banco do `docker-compose` — os testes nunca tocam nos seus dados locais.

Rodar todos os testes:

```bash
./mvnw test
```

Rodar só os testes do repository:

```bash
./mvnw test -Dtest='PersonRepositoryTest*'
```

No IntelliJ, clique na seta verde ao lado da classe ou do método de teste (`Ctrl+Shift+F10` com o cursor dentro dele).

| Teste                                                                                              | Tipo                                           |
|----------------------------------------------------------------------------------------------------|------------------------------------------------|
| [`PersonRepositoryTest`](src/test/java/com/example/peopleapi/repository/PersonRepositoryTest.java) | Integração do repository (`@DataJpaTest`), um grupo por método |
| [`PeopleApiApplicationTests`](src/test/java/com/example/peopleapi/PeopleApiApplicationTests.java) | Sobe o contexto completo da aplicação           |
| [`SecurityConfigTest`](src/test/java/com/example/peopleapi/security/SecurityConfigTest.java)       | Autenticação por API key (`@WebMvcTest`)       |

## Autenticação

A rota `DELETE /list/{id}` é protegida por **API key**, enviada no header `X-API-Key`. As demais rotas são públicas.

A chave é lida da variável de ambiente `API_KEY` (padrão `dev-api-key` para desenvolvimento local). Em produção, defina sempre uma chave própria:

```bash
API_KEY=minha-chave-secreta ./mvnw spring-boot:run
```

Exemplo de chamada:

```bash
curl -X DELETE http://localhost:8080/list/1 -H "X-API-Key: dev-api-key"
```

| Situação                         | Resposta           |
|----------------------------------|--------------------|
| Sem o header `X-API-Key`         | `401 Unauthorized` |
| Com uma chave inválida           | `401 Unauthorized` |
| Com a chave correta              | Segue para a rota  |

A implementação usa Spring Security: o [`ApiKeyAuthenticationFilter`](src/main/java/com/example/peopleapi/security/ApiKeyAuthenticationFilter.java) valida o header e o [`SecurityConfig`](src/main/java/com/example/peopleapi/security/SecurityConfig.java) define quais rotas exigem autenticação.

## Rotas

| Método | Rota                                  |
|--------|---------------------------------------|
| POST   | `/registrarName`                      |
| GET    | `/list`                               |
| GET    | `/list/{document}`                    |
| DELETE | `/list/{id}` 🔒 (API key)             |
| GET    | `/findNacionalityByPerson/{document}` |
