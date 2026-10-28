# ReleaseGuard

**Software Supply Chain Security, SBOM Auditing, and Release Governance Platform**

ReleaseGuard provides centralized governance, dependency tracking, vulnerability auditing, and deployment traceability across versioned releases.

---

## 1. Project Overview & Governance Boundaries

ReleaseGuard tracks:
- **Applications & Ownership**: Registered software applications, git repositories, and designated owner teams.
- **Releases**: Versioned releases linked to Git commit SHAs, Docker image references, and content digests.
- **CycloneDX SBOM Import**: Ingestion and validation of CycloneDX JSON 1.6 Software Bills of Materials (up to 10 MiB / 10,000 components).
- **Dependency Comparison**: Deterministic side-by-side release dependency comparison based on Package URL (purl) identity (`ADDED`, `REMOVED`, `VERSION_CHANGED`, `UNCHANGED`, `UNCERTAIN`).
- **Vulnerability Findings**: Ingestion of structured vulnerability reports linked to component BOM refs, validated against SBOM checksums, and attributed to the current application owner.
- **Release Approval Policy (`release-policy-v1`)**:
  - Attached vulnerability scan must be $\le 7$ days old.
  - Zero `CRITICAL`, `HIGH`, `UNKNOWN`, or missing severity data permitted.
  - `MEDIUM`, `LOW`, and `INFORMATIONAL` findings permitted with review.
  - Fresh scan with 0 reported findings satisfies approval.
- **Deployment Traceability & Atomic Rollbacks**:
  - Preserved deployment history across `DEVELOPMENT`, `STAGING`, and `PRODUCTION`.
  - Atomic pointer updates using pessimistic write locks.
  - Rollback recording with `expectedCurrentDeploymentId` to prevent concurrent stale rollbacks.
  - Production deployments and rollbacks enforce approval evidence and re-evaluate scan freshness.

> **Important Governance Boundaries:**
> 1. **Recording vs Execution**: Deployment and rollback APIs record audit evidence and history; they do not directly trigger cloud infrastructure deployments.
> 2. **Vulnerability Imports**: ReleaseGuard ingests structured scan reports; it does not directly run vulnerability scanners.
> 3. **Actor Identity**: `actor` and `deployedBy` fields are caller-supplied audit identifiers; enterprise OAuth2/OIDC authentication is not yet integrated.
> 4. **Synthetic Findings**: Demonstration vulnerability findings (`DEMO-001`) are synthetic test data, not claims of live vulnerabilities.

---

## 2. Architecture & Tech Stack

```
                     ┌─────────────────────────────────────────┐
                     │          Browser / UI Client            │
                     └────────────────────┬────────────────────┘
                                          │ Port 3000
                                          ▼
                     ┌─────────────────────────────────────────┐
                     │            Frontend (Nginx)             │
                     │    React 18 + Vite 5 + TypeScript       │
                     └────────────────────┬────────────────────┘
                                          │ Reverse Proxy (/api/*)
                                          ▼ Port 8080
                     ┌─────────────────────────────────────────┐
                     │          Backend (Spring Boot)          │
                     │          Java 21 / Spring MVC           │
                     └────────────────────┬────────────────────┘
                                          │ Port 5432 (Internal)
                                          ▼
                     ┌─────────────────────────────────────────┐
                     │           PostgreSQL 17.11              │
                     │        Persistent Data Volume           │
                     └─────────────────────────────────────────┘
```

- **Backend**: Java 21, Spring Boot 4.1.1, Spring Data JPA, Hibernate, Flyway, PostgreSQL 17.11, Testcontainers 2.0.5, PackageURL-Java 1.5.0, Jackson 3.
- **Frontend**: React 18, TypeScript, Vite 5, Tailwind CSS, Lucide React, Vitest, Testing Library.
- **Infrastructure**: Docker multi-stage builds, Docker Compose, Nginx, Jenkins Declarative Pipeline, SonarQube Scanner.

---

## 3. Quickstart with Docker Compose

