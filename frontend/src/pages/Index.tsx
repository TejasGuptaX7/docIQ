// src/pages/Index.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  SignedIn,
  SignedOut,
  SignInButton,
  SignOutButton,
  UserButton
} from '@clerk/clerk-react';
import {
  Upload,
  MessageSquare,
  Zap,
  FileText,
  Search,
  Lock,
  Moon,
  Sun
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';

import Aurora from '@/components/reactbit/backgrounds/Aurora/Aurora';
import AnimatedText from '@/components/AnimatedText';
import DrawnExplanation from '@/assets/DrawnExplanation.png';
import FloatingShapes from '@/components/FloatingShapes';
import GradientMesh from '@/components/GradientMesh';

import { Navigation } from '@/components/Navigation';
import { GlassCard } from '@/components/GlassCard';

export default function Index() {
  const [isDark, setIsDark] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    document.documentElement.classList.add('dark');
  }, []);

  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle('dark');
  };

  const features = [
    {
      icon: Search,
      title: 'Semantic Search',
      description: 'Find information using natural language queries, not just keywords.'
    },
    {
      icon: MessageSquare,
      title: 'Intelligent Q&A',
      description: 'Ask questions about your documents and get contextual answers instantly.'
    },
    {
      icon: Zap,
      title: 'Instant Insights',
      description: 'Extract key themes, summaries, and actionable insights automatically.'
    },
    {
      icon: Lock,
      title: 'Privacy First',
      description: 'Your documents stay private and secure, processed locally when possible.'
    }
  ];

  return (
    <div className="relative min-h-screen overflow-hidden bg-gradient-main">
      {/* Background layers */}
      <div className="absolute inset-x-0 top-0 h-[50vh] pointer-events-none z-0">
        <Aurora
          colorStops={['#3A29FF', '#FF94B4', '#FF3232']}
          blend={0.5}
          amplitude={1}
          speed={0.5}
        />
      </div>
      <GradientMesh />
      <FloatingShapes />

      {/* Navigation with Clerk integration */}
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
              <Button variant="ghost" size="icon" onClick={toggleTheme}>
                {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
              </Button>
              <SignedOut>
                <SignInButton mode="modal">
                  <Button variant="default" size="sm" className="font-mono">
                    <Upload className="h-4 w-4" />
                    Sign In
                  </Button>
                </SignInButton>
              </SignedOut>
              <SignedIn>
                <UserButton />
                <SignOutButton>
                  <Button variant="ghost" size="sm">Sign Out</Button>
                </SignOutButton>
              </SignedIn>
            </div>
          </nav>
        </GlassCard>
      </div>

      {/* Hero Section */}
      <section className="min-h-screen flex items-center justify-center px-4 pt-24">
        <div className="text-center max-w-4xl mx-auto animate-fade-in-up">
          <h1 className="text-5xl md:text-7xl font-mono font-bold mb-4">
            Transform Documents into{' '}
            <AnimatedText
              words={['Intelligence', 'Conversations', 'Insights', 'Knowledge']}
              className="text-primary inline"
            />
          </h1>
          <p className="text-xl md:text-2xl text-muted-foreground max-w-2xl mx-auto leading-relaxed">
            Upload any document and unlock AI-powered semantic search, intelligent Q&A, and instant insights.
          </p>
          <div className="mt-8">
            <SignedOut>
              <SignInButton mode="modal" afterSignInUrl="/dashboard">
                <Button variant="default" size="xl" className="min-w-[200px] font-mono">
                  Try docIQ for Free
                </Button>
              </SignInButton>
            </SignedOut>
            <SignedIn>
              <Button
                variant="default"
                size="xl"
                className="min-w-[200px] font-mono"
                onClick={() => navigate('/dashboard')}
              >
                Try docIQ for Free
              </Button>
            </SignedIn>
          </div>
          <p className="text-sm text-muted-foreground mt-6">
            PDF, DOCX, TXT supported • No signup required
          </p>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-24 px-4">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-mono font-bold mb-6">
              Why Choose <span className="text-primary">docIQ</span>?
            </h2>
            <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
              Transform how you interact with documents using cutting-edge AI technology.
            </p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, idx) => (
              <GlassCard key={idx} variant="feature" className="p-6 h-full">
                <div className="text-center">
                  <div className="w-12 h-12 bg-primary/20 rounded-lg flex items-center justify-center mx-auto mb-4">
                    <feature.icon className="h-6 w-6 text-primary" />
                  </div>
                  <h3 className="font-mono font-semibold text-lg mb-3">{feature.title}</h3>
                  <p className="text-muted-foreground text-sm leading-relaxed">{feature.description}</p>
                </div>
              </GlassCard>
            ))}
          </div>
        </div>
      </section>

      {/* Explainer Section */}
      <section className="max-w-6xl mx-auto px-6 pb-24">
        <div className="grid grid-cols-1 md:grid-cols-2 items-center gap-12">
          <div className="max-w-prose">
            <h2 className="text-3xl font-semibold font-mono mb-4">Meet docIQ</h2>
            <p className="text-muted-foreground text-lg leading-relaxed">
              Upload any PDF, slide deck, or webpage, and docIQ transforms it into a live,
              searchable, ask-me-anything knowledge base. No more hunting for answers — just type your question and get instant, AI-backed responses.
            </p>
          </div>
          <div className="bg-white p-4 rounded-xl shadow-lg dark:invert">
            <img src={DrawnExplanation} alt="docIQ flow" className="w-full" />
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="text-center pb-20">
        <SignedOut>
          <SignInButton mode="modal">
            <Button
              size="lg"
              className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-8 py-4 text-lg font-semibold"
            >
              Experience docIQ <MessageSquare className="w-5 h-5 ml-2" />
            </Button>
          </SignInButton>
        </SignedOut>
        <SignedIn>
          <Button
            size="lg"
            onClick={() => navigate('/dashboard')}
            className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-8 py-4 text-lg font-semibold"
          >
            Experience docIQ <MessageSquare className="w-5 h-5 ml-2" />
          </Button>
        </SignedIn>
      </section>
    </div>
  );
}
