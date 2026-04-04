import { Suspense } from 'react';
import ResetPasswordPageInner from './ResetPasswordPageInner';

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordPageInner />
    </Suspense>
  );
}
