const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

// Called by AuthProvider to handle 401/403 responses (e.g. expired JWT)
let _authErrorHandler = null;
let _authErrorHandling = false;
export function setAuthErrorHandler(fn) {
  _authErrorHandler = fn;
}

function getApiBaseUrl() {
  return process.env.NEXT_PUBLIC_API_BASE_URL || '';
}

export function buildApiUrl(path) {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) {
    return path;
  }

  return new URL(path, baseUrl).toString();
}

export async function apiFetch(path, options = {}) {
  const response = await fetch(buildApiUrl(path), {
    ...options,
    credentials: 'include',
    headers: {
      ...DEFAULT_HEADERS,
      ...(options.headers || {}),
    },
  });

  if ((response.status === 401 || response.status === 403) && !_authErrorHandling && _authErrorHandler) {
    _authErrorHandling = true;
    _authErrorHandler().finally(() => { _authErrorHandling = false; });
  }

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || 'Request failed');
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
