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

## CI

Todo pull request roda o workflow [`pre-check`](.github/workflows/pre-check.yml) no GitHub Actions, que garante que a branch continua estável antes do merge:

1. Configura o Java 21 (com cache das dependências do Maven)
2. Sobe o banco com `docker compose`
3. **Build:** `./mvnw clean compile`
4. **Test:** `./mvnw test`
5. **Install:** `./mvnw install -DskipTests`

Se algum passo falhar, o check fica vermelho no PR e os relatórios dos testes ficam disponíveis para download em **Actions → execução → Artifacts → `surefire-reports`**. Novos commits no mesmo PR cancelam a execução anterior.

Para exigir que o check passe antes do merge, ative em **Settings → Branches → Branch protection rules** (branch `main`) a opção **Require status checks to pass before merging** e selecione `Build, test and install`.

## Rotas

| Método | Rota                                  |
|--------|---------------------------------------|
| POST   | `/registrarName`                      |
| GET    | `/list`                               |
| GET    | `/list/{document}`                    |
| DELETE | `/list/{id}`                          |
| GET    | `/findNacionalityByPerson/{document}` |
