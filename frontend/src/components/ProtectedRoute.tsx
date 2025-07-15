import { ReactNode } from 'react';
import { useAuth, RedirectToSignIn } from '@clerk/clerk-react';

interface ProtectedRouteProps {
  children: ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isSignedIn, isLoaded } = useAuth();

  if (!isLoaded) return null; // Wait until Clerk loads
  if (!isSignedIn) return <RedirectToSignIn />;

  return <>{children}</>;
}
