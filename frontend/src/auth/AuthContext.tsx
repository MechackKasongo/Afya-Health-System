import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api, clearTokens, getStoredAccessToken, persistTokens } from '../api/client';
import type { MeResponse, TokenResponse } from '../api/types';

interface AuthContextValue {
  user: MeResponse | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const token = getStoredAccessToken();
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const { data } = await api.get<MeResponse>('/api/v1/auth/me');
      setUser(data);
    } catch {
      setUser(null);
      clearTokens();
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshUser();
  }, [refreshUser]);

  useEffect(() => {
    const onProfile = (e: Event) => {
      const detail = (e as CustomEvent<MeResponse>).detail;
      if (detail) setUser(detail);
    };
    window.addEventListener('afya-auth-profile', onProfile as EventListener);
    return () => window.removeEventListener('afya-auth-profile', onProfile as EventListener);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const { data } = await api.post<TokenResponse>('/api/v1/auth/login', { username, password });
    persistTokens(data.accessToken, data.refreshToken);
    setUser(data.me);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout');
    } catch {
      /* ignore */
    }
    clearTokens();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      refreshUser,
    }),
    [user, loading, login, logout, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}
