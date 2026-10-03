# Banking Test Automation Framework

A Spring Boot banking application with JWT-based authentication, account management, and transfer workflows, complemented by a lightweight browser UI and automated API tests.

## Features

- User registration and login with JWT authentication
- Protected account creation and retrieval routes
- Funds transfer and transaction history endpoints
- H2 in-memory database for local execution
- Simple static dashboard for browser-based interaction
- TestNG + Rest Assured API validation suite

## Run locally

```bash
cd automation-framework
mvn spring-boot:run
```

Then open:

- http://localhost:8080/

## Test suite

The application must be running before the API suite executes.

```bash
cd automation-framework
mvn spring-boot:run
```

In a second terminal:

```bash
cd automation-framework
mvn test
```

## Project structure

- `src/main/java/com/banking` — backend application code
- `src/main/resources/static` — browser UI assets
- `src/test/java/com/banking/api` — API automation tests

## Notes

The project is designed as a demo banking platform and QA automation exercise, with a front-end dashboard that calls the same API used by the backend test suite.
