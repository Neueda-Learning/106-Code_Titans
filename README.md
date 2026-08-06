# __Code_Titans

## Project Overview

__Code_Titans is a payment processing system that manages account data, executes payment workflows, and provides analytics-style reporting dashboards.

The platform is split into:
- A Spring Boot backend that exposes REST APIs for accounts, payments, payment lifecycle updates, audit history, and reports.
- An Nginx-hosted frontend (static HTML/CSS/JS) that consumes backend APIs to render dashboards and operational views.

Core business capabilities include:
- Account management and lookup
- Payment creation and status transitions
- Payment history/audit tracking
- Reporting views (status breakdown, top senders/receivers, failures, trends)

## Tech Stack

- Backend: Java 17, Spring Boot, Spring Web MVC, Spring Data JDBC
- Database: MySQL 8
- Frontend hosting: Nginx (serving static UI from backend/nginx)
- Build tool: Maven Wrapper (mvnw/mvnw.cmd)
- API documentation: SpringDoc OpenAPI UI
- Containerization: Docker and Docker Compose

## Project Architecture

Backend source lives under [backend/src/main/java/com/neueda/__Code_Titans](backend/src/main/java/com/neueda/__Code_Titans).

- controller: REST endpoints for accounts, payments, and reports
	- [backend/src/main/java/com/neueda/__Code_Titans/controller](backend/src/main/java/com/neueda/__Code_Titans/controller)
- service: business logic, payment validation, status transitions, settlement, and reporting logic
	- [backend/src/main/java/com/neueda/__Code_Titans/service](backend/src/main/java/com/neueda/__Code_Titans/service)
- repo: data access using JdbcTemplate and SQL queries
	- [backend/src/main/java/com/neueda/__Code_Titans/repo](backend/src/main/java/com/neueda/__Code_Titans/repo)
- entity: table-mapped domain models for accounts, payments, and payment history
	- [backend/src/main/java/com/neueda/__Code_Titans/entity](backend/src/main/java/com/neueda/__Code_Titans/entity)
- config: infrastructure configuration such as CORS
	- [backend/src/main/java/com/neueda/__Code_Titans/config](backend/src/main/java/com/neueda/__Code_Titans/config)

Database initialization scripts:
- [backend/src/main/resources/schema.sql](backend/src/main/resources/schema.sql)
- [backend/src/main/resources/data.sql](backend/src/main/resources/data.sql)

Frontend static app:
- [backend/nginx](backend/nginx)

## Getting Started

### Prerequisites

- Java 17+
- Maven (optional if using wrapper)
- Docker Desktop (for containerized run)
- MySQL (only for local non-Docker run)

### Option A: Run with Maven (Local)

1. Move into the backend module:

```powershell
cd backend
```

2. Configure database connection:
- Update active profile and DB settings in [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)
- Use local profile values from [backend/src/main/resources/application-dev.properties](backend/src/main/resources/application-dev.properties) if needed.

3. Build and run tests:

```powershell
.\mvnw.cmd clean test
```

4. Start the Spring Boot app:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend default URL:
- http://localhost:8082

Frontend (if served separately) expects backend on port 8082.

### Option B: Run with Docker Compose

From [backend](backend):

```powershell
cd backend
docker compose up --build
```

This starts:
- MySQL on port 3306
- Spring Boot API on port 8082
- Nginx frontend on port 8085

Application URLs:
- Frontend: http://localhost:8085
- Backend API: http://localhost:8082

To stop:

```powershell
docker compose down
```

## Testing

JUnit tests are located under:
- [backend/src/test/java/com/neueda/__Code_Titans](backend/src/test/java/com/neueda/__Code_Titans)

Test coverage includes controller, repository, and service layers.

Run test suite:

```powershell
cd backend
.\mvnw.cmd test
```

## Useful Paths

- Backend module root: [backend](backend)
- Docker Compose file: [backend/docker-compose.yml](backend/docker-compose.yml)
- Backend Dockerfile: [backend/Dockerfile](backend/Dockerfile)
- Nginx Dockerfile: [backend/nginx/Dockerfile](backend/nginx/Dockerfile)

