'use client';

import { createContext, useContext, useEffect, useState } from 'react';

const ThemeContext = createContext({ theme: 'light', toggleTheme: () => {} });

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light');

  // Sync with DOM on mount (ThemeScript may have already applied dark class)
  useEffect(() => {
    const stored = localStorage.getItem('codehaja_theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const resolved = stored ?? (prefersDark ? 'dark' : 'light');
    setTheme(resolved);
    applyTheme(resolved);
  }, []);

  function toggleTheme() {
    setTheme((prev) => {
      const next = prev === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      localStorage.setItem('codehaja_theme', next);
      return next;
    });
  }

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

function applyTheme(theme) {
  if (theme === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
}

export function useTheme() {
  return useContext(ThemeContext);
}
