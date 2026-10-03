# Test strategy

## Objectives and coverage

The suite focuses on validating the main customer journeys and the API contracts that support them:

- **Authentication API:** registration and login, JWT responses and protected access, plus invalid or missing registration data, duplicate email, and invalid credentials.
- **Accounts API:** account creation and retrieval, account types and balances, invalid account data, missing authorization, and unknown account IDs.
- **Transactions API:** transfers, balance changes, transaction history, and validation for insufficient funds and invalid amounts or accounts.
- **Browser end-to-end:** registration and the default checking account, successful and failed login, displaying API-created accounts, creating an account from the dashboard, and completing a transfer with updated balances and activity.

API tests use REST Assured to check HTTP status codes and response data. Selenium tests use Page Objects and exercise user-visible behavior in Chrome. Registration, account creation, and transfer journeys are specified as Gherkin features and run through Cucumber's TestNG integration; login behavior remains in the direct TestNG UI tests. Cucumber writes `target/e2e-report/cucumber.html`, including screenshots attached when a scenario fails. The TestNG listener generates `target/e2e-report/index.html` for the remaining UI tests.

## Test data and environment

Run the Spring Boot application on port 8080 before running `mvn test`. It can run locally with the default in-memory H2 database or with Docker Compose and PostgreSQL, as described in the README. Tests connect to the application at `http://localhost:8080` and can create users, accounts, and transactions. Restarting the local H2 app clears its data; Docker Compose uses persistent PostgreSQL storage, so test data may remain between runs.

Test users use synthetic names, fixed test-only passwords, and unique email addresses (timestamp-based for API tests and UUID-based for UI fixtures) to avoid collisions. API tests create their own users; UI tests use the test API helper to prepare accounts where a scenario needs pre-existing data. No real customer or banking data is required. Test records are not individually deleted, so use a disposable local H2 database or Docker database rather than a shared or production database.

The default run opens Chrome visibly. Add `-Dui.headless=true` for headless execution. UI browser and fixture API URLs can be configured with `-Dui.baseUrl=...` and `-Dapi.baseUrl=...`; the API test base URL is `http://localhost:8080/api`.

## Scope and exclusions

This is a functional regression suite for the demo application, not a production certification. It does not currently validate real bank/payment integrations, load or concurrency behavior, cross-browser compatibility beyond Chrome, full accessibility conformance, production database migration/operations, or provide a comprehensive security assessment. Passing these tests does not establish production readiness or suitability for real financial data.
