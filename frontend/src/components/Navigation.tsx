// src/components/Navigation.tsx
import { Button } from "@/components/ui/button";
import { GlassCard } from "@/components/GlassCard";
import { FileText, Download } from "lucide-react";
import { SignedIn, SignedOut, SignInButton, UserButton } from '@clerk/clerk-react';

export function Navigation() {
  return (
    <div className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50 w-full max-w-6xl px-4">
      <GlassCard variant="nav" className="px-6 py-3">
        <nav className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileText className="h-6 w-6 text-primary" />
            <span className="font-mono font-bold text-lg">docIQ</span>
          </div>
          
          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-sm hover:text-primary transition-colors">Features</a>
            <a href="#pricing" className="text-sm hover:text-primary transition-colors">Pricing</a>
            <a href="#docs" className="text-sm hover:text-primary transition-colors">Docs</a>
            <SignedIn>
              <a href="/dashboard" className="text-sm hover:text-primary transition-colors">Dashboard</a>
            </SignedIn>
          </div>
          
          <div className="flex items-center gap-4">
            <SignedOut>
              <SignInButton mode="modal">
                <Button variant="default" size="sm" className="font-mono">
                  <Download className="h-4 w-4" />
                  Download
                </Button>
              </SignInButton>
            </SignedOut>
            
            <SignedIn>
              <UserButton />
            </SignedIn>
          </div>
        </nav>
      </GlassCard>
    </div>
  );
}