### Prerequisites
- Docker Desktop (with Docker Compose enabled)
- Git

### 1. Configure Environment
Clone the repository and initialize your local environment file:
```powershell
Set-Location "$HOME\Projects\releaseguard"
Copy-Item .env.example .env
```
Edit `.env` and set a secure local password for PostgreSQL:
```env
POSTGRES_DB=releaseguard
POSTGRES_USER=releaseguard
POSTGRES_PASSWORD=your_secure_local_password
```

### 2. Build and Start All Services
```powershell
docker compose up --build -d
```
Verify container health:
```powershell
docker compose ps
```
The stack spins up three healthy containers:
- `releaseguard-postgres`: PostgreSQL 17 database (`5432:5432`)
- `releaseguard-backend`: Spring Boot 21 service (`8080:8080`)
- `releaseguard-frontend`: Nginx web server & API proxy (`3000:80`)

### 3. Access the Dashboard
Open your web browser and navigate to:
**`http://localhost:3000`**

To stop the services:
```powershell
docker compose down
```
*(To preserve database volume data, do not pass `-v`)*.

---

## 4. Local Development Setup

### Backend (Java 21 & Maven)
```powershell
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. Set database password in terminal session
$env:DB_PASSWORD = "your_secure_local_password"

# 3. Run Spring Boot application
Set-Location "$HOME\Projects\releaseguard\backend"
.\mvnw.cmd spring-boot:run

# Healthcheck
Invoke-RestMethod http://localhost:8080/actuator/health
```

### Frontend (Node 22 / Vite)
```powershell
Set-Location "$HOME\Projects\releaseguard\frontend"
npm install
npm run dev
```
Vite runs on `http://localhost:3000` with hot-reloading and automatically proxies `/api/*` to `http://localhost:8080`.

---

## 5. Testing & Verification Commands

### Backend Tests
```powershell
Set-Location "$HOME\Projects\releaseguard\backend"

# Run targeted unit tests
.\mvnw.cmd test "-Dtest=SbomControllerTest,VulnerabilityControllerTest"

# Run full test suite (145 tests including Testcontainers PostgreSQL integration)
.\mvnw.cmd test

# Package Spring Boot executable JAR
.\mvnw.cmd clean package
```

### Frontend Tests
```powershell
Set-Location "$HOME\Projects\releaseguard\frontend"

# Run Vitest test suite
npm test

# Run TypeScript compilation and production build
npm run build
```

---

## 6. End-to-End API Walkthrough

Example workflow demonstrating the full governance lifecycle using PowerShell or curl:

### 1. Register an Application
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications" `
  -ContentType "application/json" `
  -Body '{"name":"College Planner","repositoryUrl":"https://github.com/example/planner","ownerTeam":"Planner Team"}'
```

### 2. Register a Release
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications/1/releases" `
  -ContentType "application/json" `
  -Body '{"version":"v1.0.0","gitCommitSha":"0123456789abcdef0123456789abcdef01234567","dockerImage":"college-planner:1.0.0"}'
```

### 3. Upload CycloneDX SBOM
```powershell
curl -X POST "http://localhost:8080/api/v1/applications/1/releases/1/sbom" `
  -F "file=@docs/examples/college-planner-sbom.json"
```

### 4. Upload Vulnerability Scan Report
```powershell
curl -X POST "http://localhost:8080/api/v1/applications/1/releases/1/vulnerability-imports" `
  -F "file=@docs/examples/college-planner-demo-findings.json"
```

### 5. Submit for Review & Governance Approval
```powershell
# Submit release for review
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications/1/releases/1/decisions" `
  -ContentType "application/json" `
  -Body '{"action":"SUBMIT","actor":"Planner Team","reason":"Ready for security review"}'

# Approve release with scan evidence attached
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications/1/releases/1/decisions" `
  -ContentType "application/json" `
  -Body '{"action":"APPROVE","actor":"Security Lead","reason":"Scan meets release-policy-v1 criteria","vulnerabilityImportId":1}'
```

