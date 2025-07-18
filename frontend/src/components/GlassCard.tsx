// src/components/GlassCard.tsx
import { cn } from "@/lib/utils";

interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  variant?: 'default' | 'nav' | 'feature';
}

export function GlassCard({ children, className, variant = 'default' }: GlassCardProps) {
  const variants = {
    default: "bg-glass/20 backdrop-blur-glass border border-glass-border shadow-glass",
    nav: "bg-glass/15 backdrop-blur-glass border border-glass-border/50 shadow-glass",
    feature: "bg-gradient-card backdrop-blur-glass border border-glass-border/30 shadow-glass hover:bg-glass/25 transition-all duration-300"
  };

  return (
    <div className={cn(
      "rounded-lg",
      variants[variant],
      className
    )}>
      {children}
    </div>
  );
}