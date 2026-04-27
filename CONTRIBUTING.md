# Contributing to RAF Framework

## Prerequisites

- JDK 17+
- Maven 3.8.8+

## Build and Test

```bash
mvn clean test --no-transfer-progress
```

## Branch Naming

- `feature/<name>`
- `fix/<name>`
- `docs/<name>`
- `test/<name>`

## Commit Convention

Use Conventional Commits:

- `feat:`
- `fix:`
- `docs:`
- `test:`
- `refactor:`

## Testing Requirements

- New starter/configuration behavior must include `ApplicationContextRunner` tests.
- Behavior changes must include at least one failing-first test.

## Pull Request Process

1. Fork repository.
2. Create branch.
3. Add tests and docs.
4. Open PR with template completed.
