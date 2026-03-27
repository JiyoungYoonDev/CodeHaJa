import { apiFetch } from '@/lib/api-client';

export async function signupApi(data) {
  return apiFetch('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function loginApi(data) {
  return apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function logoutApi() {
  return apiFetch('/api/auth/logout', { method: 'POST' });
}

export async function refreshApi() {
  return apiFetch('/api/auth/refresh', { method: 'POST' });
}

export async function getMeApi() {
  return apiFetch('/api/auth/me');
}

export async function googleLoginApi(idToken) {
  return apiFetch('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken }),
  });
}

export async function forgotPasswordApi(email) {
  return apiFetch('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export async function resetPasswordApi(token, newPassword) {
  return apiFetch('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}
