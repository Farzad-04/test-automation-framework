# Northstar Bank · Test Automation Framework

A Spring Boot banking demo with JWT authentication, account management, transfers, a responsive browser dashboard, and a TestNG/Rest Assured API suite.

> This is an educational demo, not a real banking service. Do not use it for real money or sensitive personal information.

## Features

- JWT-protected account and transfer APIs
- Sign-in and registration from the web dashboard
- Account balances, recent transfer activity, and account creation
- Local H2 mode for quick development
- Docker Compose deployment with PostgreSQL persistence
- TestNG + Rest Assured API tests

## Run locally

Requires Java 21 and Maven.

```bash
mvn spring-boot:run
```

Open <http://localhost:8080/> and register a demo user. The default H2 database is in-memory, so its data resets when the application restarts.

## Run with Docker Compose

Requires Docker with the Compose plugin. From this directory, create a local environment file and set unique secrets:

```bash
cp .env.example .env
```

Replace `DB_PASSWORD` and `APP_JWT_SECRET` in `.env` with generated values (for example, use `openssl rand -base64 32` separately for each). Then start the app and database:

```bash
docker compose up --build -d
```

Open <http://localhost:8080/>. PostgreSQL data is kept in the named `banking-data` volume across restarts. Stop the services with `docker compose down`; keep the volume to preserve data. **Do not use sample secrets or expose this demo publicly without reviewing its security and operational requirements.**

## Run the API tests

The API tests expect the app to be running on port 8080. Start the app in one terminal:

```bash
mvn spring-boot:run
```

Then run the suite in a second terminal:

```bash
mvn test
```

Maven uses the root `testng.xml` suite, which runs the authentication, account management, and transaction API tests.

## Project structure

- `src/main/java/com/banking` — Spring Boot API and application services
- `src/main/resources/static` — browser dashboard assets
- `src/main/resources/application-prod.properties` — PostgreSQL deployment profile
- `src/test/java/com/banking/api` — API automation tests
- `Dockerfile` and `docker-compose.yml` — container build and local deployment
