export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Application {
  id: number;
  name: string;
  description: string | null;
  repositoryUrl: string;
  ownerTeam: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateApplicationRequest {
  name: string;
  description?: string;
  repositoryUrl: string;
  ownerTeam: string;
}

export type ReleaseStatus = 'DRAFT' | 'READY_FOR_REVIEW' | 'APPROVED' | 'RETIRED';

export interface Release {
  id: number;
  applicationId: number;
  version: string;
  gitCommitSha: string;
  dockerImage: string;
  imageDigest: string | null;
  status: ReleaseStatus;
  createdAt: string;
  updatedAt: string;
  lockVersion: number;
}

export interface CreateReleaseRequest {
  version: string;
  gitCommitSha: string;
  dockerImage: string;
  imageDigest?: string | null;
}

export interface SbomDocument {
  id: number;
  releaseId: number;
  originalFilename: string;
  bomFormat: string;
  specVersion: string;
  documentSha256: string;
  componentCount: number;
  uploadedAt: string;
}

export interface SbomComponent {
  id: number;
  bomRef: string | null;
  componentType: string;
  groupName: string | null;
  name: string;
  version: string | null;
  purl: string | null;
}

export type ChangeType = 'ADDED' | 'REMOVED' | 'UNCHANGED' | 'VERSION_CHANGED' | 'UNCERTAIN';

export interface ComponentIdentity {
  type: string;
  namespace: string | null;
  name: string;
  subpath: string | null;
  qualifiers: Record<string, string>;
}

export interface ComponentVersionSnapshot {
  bomRef: string | null;
  version: string | null;
  purl: string | null;
}

export interface DependencyChange {
  identity: ComponentIdentity;
  changeType: ChangeType;
  before: ComponentVersionSnapshot | null;
  after: ComponentVersionSnapshot | null;
  reason: string | null;
}

export interface ReleaseComparisonResponse {
  applicationId: number;
  fromReleaseId: number;
  toReleaseId: number;
  changes: DependencyChange[];
}

export type VulnerabilitySeverity =
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'INFORMATIONAL'
  | 'UNKNOWN';

export interface VulnerabilityImport {
  id: number;
  releaseId: number;
  sbomDocumentId: number;
  scannerName: string;
  scannerVersion: string;
  documentSha256: string;
  findingCount: number;
  scannedAt: string;
  importedAt: string;
}

export interface VulnerabilityFinding {
  id: number;
  importId: number;
  applicationId: number;
  applicationName: string;
  ownerTeam: string;
  releaseId: number;
  releaseVersion: string;
  componentId: number;
  componentName: string;
  componentVersion: string | null;
  purl: string | null;
  vulnerabilityId: string;
  source: string;
  severity: VulnerabilitySeverity;
  description: string;
  fixedVersion: string | null;
}

export type DeploymentEnvironment = 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';
export type DeploymentType = 'DEPLOY' | 'ROLLBACK';

export interface Deployment {
  id: number;
  applicationId: number;
  releaseId: number;
  releaseVersion: string;
  environment: DeploymentEnvironment;
  deployedBy: string;
  deployedAt: string;
  recordedAt: string;
  notes: string | null;
  deploymentType: DeploymentType;
  rollbackFromDeploymentId: number | null;
  rollbackTargetDeploymentId: number | null;
  rollbackReason: string | null;
}

export interface RecordDeploymentRequest {
  releaseId: number;
  environment: DeploymentEnvironment;
  deployedBy: string;
  deployedAt: string;
  notes?: string;
}

export interface RecordRollbackRequest {
  environment: DeploymentEnvironment;
  expectedCurrentDeploymentId: number;
  targetDeploymentId: number;
  deployedBy: string;
  deployedAt: string;
  reason: string;
}

export interface RollbackResponse {
  deployment: Deployment;
  deploymentType: 'ROLLBACK';
  rollbackFromDeploymentId: number;
  rollbackTargetDeploymentId: number;
  reason: string;
}

export type DecisionAction = 'SUBMIT' | 'APPROVE' | 'REJECT' | 'REVOKE' | 'RETIRE';

export interface ReleaseDecision {
  id: number;
  releaseId: number;
  action: DecisionAction;
  fromStatus: ReleaseStatus;
  toStatus: ReleaseStatus;
  actor: string;
  reason: string;
  vulnerabilityImportId: number | null;
  policyVersion: string | null;
  decidedAt: string;
}

export interface CreateDecisionRequest {
  action: DecisionAction;
  actor: string;
  reason: string;
  vulnerabilityImportId?: number | null;
}
