# ReleaseGuard: Real GitHub Project Demonstration

This document provides complete, reproducible evidence of ReleaseGuard tracking a real-world, publicly available GitHub application through its full release, dependency governance, vulnerability tracking, deployment, and rollback lifecycle.

---

## 1. Demonstration Overview & Boundaries

### Purpose
To prove ReleaseGuard's production capabilities using real upstream codebases and real vulnerability scanners, without relying on mock vulnerabilities or synthetic payloads for the demonstration:
- **Upstream Application**: `spring-petclinic/spring-petclinic-rest`
- **Releases Selected**: `v3.2.1` and `v3.4.0` (Spring Boot 3.2.1 to 3.4.0 upgrade)
- **SBOM Format**: CycloneDX JSON 1.6 generated with `cyclonedx-maven-plugin:2.9.1`
- **Scanner**: Aqua Security Trivy `0.57.1`
- **Governance**: Production policy enforcement (`release-policy-v1`) against genuine CVE findings
- **Deployments**: Staging environment progression and atomic rollback recording

### Governance & Security Invariants
1. **Preservation of Existing Data**: Pre-existing synthetic demo data ("College Planner") is preserved untouched and explicitly designated as synthetic.
2. **Honest Policy Enforcement**: Real-world open-source dependencies contain known CVEs. Rather than suppressing, mocking, or modifying real findings to force a passing grade, ReleaseGuard's `release-policy-v1` honestly evaluates the upstream scan, flags the CRITICAL and HIGH severity findings, and rejects production approval. This provides concrete evidence that policy enforcement functions as designed.
3. **Deployment Audit Role**: Deployment and rollback endpoints record immutable release provenance and pointer state; they record events rather than invoking cloud infrastructure.
4. **Caller-Supplied Identity**: Actor and deployedBy fields are audit metadata supplied by the caller; authentication/SSO is out of scope for this milestone.

---

## 2. Upstream Project Selection

