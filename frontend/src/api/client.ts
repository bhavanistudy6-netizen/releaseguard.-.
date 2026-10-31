import {
  Application,
  CreateApplicationRequest,
  CreateDecisionRequest,
  CreateReleaseRequest,
  CreateUserRequest,
  Deployment,
  DeploymentEnvironment,
  PageResponse,
  RecordDeploymentRequest,
  RecordRollbackRequest,
  Release,
  ReleaseComparisonResponse,
  ReleaseDecision,
  ResetPasswordRequest,
  RollbackResponse,
  SbomComponent,
  SbomDocument,
  UpdateUserRequest,
  User,
  VulnerabilityFinding,
  VulnerabilityImport,
} from '../types';

export class ApiError extends Error {
  status: number;
  details?: unknown;

  constructor(status: number, message: string, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

function getCsrfTokenFromCookie(): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

let cachedCsrfToken: string | null = null;

export async function fetchCsrfToken(): Promise<string | null> {
  try {
    const res = await fetch('/api/v1/auth/csrf', {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    });
    if (res.ok) {
      const data = await res.json();
      cachedCsrfToken = data.token;
      return data.token;
    }
  } catch {
    // ignore
  }
  return null;
}

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const method = (options?.method || 'GET').toUpperCase();
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(options?.headers as Record<string, string>),
  };

