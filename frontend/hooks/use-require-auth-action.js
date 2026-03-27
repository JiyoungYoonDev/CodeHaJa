import { useState, useCallback } from 'react';
import { useAuth } from '@/lib/auth-context';

export function useRequireAuthAction(action) {
  const { user } = useAuth();
  const [showAuthModal, setShowAuthModal] = useState(false);

  const guard = useCallback(
    (...args) => {
      if (!user) {
        setShowAuthModal(true);
        return;
      }
      Promise.resolve(action?.(...args)).catch((e) => {
        console.error('[RequireAuthAction] action failed:', e);
      });
    },
    [user, action],
  );

  return {
    guard,
    showAuthModal,
    closeAuthModal: () => setShowAuthModal(false),
  };
}