| Attribute | Specification |
|:---|:---|
| **Repository** | [https://github.com/spring-petclinic/spring-petclinic-rest](https://github.com/spring-petclinic/spring-petclinic-rest) |
| **License** | Apache License 2.0 |
| **Maintainer** | Spring PetClinic Community |
| **Domain** | REST API for veterinary clinic management |
| **Technology Stack** | Java 21, Spring Boot, Spring Data JPA, PostgreSQL/H2, MapStruct, OpenAPI |
| **Release 1 (Baseline)** | Tag `v3.2.1` &bull; Git SHA: `dc06a75075a6a0d493897f8982df7e474afcd7cf` |
| **Release 2 (Upgrade)** | Tag `v3.4.0` &bull; Git SHA: `26a80b7a510af1fda0acdfb45d65aa480bdb7607` |

### Why This Application?
1. **Real-world Spring Boot transition**: Migrating from Spring Boot 3.2.1 to 3.4.0 represents an authentic enterprise upgrade with major dependency shifts (Jackson 2.15 -> 2.18, Logback 1.4 -> 1.5, Tomcat, and Spring Framework).
2. **Deterministic, reproducibly buildable**: Uses standard Maven wrapper and JDK 21.
3. **High-quality CycloneDX metadata**: Produces full PURLs and coordinate metadata recognized by modern vulnerability databases.

---

## 3. SBOM Generation & Container Packaging

### Commands Used to Build Revisions
Both revisions were cloned into a separate staging workspace (`releaseguard-demo-target`) to maintain isolation:

```powershell
# Clone upstream repository
git clone https://github.com/spring-petclinic/spring-petclinic-rest.git C:\Users\vigneshwara rao\Projects\releaseguard-demo-target
cd "C:\Users\vigneshwara rao\Projects\releaseguard-demo-target"

# Build Release 1 (v3.2.1)
git checkout v3.2.1
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom "-DschemaVersion=1.6"

# Build Release 2 (v3.4.0)
git checkout v3.4.0
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom "-DschemaVersion=1.6"
```

### Generated SBOM Evidence

| Release | File | Components | Spec | SHA-256 Checksum |
|:---|:---|:---:|:---:|:---|
| **v3.2.1** | `spring-petclinic-rest-v3.2.1-bom.json` | 100 | CycloneDX 1.6 | `9e9fe707426299dfd7f22aca8af237e42fcbeb20579a16a76921dacbcd2e3a4a` |
| **v3.4.0** | `spring-petclinic-rest-v3.4.0-bom.json` | 101 | CycloneDX 1.6 | `4b46b298978635ea6a77fccc99b57cd5eb65c2ff0dfeb35e39b377e22fa64a65` |

Preserved under: [`docs/examples/real-demo/`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/docs/examples/real-demo/)

### Docker Images Built and Tagged

```powershell
# Tagged in local Docker daemon
docker image inspect spring-petclinic-rest:dc06a75075a6a0d493897f8982df7e474afcd7cf
docker image inspect spring-petclinic-rest:26a80b7a510af1fda0acdfb45d65aa480bdb7607
```

---

## 4. Real Vulnerability Scanning with Trivy

### Scan Execution Command
Official Aqua Security Trivy container (`aquasec/trivy:0.57.1`) was invoked directly against the generated CycloneDX SBOMs:

```powershell
# Scan Release 1 SBOM
docker run --rm -v "${PWD}/docs/examples/real-demo:/work" aquasec/trivy:0.57.1 `
  sbom --format json --output /work/trivy-raw-v3.2.1.json /work/spring-petclinic-rest-v3.2.1-bom.json

# Scan Release 2 SBOM
docker run --rm -v "${PWD}/docs/examples/real-demo:/work" aquasec/trivy:0.57.1 `
  sbom --format json --output /work/trivy-raw-v3.4.0.json /work/spring-petclinic-rest-v3.4.0-bom.json
```

### Raw Findings Captured

| Scan Target | Raw JSON Size | Total Findings | CRITICAL | HIGH | MEDIUM | LOW |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| **v3.2.1 SBOM** | 443,562 bytes | 104 | 11 | 41 | 38 | 14 |
| **v3.4.0 SBOM** | 430,914 bytes | 101 | 11 | 38 | 37 | 15 |

Raw scan files:
- [`docs/examples/real-demo/trivy-raw-v3.2.1.json`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/docs/examples/real-demo/trivy-raw-v3.2.1.json)
- [`docs/examples/real-demo/trivy-raw-v3.4.0.json`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/docs/examples/real-demo/trivy-raw-v3.4.0.json)

---

## 5. Trivy Report Adapter

To map Trivy's schema into ReleaseGuard's `VulnerabilityReport` contract (`POST /api/v1/applications/{appId}/releases/{relId}/vulnerability-imports`), a tested, deterministic adapter was developed in [`scripts/trivy-adapter.mjs`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/scripts/trivy-adapter.mjs).

### Key Architectural Behaviors
1. **Component Identity Mapping**:
   - Matches Trivy's `PkgIdentifier.BOMRef` against ReleaseGuard component `bom-ref`.
   - Fallback to exact PURL matching.
   - Coordinate fallback (`group:name@version`).
   - Strict ambiguity detection: rejects ambiguous matches across multiple components.
2. **Deduplication**: Findings are deduplicated on `(componentBomRef, source, vulnerabilityId)`.
3. **Severity Normalization**: Non-standard severities are normalized to `UNKNOWN`.
4. **Provenance & Checksum Verification**: Calculates and records the exact SHA-256 hash of the referenced CycloneDX SBOM to guarantee cryptographic binding.

### Unit Test Verification
Run with Node's native test runner:
```powershell
node --test scripts/trivy-adapter.test.mjs
```
**Results: 9 tests passed, 0 failures.**

### Output ReleaseGuard Reports
- [`docs/examples/real-demo/spring-petclinic-rest-v3.2.1-findings.json`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/docs/examples/real-demo/spring-petclinic-rest-v3.2.1-findings.json) (104 findings, bound to SBOM `9e9fe707...`)
- [`docs/examples/real-demo/spring-petclinic-rest-v3.4.0-findings.json`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/docs/examples/real-demo/spring-petclinic-rest-v3.4.0-findings.json) (101 findings, bound to SBOM `4b46b298...`)

---

## 6. End-to-End Workflow Execution Walkthrough

The complete demonstration was executed and verified using the automated runner script [`scripts/execute-real-demo.mjs`](file:///C:/Users/vigneshwara%20rao/Projects/releaseguard/scripts/execute-real-demo.mjs):

```powershell
node scripts/execute-real-demo.mjs
```

### Step 1: Health & Connectivity Verification
- **Endpoint**: `GET /actuator/health`
- **Result**: `HTTP 200 OK` &bull; `{"groups":["liveness","readiness"],"status":"UP"}`

### Step 2: Application Registration
- **Endpoint**: `POST /api/v1/applications`
- **Payload**:
  ```json
  {
    "name": "Spring PetClinic REST",
    "description": "Official Spring PetClinic REST API demonstrating real-world dependency governance and vulnerability tracking across Spring Boot 3.2.1 and 3.4.0 releases.",
    "repositoryUrl": "https://github.com/spring-petclinic/spring-petclinic-rest",
    "ownerTeam": "Core Services Platform Team"
  }
  ```
- **Registered Application ID**: `2` (Preserving Application ID `1` "College Planner")

### Step 3: Register Releases
- **Release 1**: `v3.2.1` &bull; SHA: `dc06a75075a6a0d493897f8982df7e474afcd7cf` &bull; Assigned Release ID: `3`
- **Release 2**: `v3.4.0` &bull; SHA: `26a80b7a510af1fda0acdfb45d65aa480bdb7607` &bull; Assigned Release ID: `4`

### Step 4: Import SBOM for Release 1 (v3.2.1)
- **Endpoint**: `POST /api/v1/applications/2/releases/3/sbom`
- **Status**: `HTTP 201 Created`
- **Component Count**: `100` components imported
- **Spec**: CycloneDX 1.6 &bull; SHA-256: `9e9fe707426299dfd7f22aca8af237e42fcbeb20579a16a76921dacbcd2e3a4a`

### Step 5: Import SBOM for Release 2 (v3.4.0)
- **Endpoint**: `POST /api/v1/applications/2/releases/4/sbom`
- **Status**: `HTTP 201 Created`
- **Component Count**: `101` components imported
- **Spec**: CycloneDX 1.6 &bull; SHA-256: `4b46b298978635ea6a77fccc99b57cd5eb65c2ff0dfeb35e39b377e22fa64a65`

### Step 6: Dependency Comparison (v3.2.1 -> v3.4.0)
- **Endpoint**: `GET /api/v1/applications/2/releases/compare?fromReleaseId=3&toReleaseId=4`
- **Comparison Summary**:
  - **Total Items Compared**: `103`
  - **UNCHANGED**: `10`
  - **VERSION_CHANGED**: `88`
  - **ADDED**: `3` (`javax.activation-api 1.2.0`, `jspecify 1.0.0`, `webjars-locator-lite 1.0.1`)
  - **REMOVED**: `2` (`classgraph 4.8.149`, `webjars-locator-core 0.55`)
  - **UNCERTAIN**: `0`
- **Key Version Changes**:
  - `ch.qos.logback:logback-classic`: `1.4.14` -> `1.5.12`
  - `com.fasterxml.jackson.core:jackson-databind`: `2.15.3` -> `2.18.1`
  - `org.springframework:spring-core`: `6.1.2` -> `6.2.0`
  - `org.apache.tomcat.embed:tomcat-embed-core`: `10.1.17` -> `10.1.33`

### Step 7 & 8: Import Real Vulnerability Reports
- **Release 1 Import**: ID `3` &bull; Scanner: `Trivy 0.57.1` &bull; `104` findings bound to exact components and `Core Services Platform Team`.
- **Release 2 Import**: ID `4` &bull; Scanner: `Trivy 0.57.1` &bull; `101` findings bound to exact components and `Core Services Platform Team`.

### Step 9: Policy Evaluation & Governance Enforcement
- **Submission**: `POST /api/v1/applications/2/releases/4/decisions` with action `SUBMIT`.
  - Release transitions from `DRAFT` to `READY_FOR_REVIEW` (Decision ID `2`).
- **Approval Attempt with Evidence**: `POST /api/v1/applications/2/releases/4/decisions` with action `APPROVE`, `vulnerabilityImportId: 4`.
  - **Result**: `HTTP 409 Conflict`
  - **Policy Blockers**: `Findings must not contain CRITICAL vulnerabilities; Findings must not contain HIGH vulnerabilities`
  - **Significance**: ReleaseGuard accurately blocked production release approval because the upstream components in `spring-petclinic-rest:v3.4.0` contain genuine HIGH/CRITICAL CVEs (e.g., `CVE-2026-68494` on Jackson Core). Policy `release-policy-v1` operated with strict integrity.

### Step 10: Production Deployment Barrier Enforcement
- **Attempt**: `POST /api/v1/applications/2/deployments` with `environment: "PRODUCTION"`, `releaseId: 4`.
- **Result**: `HTTP 409 Conflict` &bull; `Release must be APPROVED for production deployment`.
- **Significance**: Unapproved releases cannot bypass governance and enter production.

### Step 11: Staging Deployments
1. **Deployment 1**: Deployed baseline `v3.2.1` to `STAGING` (Deployment ID `9`).
2. **Deployment 2**: Deployed upgrade `v3.4.0` to `STAGING` (Deployment ID `10`).
3. **Current Pointer Check**: `GET /api/v1/applications/2/deployments/current?environment=STAGING` confirmed pointer moved to Deployment `10` (`v3.4.0`).

### Step 12: Query Deployed Findings in STAGING
- **Endpoint**: `GET /api/v1/applications/2/deployments/current/findings?environment=STAGING&importId=4`
- **Result**: Successfully surfaced the `101` active vulnerability findings currently deployed to `STAGING` on release `v3.4.0`, linked to owner `Core Services Platform Team`.

### Step 13: Atomic Rollback to v3.2.1
- **Endpoint**: `POST /api/v1/applications/2/deployments/rollbacks`
- **Request**:
  ```json
  {
    "environment": "STAGING",
    "expectedCurrentDeploymentId": 10,
    "targetDeploymentId": 9,
    "deployedBy": "incident-commander",
    "deployedAt": "2026-10-30T18:44:15.820Z",
    "reason": "Automated policy rollback: reverting STAGING to verified v3.2.1 due to vulnerability policy failure on v3.4.0."
  }
  ```
- **Outcome**:
  - Rollback Recorded: Deployment ID `11` (`ROLLBACK`)
  - Target Release: `v3.2.1` (Release ID `3`)
  - Current Pointer: Verified that `STAGING` current pointer now points to Deployment ID `11` (`v3.2.1`).
  - Stale Pointer Protection: Protected by `expectedCurrentDeploymentId` against concurrent race conditions.

---

## 7. Web Dashboard UI Walkthrough

All steps can be visually inspected and navigated in the web dashboard at **`http://localhost:3000`**:

1. **Applications View** (`/applications`):
   - Displays `Spring PetClinic REST` alongside `College Planner`.
   - Shows Owner Team `Core Services Platform Team` and link to GitHub repository.
2. **Releases View** (`/applications/2`):
   - Lists releases `v3.2.1` and `v3.4.0`.
   - Shows status badges (`READY_FOR_REVIEW`, `DRAFT`), full Git commit SHAs, and container tags.
3. **SBOM Inventory View**:
   - Inspects the 100 components of `v3.2.1` and 101 components of `v3.4.0` with PURLs and versions.
4. **Dependency Comparison View** (`/compare`):
   - Side-by-side comparison between `v3.2.1` and `v3.4.0`.
   - Highlights 88 version upgrades (e.g. Jackson, Spring, Logback), 3 additions, and 2 removals.
5. **Vulnerability Findings View**:
   - Displays the 101 real Trivy findings for `v3.4.0` categorized by severity badges (`HIGH`, `MEDIUM`, `LOW`).
   - Links each finding to its component and owner team.
6. **Governance & Approvals View**:
   - Displays the audit log of decisions: Submission by `release-lead`, and rejection by policy `release-policy-v1`.
7. **Deployments & Rollback View**:
   - Shows STAGING deployment timeline: Deployment `9` (`v3.2.1`), Deployment `10` (`v3.4.0`), and Rollback Deployment `11` (`v3.2.1`).
   - Confirms the current active release in STAGING is `v3.2.1`.

---

## 8. Reproducibility Instructions

To reproduce this demonstration at any time on your workstation:

```powershell
# 1. Ensure PostgreSQL is running
docker compose up -d postgres

# 2. Run the end-to-end demo runner
node scripts/execute-real-demo.mjs
```

To run adapter tests:
```powershell
node --test scripts/trivy-adapter.test.mjs
```

To run backend tests:
```powershell
cd backend
.\mvnw.cmd test
```

To run frontend tests:
```powershell
cd frontend
npm test -- --run
```

