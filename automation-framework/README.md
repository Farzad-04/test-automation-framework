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
- Selenium UI tests using Page Objects for registration, login, account creation, and transfers

## Run locally

Requires Java 21, Maven, Node.js 22+, and npm. Build the React dashboard once after cloning:

```bash
cd frontend
npm ci
npm run build
cd ..
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

## Run the API and UI tests

The API and Selenium UI suites expect the app to be running on port 8080. The UI suite opens a visible Chrome window by default and resolves ChromeDriver automatically. Set `-Dui.headless=true` to run without a visible window (as CI does). Start the app in one terminal:

```bash
mvn spring-boot:run
```

Then run the suite in a second terminal:

```bash
mvn test
```

Maven uses the root `testng.xml` suite, which runs the authentication, account management, and transaction API tests.
The same suite also runs the browser-based UI tests. Override their base URLs with `-Dui.baseUrl=http://localhost:8080/` and `-Dapi.baseUrl=http://localhost:8080/api` if needed.
By default, Selenium launches visible Chrome windows on your desktop. To run in headless mode instead:

```bash
mvn test -Dui.headless=true
```

After execution, open `target/e2e-report/index.html` for the E2E results and flow screenshots. Screenshots are saved alongside it in `target/e2e-report/screenshots/`.

## Project structure

- `src/main/java/com/banking` — Spring Boot API and application services
- `src/main/resources/static` — browser dashboard assets
- `frontend` — React dashboard source and Vite build configuration
- `src/main/resources/application-prod.properties` — PostgreSQL deployment profile
- `src/test/java/com/banking/api` — API automation tests
- `src/test/java/com/banking/ui` — Selenium Page Objects and UI tests
- `Dockerfile` and `docker-compose.yml` — container build and local deployment
