// src/pages/NotFound.tsx
import { useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/GlassCard";
import { Home, Search } from "lucide-react";

const NotFound = () => {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    console.error(
      "404 Error: User attempted to access non-existent route:",
      location.pathname
    );
  }, [location.pathname]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-main px-4">
      <GlassCard variant="feature" className="max-w-md w-full p-8 text-center">
        <div className="mb-6">
          <h1 className="text-8xl font-mono font-bold text-primary mb-4">404</h1>
          <p className="text-2xl font-mono text-foreground mb-2">Page Not Found</p>
          <p className="text-muted-foreground">
            Oops! The page you're looking for doesn't exist.
          </p>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Button 
            variant="default" 
            onClick={() => navigate('/')}
            className="font-mono"
          >
            <Home className="h-4 w-4" />
            Back to Home
          </Button>
          <Button 
            variant="outline" 
            onClick={() => navigate('/dashboard')}
            className="font-mono"
          >
            <Search className="h-4 w-4" />
            Go to Dashboard
          </Button>
        </div>
      </GlassCard>
    </div>
  );
};

export default NotFound;