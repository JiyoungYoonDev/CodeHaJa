'use client';

import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getMeApi, logoutApi, refreshApi } from '../../services/auth-service';
import { setAuthErrorHandler } from './api-client';

const AuthContext = createContext(null);

function clearLocalUserData() {
  if (typeof window === 'undefined') return;
  const keysToRemove = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (
      key?.startsWith('codehaja_visited_') ||
      key?.startsWith('codehaja_resume_') ||
      key?.startsWith('codehaja_code_')
    ) {
      keysToRemove.push(key);
    }
  }
  keysToRemove.forEach((k) => localStorage.removeItem(k));
}

export function AuthProvider({ children }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    try {
      const res = await getMeApi();
      setUser(res?.data ?? res ?? null);
    } catch {
      // Try refresh
      try {
        await refreshApi();
        const res = await getMeApi();
        setUser(res?.data ?? res ?? null);
      } catch {
        setUser(null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  // On 401/403 responses, re-run loadUser (tries refresh, clears user on failure)
  useEffect(() => {
    setAuthErrorHandler(loadUser);
    return () => setAuthErrorHandler(null);
  }, [loadUser]);

  const login = useCallback((userData) => {
    clearLocalUserData();
    queryClient.clear();
    setUser(userData?.data ?? userData);
  }, [queryClient]);

  const logout = useCallback(async () => {
    try {
      await logoutApi();
    } catch {
      // silent
    }
    clearLocalUserData();
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, reload: loadUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