### 6. Record Deployment
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications/1/deployments" `
  -ContentType "application/json" `
  -Body '{"releaseId":1,"environment":"STAGING","deployedBy":"Planner Team","deployedAt":"2026-10-28T12:00:00Z"}'
```

### 7. Record Atomic Rollback
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/applications/1/deployments/rollbacks" `
  -ContentType "application/json" `
  -Body '{"environment":"STAGING","expectedCurrentDeploymentId":2,"targetDeploymentId":1,"deployedBy":"Planner Team","deployedAt":"2026-10-28T14:00:00Z","reason":"Restoring previous verified release"}'
```

---

## 7. CI/CD: Jenkins & SonarQube

The project includes a declarative `Jenkinsfile` and `sonar-project.properties` implementing:
1. **Prerequisites & Agent Verification**: Checks Java 21, Docker daemon availability, and Node tooling.
2. **Backend Build & JaCoCo Coverage**: Runs test suite and generates `backend/target/site/jacoco/jacoco.xml`.
3. **Frontend Build & Test**: Runs `npm ci`, `npm test`, and `npm run build`.
4. **SonarQube Quality Gate**: Analyzes Java bytecode and TypeScript source against code quality and coverage thresholds.
5. **Container Image Build & Immutable Tagging**:
   - Tags images with the full immutable Git commit SHA: `releaseguard-backend:${GIT_COMMIT}`.
   - Adds branch and short SHA tags (never relying on `:latest` alone).
6. **Governance Audit Recording**: Optional demonstration step recording the release in ReleaseGuard upon pipeline completion.

### Jenkins Setup Prerequisites
- **Plugins**: `Pipeline`, `Git`, `Docker Pipeline`, `SonarQube Scanner`, `JUnit Plugin`.
- **Global Tool Configurations**:
  - JDK: `Java-21`
  - SonarQube Scanner: `SonarQubeScanner`
- **Credentials Configured in Jenkins**:
  - `docker-registry-credentials`: Username & Password for image pushing.
  - `sonarqube-token`: Secret text token configured in Jenkins SonarQube server settings.
- **Agent Environment**: Jenkins agent requires Docker socket access (`/var/run/docker.sock`) to execute Testcontainers tests and build container images.

---

## 8. Completion & Verification Checklist

| Milestone | Component | Verification Status |
| :--- | :--- | :--- |
| **Milestone 1–8** | Backend Foundation, Registry, SBOM, Comparison, Findings, Deployments, Approvals | **Verified Locally** (145 tests, 0 failures) |
| **Milestone 9** | Scoped Endpoints (`/sbom`, `/vulnerability-imports`) | **Verified Locally** (`SbomControllerTest`, `VulnerabilityControllerTest`) |
| **Milestone 9** | Frontend React Dashboard (All 11 Workflows) | **Verified Locally** (Vitest 4/4 suites pass, production build builds clean `dist/`) |
| **Milestone 10** | Backend Multi-Stage Dockerfile (Java 21) | **Verified Configuration** (`backend/Dockerfile`, non-root, Actuator healthcheck) |
| **Milestone 10** | Frontend Multi-Stage Dockerfile & Nginx | **Verified Configuration** (`frontend/Dockerfile`, `nginx.conf`, SPA fallback, API reverse proxy) |
| **Milestone 10** | Multi-Container `compose.yaml` | **Verified Configuration** (internal network `postgres:5432`, healthchecks, volumes) |
| **Milestone 10** | JaCoCo Coverage & SonarQube Config | **Verified Configuration** (`backend/pom.xml`, `sonar-project.properties`) |
| **Milestone 10** | Declarative `Jenkinsfile` | **Verified Configuration** (immutable commit SHA tags, Testcontainers notes, Quality Gate step) |
| **Milestone 10** | External Jenkins / SonarQube Server Execution | **External Setup Required** (requires live Jenkins controller & SonarQube server credentials) |
