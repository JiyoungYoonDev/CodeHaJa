export default function AuthLayout({ children }) {
  return (
    <div className='min-h-screen bg-gradient-to-br from-slate-50 via-white to-indigo-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-indigo-950/20 flex items-center justify-center p-4'>
      {children}
    </div>
  );
}
