'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import { Menu, Code2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
  NavigationMenuLink,
  navigationMenuTriggerStyle,
} from '@/components/ui/navigation-menu';
import { cn } from '@/lib/utils';
import ThemeToggle from './ThemeToggle';
import { useAuth } from '@/lib/auth-context';
import UserDropdown from './UserDropdown';

const navLinks = [
  { href: '/roadmap', label: 'Courses' },
  { href: '/roadmap', label: 'Roadmap' },
];

export default function Navbar() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const { user, loading } = useAuth();

  return (
    <header className='sticky top-0 z-50 w-full border-b border-border bg-background/80 backdrop-blur-lg'>
      <div className='max-w-[100rem] mx-auto flex h-16 items-center justify-between px-6'>
        {/* Logo */}
        <Link href='/' className='flex items-center gap-2'>
          <Code2 size={28} className='text-primary' />
          <span className='text-xl font-black tracking-tight text-foreground'>
            Code<span className='text-primary'>Haja</span>
          </span>
        </Link>

        {/* Desktop Nav */}
        <NavigationMenu className='hidden md:flex'>
          <NavigationMenuList>
            {navLinks.map((link) => (
              <NavigationMenuItem key={link.label}>
                <NavigationMenuLink
                  asChild
                  className={cn(
                    navigationMenuTriggerStyle(),
                    pathname === link.href && 'bg-accent text-accent-foreground',
                  )}
                >
                  <Link href={link.href}>{link.label}</Link>
                </NavigationMenuLink>
              </NavigationMenuItem>
            ))}
          </NavigationMenuList>
        </NavigationMenu>

        {/* Desktop right side */}
        <div className='hidden md:flex items-center gap-2'>
          <ThemeToggle />
          {!loading && (
            user ? (
              <UserDropdown />
            ) : (
              <>
                <Button variant='ghost' asChild>
                  <Link href='/login'>Login</Link>
                </Button>
                <Button asChild>
                  <Link href='/signup'>Sign Up</Link>
                </Button>
              </>
            )
          )}
        </div>

        {/* Mobile: ThemeToggle + Hamburger */}
        <div className='flex items-center gap-1 md:hidden'>
          <ThemeToggle />
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <Button variant='ghost' size='icon'>
                <Menu size={24} />
              </Button>
            </SheetTrigger>
            <SheetContent side='right' className='w-72'>
              <SheetHeader>
                <SheetTitle>
                  <Link
                    href='/'
                    onClick={() => setOpen(false)}
                    className='flex items-center gap-2'
                  >
                    <Code2 size={24} className='text-primary' />
                    <span className='text-lg font-black tracking-tight text-foreground'>
                      Code<span className='text-primary'>Haja</span>
                    </span>
                  </Link>
                </SheetTitle>
              </SheetHeader>

              <nav className='flex flex-col gap-1 mt-8'>
                {navLinks.map((link) => (
                  <Link
                    key={link.label}
                    href={link.href}
                    onClick={() => setOpen(false)}
                    className={cn(
                      'px-4 py-3 rounded-lg text-sm font-medium transition-colors',
                      pathname === link.href
                        ? 'bg-accent text-primary'
                        : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                    )}
                  >
                    {link.label}
                  </Link>
                ))}
              </nav>

              <div className='flex flex-col gap-3 mt-8 px-4'>
                {!loading && (
                  user ? (
                    <div className='px-1'>
                      <p className='text-sm font-bold text-foreground truncate'>{user.name}</p>
                      <p className='text-xs text-muted-foreground truncate'>{user.email}</p>
                    </div>
                  ) : (
                    <>
                      <Button variant='outline' asChild className='w-full'>
                        <Link href='/login' onClick={() => setOpen(false)}>Login</Link>
                      </Button>
                      <Button asChild className='w-full'>
                        <Link href='/signup' onClick={() => setOpen(false)}>Sign Up</Link>
                      </Button>
                    </>
                  )
                )}
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
