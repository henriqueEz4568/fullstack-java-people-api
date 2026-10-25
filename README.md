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
| `NATIONALIZE_URL` | `https://api.nationalize.io`          |
| `CORS_ALLOWED_ORIGINS` | vazio (CORS desligado)           |

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

Rodar só os testes ponta a ponta das rotas:

```bash
./mvnw test -Dtest='PersonControllerIntegrationTest*'
```

No IntelliJ, clique na seta verde ao lado da classe ou do método de teste (`Ctrl+Shift+F10` com o cursor dentro dele).

| Teste                                                                                              | Tipo                                           |
|----------------------------------------------------------------------------------------------------|------------------------------------------------|
| [`PersonRepositoryTest`](src/test/java/com/example/peopleapi/repository/PersonRepositoryTest.java) | Integração do repository (`@DataJpaTest`), um grupo por método |
| [`PeopleApiApplicationTests`](src/test/java/com/example/peopleapi/PeopleApiApplicationTests.java) | Sobe o contexto completo da aplicação           |
| [`SecurityConfigTest`](src/test/java/com/example/peopleapi/security/SecurityConfigTest.java)       | Autenticação por API key (`@WebMvcTest`)       |
| [`CorsConfigTest`](src/test/java/com/example/peopleapi/security/CorsConfigTest.java)               | CORS: origens permitidas, curingas, preflight e origem bloqueada |
| [`PersonControllerIntegrationTest`](src/test/java/com/example/peopleapi/controller/PersonControllerIntegrationTest.java) | Ponta a ponta com banco real: todas as rotas, validação de entrada, tratamento de erros e API key (a API externa de nacionalidade é simulada) |
| [`NationalityServiceTest`](src/test/java/com/example/peopleapi/nationality/NationalityServiceTest.java) | Unitário: escolha do país mais provável e conversão do código ISO |
| [`NationalizeClientTest`](src/test/java/com/example/peopleapi/nationality/NationalizeClientTest.java) | Cliente HTTP do nationalize.io com servidor simulado (`@RestClientTest`) |

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

## CORS

CORS só é necessário quando um front-end roda em **outra origem** (outro domínio ou porta, por exemplo um React em `http://localhost:5173`) e chama a API pelo navegador. Por padrão ele fica **desligado**: o navegador bloqueia chamadas de outras origens.

Para liberar, informe as origens permitidas separadas por vírgula na variável `CORS_ALLOWED_ORIGINS`:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173 ./mvnw spring-boot:run
```

Várias origens e curingas também são aceitos:

```bash
CORS_ALLOWED_ORIGINS="http://localhost:5173,https://*.meudominio.com" ./mvnw spring-boot:run
```

Para liberar qualquer origem (apenas em desenvolvimento):

```bash
CORS_ALLOWED_ORIGINS="*" ./mvnw spring-boot:run
```

Com CORS ligado, a API aceita os métodos `GET`, `POST`, `DELETE` e `OPTIONS`, os headers `Content-Type` e `X-API-Key` (necessário para o `DELETE` autenticado) e expõe o header `Location` do cadastro. Requisições de origens fora da lista recebem `403`.

No IntelliJ, adicione `CORS_ALLOWED_ORIGINS=http://localhost:5173` em **Run → Edit Configurations... → Environment variables**.

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

| Método | Rota                                  | Sucesso | Erros possíveis |
|--------|---------------------------------------|---------|-----------------|
| POST   | `/registrarName`                      | `201`   | `400`, `409`    |
| GET    | `/list`                               | `200`   | —               |
| GET    | `/list/{document}`                    | `200`   | `400`, `404`    |
| DELETE | `/list/{id}` 🔒 (API key)             | `204`   | `400`, `401`, `404` |
| GET    | `/findNacionalityByPerson/{document}` | `200`   | `400`, `404`, `502` |

### Validações

| Rota | Campo | Regra |
|------|-------|-------|
| `POST /registrarName` | `document` | obrigatório, exatamente 11 dígitos numéricos |
| `POST /registrarName` | `firstName`, `lastName` | obrigatórios, não podem ser vazios, máximo 100 caracteres |
| `POST /registrarName` | `email` | obrigatório, formato de e-mail válido, máximo 255 caracteres |
| `GET /list/{document}` | `document` | exatamente 11 dígitos numéricos |
| `DELETE /list/{id}` | `id` | número inteiro maior que zero |
| `GET /findNacionalityByPerson/{document}` | `document` | exatamente 11 dígitos numéricos |

### Erros

Todos os erros seguem o formato padrão [Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457) com `Content-Type: application/problem+json`. Erros de validação trazem o campo `errors` com a mensagem de cada campo inválido:

```json
{
  "status": 400,
  "title": "Dados inválidos",
  "detail": "Um ou mais campos são inválidos",
  "instance": "/registrarName",
  "errors": {
    "document": "deve conter exatamente 11 dígitos numéricos",
    "email": "deve ser um e-mail válido"
  }
}
```

| Status | Quando |
|--------|--------|
| `400`  | Campo inválido, JSON malformado/ausente ou parâmetro com tipo errado |
| `401`  | `DELETE` sem API key ou com chave inválida |
| `404`  | Pessoa não encontrada, ou a API externa não conseguiu prever a nacionalidade |
| `409`  | Já existe uma pessoa com o mesmo documento |
| `502`  | Falha ao consultar a API externa de nacionalidade |

### Nacionalidade

`GET /findNacionalityByPerson/{document}` busca a pessoa pelo documento, consulta o primeiro nome dela em [nationalize.io](https://nationalize.io/), escolhe o país de maior probabilidade e converte o código ISO para o nome do país em português.

A chamada HTTP fica em [`NationalizeClient`](src/main/java/com/example/peopleapi/nationality/NationalizeClient.java) e a regra de escolha/conversão em [`NationalityService`](src/main/java/com/example/peopleapi/nationality/NationalityService.java); o controller apenas orquestra.

### Exemplos

```bash
curl -X POST http://localhost:8080/registrarName \
  -H "Content-Type: application/json" \
  -d '{"document": "12345678901", "firstName": "Nathaniel", "lastName": "Smith", "email": "nathaniel@email.com"}'
```

```bash
curl http://localhost:8080/list
```

```bash
curl http://localhost:8080/list/12345678901
```

```bash
curl http://localhost:8080/findNacionalityByPerson/12345678901
```

```json
{"document": "12345678901", "firstName": "Nathaniel", "countryCode": "US", "countryName": "Estados Unidos", "probability": 0.357622}
```

```bash
curl -X DELETE http://localhost:8080/list/1 -H "X-API-Key: dev-api-key"
```
