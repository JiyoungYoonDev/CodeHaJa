export default function LearnLayout({ children }) {
  return (
    <div
      className='dark fixed inset-0 z-[100] overflow-hidden'
      style={{ background: 'var(--background)' }}
    >
      {children}
    </div>
  );
}
