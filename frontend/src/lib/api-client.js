const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

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
    headers: {
      ...DEFAULT_HEADERS,
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || 'Request failed');
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
