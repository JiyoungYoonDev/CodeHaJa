const KEY = 'codehaja_anon_key';

function generateUUID() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // Fallback for environments without crypto.randomUUID
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function getAnonymousUserKey() {
  if (typeof window === 'undefined') return '';
  let key = localStorage.getItem(KEY);
  if (!key) {
    key = generateUUID();
    localStorage.setItem(KEY, key);
  }
  return key;
}