  // Attach CSRF token on state-changing browser requests
  if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    let csrfToken = getCsrfTokenFromCookie() || cachedCsrfToken;
    if (!csrfToken) {
      csrfToken = await fetchCsrfToken();
    }
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    let details: unknown = null;
    try {
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const errorJson = await response.json();
        details = errorJson;
        errorMessage =
          errorJson.message ||
          errorJson.error ||
          (Array.isArray(errorJson.errors)
            ? errorJson.errors.map((e: { defaultMessage?: string; field?: string }) => `${e.field}: ${e.defaultMessage}`).join(', ')
            : errorMessage);
      } else {
        const errorText = await response.text();
        if (errorText) errorMessage = errorText;
      }
    } catch {
      // ignore parsing failure and keep default status error
    }

    if (response.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('releaseguard:unauthorized'));
    }

    throw new ApiError(response.status, errorMessage, details);
  }

  // Check if response has body (e.g., 204 No Content)
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  // Authentication
  async login(username: string, password: string): Promise<User> {
    const user = await request<User>('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    // refresh CSRF cookie after login session rotation
    void fetchCsrfToken();
    return user;
  },

  async logout(): Promise<void> {
    try {
      await request<{ message: string }>('/api/v1/auth/logout', {
        method: 'POST',
      });
    } finally {
      cachedCsrfToken = null;
    }
  },

  async getCurrentUser(): Promise<User> {
    return request<User>('/api/v1/auth/me');
  },

  async getCsrf(): Promise<{ token: string; parameterName: string; headerName: string }> {
    return request<{ token: string; parameterName: string; headerName: string }>('/api/v1/auth/csrf');
  },

  // Admin User Management
  async getUsers(page = 0, size = 20): Promise<PageResponse<User>> {
    return request<PageResponse<User>>(`/api/v1/admin/users?page=${page}&size=${size}`);
  },

  async getUser(id: number): Promise<User> {
    return request<User>(`/api/v1/admin/users/${id}`);
  },

  async createUser(data: CreateUserRequest): Promise<User> {
    return request<User>('/api/v1/admin/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  async updateUser(id: number, data: UpdateUserRequest): Promise<User> {
    return request<User>(`/api/v1/admin/users/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  async resetUserPassword(id: number, data: ResetPasswordRequest): Promise<void> {
    return request<void>(`/api/v1/admin/users/${id}/password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  async deleteUser(id: number): Promise<void> {
    return request<void>(`/api/v1/admin/users/${id}`, {
      method: 'DELETE',
    });
  },

  // Applications
  async getApplications(page = 0, size = 10): Promise<PageResponse<Application>> {
    return request<PageResponse<Application>>(`/api/v1/applications?page=${page}&size=${size}`);
  },

  async getApplication(id: number): Promise<Application> {
    return request<Application>(`/api/v1/applications/${id}`);
  },

  async createApplication(data: CreateApplicationRequest): Promise<Application> {
    return request<Application>('/api/v1/applications', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  // Releases
  async getReleases(applicationId: number, page = 0, size = 10): Promise<PageResponse<Release>> {
    return request<PageResponse<Release>>(
      `/api/v1/applications/${applicationId}/releases?page=${page}&size=${size}`
    );
  },

  async getRelease(applicationId: number, releaseId: number): Promise<Release> {
    return request<Release>(`/api/v1/applications/${applicationId}/releases/${releaseId}`);
  },

  async createRelease(applicationId: number, data: CreateReleaseRequest): Promise<Release> {
    return request<Release>(`/api/v1/applications/${applicationId}/releases`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  // SBOM
  async getSbom(applicationId: number, releaseId: number): Promise<SbomDocument | null> {
    try {
      return await request<SbomDocument>(`/api/v1/applications/${applicationId}/releases/${releaseId}/sbom`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        return null;
      }
      throw err;
    }
  },

  async uploadSbom(applicationId: number, releaseId: number, file: File): Promise<SbomDocument> {
    const formData = new FormData();
    formData.append('file', file);
    return request<SbomDocument>(`/api/v1/applications/${applicationId}/releases/${releaseId}/sbom`, {
      method: 'POST',
      body: formData,
    });
  },

  async getSbomComponents(
    applicationId: number,
    releaseId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<SbomComponent>> {
    return request<PageResponse<SbomComponent>>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/sbom/components?page=${page}&size=${size}`
    );
  },

  // Dependency Comparison
  async compareReleases(
    applicationId: number,
    fromReleaseId: number,
    toReleaseId: number
  ): Promise<ReleaseComparisonResponse> {
    return request<ReleaseComparisonResponse>(
      `/api/v1/applications/${applicationId}/releases/compare?fromReleaseId=${fromReleaseId}&toReleaseId=${toReleaseId}`
    );
  },

  // Vulnerability Imports & Findings
  async getVulnerabilityImports(
    applicationId: number,
    releaseId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<VulnerabilityImport>> {
    return request<PageResponse<VulnerabilityImport>>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/vulnerability-imports?page=${page}&size=${size}`
    );
  },

  async getVulnerabilityImport(
    applicationId: number,
    releaseId: number,
    importId: number
  ): Promise<VulnerabilityImport> {
    return request<VulnerabilityImport>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/vulnerability-imports/${importId}`
    );
  },

  async uploadVulnerabilityReport(
    applicationId: number,
    releaseId: number,
    file: File
  ): Promise<VulnerabilityImport> {
    const formData = new FormData();
    formData.append('file', file);
    return request<VulnerabilityImport>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/vulnerability-imports`,
      {
        method: 'POST',
        body: formData,
      }
    );
  },

  async getFindings(
    applicationId: number,
    releaseId: number,
    importId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<VulnerabilityFinding>> {
    return request<PageResponse<VulnerabilityFinding>>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/vulnerability-imports/${importId}/findings?page=${page}&size=${size}`
    );
  },

  // Release Decisions (Approvals)
  async getDecisions(
    applicationId: number,
    releaseId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<ReleaseDecision>> {
    return request<PageResponse<ReleaseDecision>>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/decisions?page=${page}&size=${size}`
    );
  },

  async createDecision(
    applicationId: number,
    releaseId: number,
    data: CreateDecisionRequest
  ): Promise<ReleaseDecision> {
    return request<ReleaseDecision>(
      `/api/v1/applications/${applicationId}/releases/${releaseId}/decisions`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      }
    );
  },

  // Deployments
  async getCurrentDeployment(
    applicationId: number,
    environment: DeploymentEnvironment
  ): Promise<Deployment | null> {
    try {
      return await request<Deployment>(
        `/api/v1/applications/${applicationId}/deployments/current?environment=${environment}`
      );
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        return null;
      }
      throw err;
    }
  },

  async getDeploymentHistory(
    applicationId: number,
    environment: DeploymentEnvironment,
    page = 0,
    size = 10
  ): Promise<PageResponse<Deployment>> {
    return request<PageResponse<Deployment>>(
      `/api/v1/applications/${applicationId}/deployments?environment=${environment}&page=${page}&size=${size}`
    );
  },

  async recordDeployment(
    applicationId: number,
    data: RecordDeploymentRequest
  ): Promise<Deployment> {
    return request<Deployment>(`/api/v1/applications/${applicationId}/deployments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },

  async recordRollback(
    applicationId: number,
    data: RecordRollbackRequest
  ): Promise<RollbackResponse> {
    return request<RollbackResponse>(
      `/api/v1/applications/${applicationId}/deployments/rollbacks`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      }
    );
  },

  async getDeployedFindings(
    applicationId: number,
    environment: DeploymentEnvironment,
    importId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<VulnerabilityFinding>> {
    return request<PageResponse<VulnerabilityFinding>>(
      `/api/v1/applications/${applicationId}/deployments/current/findings?environment=${environment}&importId=${importId}&page=${page}&size=${size}`
    );
  },
};
