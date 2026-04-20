# Testing Strategy

## Scope

- Backend only (Spring Boot)
- Frontend testing is not in scope at this time

---

## Test Types

### Unit Tests

- Test individual classes in isolation
- Use JUnit 5 and Mockito
- Mock dependencies (repositories, other services)
- Focus on service layer business logic

### Integration Tests

- Test layers working together with a running Spring context
- Use `@SpringBootTest` for full context tests
- Use `@WebMvcTest` for controller layer tests
- Use H2 in-memory database instead of MySQL
- Verify end-to-end request/response flow, including JSON structure (`Result` wrapper)

---

## Test Database

- Use H2 in-memory database for all tests
- Configure via `application-test.yml` or `application-test.properties`
- Spring profile: `test`
- Let JPA auto-generate the schema (`spring.jpa.hibernate.ddl-auto=create-drop`)

---

## What to Test

| Layer      | Test Type   | What to Verify                                      |
|------------|-------------|-----------------------------------------------------|
| Service    | Unit        | Business logic, validation, edge cases, exceptions  |
| Repository | Integration | Custom queries, data persistence                    |
| Controller | Integration | Request mapping, response format, status codes      |

---

## Naming Convention

- Test class: `{ClassName}Test` for unit tests, `{ClassName}IntegrationTest` for integration tests
- Test method: `should_ExpectedResult_When_Condition`
- Example: `should_ThrowNotFoundException_When_UserIdNotFound`

---

## Coverage Guidelines

- Service layer: aim for high coverage (business logic is the priority)
- Controller layer: cover happy path and key error scenarios
- Repository layer: cover custom query methods only (skip standard CRUD)

---

## Running Tests

```bash
./mvnw test
```
