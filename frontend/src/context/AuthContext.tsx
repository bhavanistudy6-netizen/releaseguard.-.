import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, Role } from '../types';
import { api, ApiError } from '../api/client';

export type Permission =
  | 'create-app'
  | 'create-release'
  | 'upload-sbom'
  | 'upload-vuln'
  | 'submit-review'
  | 'approve-release'
  | 'reject-release'
  | 'revoke-release'
  | 'retire-release'
  | 'deploy'
  | 'rollback'
  | 'admin';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (role: Role | Role[]) => boolean;
  can: (permission: Permission) => boolean;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refreshUser = useCallback(async () => {
    try {
      const current = await api.getCurrentUser();
      setUser(current);
      setError(null);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshUser();

    const handleUnauthorized = () => {
      setUser(null);
    };

    window.addEventListener('releaseguard:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('releaseguard:unauthorized', handleUnauthorized);
    };
  }, [refreshUser]);

  const login = async (username: string, password: string) => {
    setError(null);
    try {
      const loggedIn = await api.login(username, password);
      setUser(loggedIn);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Login failed. Please check your credentials.');
      }
      throw err;
    }
  };

  const logout = async () => {
    try {
      await api.logout();
    } finally {
      setUser(null);
      setError(null);
    }
  };

  const hasRole = useCallback(
    (role: Role | Role[]): boolean => {
      if (!user) return false;
      const checkRoles = Array.isArray(role) ? role : [role];
      return checkRoles.some((r) => user.roles.includes(r));
    },
    [user]
  );

  const can = useCallback(
    (permission: Permission): boolean => {
      if (!user) return false;
      if (user.roles.includes('ADMIN')) return true;

      switch (permission) {
        case 'create-app':
        case 'create-release':
        case 'upload-sbom':
        case 'upload-vuln':
        case 'submit-review':
          return user.roles.includes('DEVELOPER');

        case 'approve-release':
        case 'reject-release':
        case 'revoke-release':
        case 'retire-release':
          return user.roles.includes('REVIEWER');

        case 'deploy':
        case 'rollback':
          return user.roles.includes('DEPLOYER');

        case 'admin':
          return user.roles.includes('ADMIN');

        default:
          return false;
      }
    },
    [user]
  );

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        error,
        login,
        logout,
        hasRole,
        can,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

