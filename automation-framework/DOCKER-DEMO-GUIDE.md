# Docker demonstration video guide

This guide walks through recording the Northstar Bank demo with the application and PostgreSQL running in Docker Compose, then running the visible Selenium suite from the host and opening its E2E report.

## Before recording

Install Docker Desktop, Java 21, Maven, Google Chrome, and OBS Studio. Docker builds the React frontend inside the image, so Node.js/npm is not needed on the host for this demo. Selenium runs on the host to make its Chrome window visible; it resolves ChromeDriver automatically.

From the `automation-framework` directory, create the local environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace `DB_PASSWORD` and `APP_JWT_SECRET` with unique values. The JWT secret must be at least 32 characters. Keep `.env` and its values out of the recording; do not use real credentials or show secret values on screen.

## Screen and terminal layout

Use OBS Studio to record the full display. Arrange the windows before recording:

- Left: Terminal 1 for Docker Compose startup and application/database logs.
- Center: Terminal 2 for Maven test execution.
- Right: Chrome, initially showing the app and later the E2E report.

Make the terminal text large enough to read. Keep the browser beside or above the terminals, and avoid displaying `.env` or personal information.

In OBS, add a **macOS Screen Capture** source for the display, select the Desktop as the recording location in **Settings → Output → Recording**, and start recording before entering the commands. If macOS asks for permission, enable OBS in **System Settings → Privacy & Security → Screen & System Audio Recording**, then reopen OBS.

## Demo flow

### 1. Start the Docker app and database

In Terminal 1, from the project directory:

```bash
cd /Users/shahdaddy/Documents/test-automation-framework/automation-framework
docker compose up --build
```

Wait for PostgreSQL to become healthy and Spring Boot to finish starting. Leave this terminal running; the app is available at <http://localhost:8080/>.

### 2. Show the application

Open <http://localhost:8080/> in Chrome. Register a synthetic demo user or sign in with a demo account, then briefly show the dashboard and account/transfer UI. Do not use real personal or financial data.

### 3. Run the automated suite visibly

In Terminal 2, from the project directory, run:

```bash
cd /Users/shahdaddy/Documents/test-automation-framework/automation-framework
mvn test
```

The suite runs the REST Assured API tests and opens Chrome for Selenium UI tests. Keep Terminal 1 running. The UI fixture helper creates synthetic users and accounts through the app API. The tests use the same `localhost:8080` app served by Docker; they do not start another copy of the application.

Wait for Maven to finish and show the final test summary. On success, Maven reports `BUILD SUCCESS`.

### 4. Open the generated report

After Maven finishes, in Terminal 2 or another terminal:

```bash
open target/e2e-report/index.html
```

Show the result summary and a few flow screenshots from the report. The report and screenshots are generated locally under `target/e2e-report/`.

### 5. Stop services and finish the recording

Stop Docker Compose with **Control-C** in Terminal 1. To stop services later from the project directory, use:

```bash
docker compose down
```

This preserves the named PostgreSQL volume and its test data. To reset the disposable demo database completely, run `docker compose down -v`; this permanently deletes the `banking-data` volume, so only do this if its contents are not needed.

Stop recording in OBS and verify the video was saved to the selected Desktop location.

## Suggested narration

“I’m starting the banking application and PostgreSQL with Docker Compose. The dashboard is served by the container, and I’ll use the browser to walk through the customer experience.”

“Now I’m running the API and browser-based end-to-end tests against the Dockerized app. Selenium opens Chrome and checks the registration, login, account, and transfer flows.”

“The suite completed successfully. I’m opening the generated E2E report to review the results and screenshots from those user journeys.”
