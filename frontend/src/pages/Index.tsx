// src/pages/Index.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  SignedIn,
  SignedOut,
  SignInButton,
  UserButton
} from '@clerk/clerk-react';
import {
  Upload,
  MessageSquare,
  Zap,
  FileText,
  Search,
  Lock,
  Heart,
  Star,
  ExternalLink
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';

import Aurora from '@/components/reactbit/backgrounds/Aurora/Aurora';
import AnimatedText from '@/components/AnimatedText';
import DrawnExplanation from '@/assets/DrawnExplanation.png';
import FloatingShapes from '@/components/FloatingShapes';
import GradientMesh from '@/components/GradientMesh';
import { Link } from 'react-router-dom'; 
import { Navigation } from '@/components/Navigation';
import { GlassCard } from '@/components/GlassCard';

export default function Index() {
  const navigate = useNavigate();

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

  const scrollToPricing = () => {
    const pricingSection = document.getElementById('pricing');
    if (pricingSection) {
      pricingSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

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
              <img src="/dociq-logo.png" alt="docIQ Logo" className="h-10 w-10" />
              <span className="font-space font-bold text-xl tracking-tight">docIQ</span>
            </div>
            <div className="hidden md:flex items-center gap-8">
              <a href="#features" className="text-sm font-medium hover:text-primary transition-colors">Features</a>
              <button 
                onClick={scrollToPricing}
                className="text-sm font-medium hover:text-primary transition-colors cursor-pointer"
              >
                Pricing
              </button>
              <a href="#docs" className="text-sm font-medium hover:text-primary transition-colors">Docs</a>
              <SignedIn>
                <a href="/dashboard" className="text-sm font-medium hover:text-primary transition-colors">Dashboard</a>
              </SignedIn>
            </div>
            <div className="flex items-center gap-4">
              <SignedOut>
                <SignInButton mode="modal">
                  <Button variant="default" size="sm" className="font-space font-medium">
                    <Upload className="h-4 w-4" />
                    Sign In
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

      {/* Hero Section */}
      <section className="min-h-screen flex items-center justify-center px-4 pt-24">
        <div className="text-center max-w-5xl mx-auto animate-fade-in-up">
          <h1 className="text-6xl md:text-8xl font-space font-extrabold mb-6 tracking-tight leading-[0.9]">
            Transform Documents into{' '}
            <AnimatedText
              words={['Intelligence', 'Conversations', 'Insights', 'Knowledge']}
              className="text-primary inline bg-gradient-to-r from-purple-400 via-pink-500 to-blue-500 bg-clip-text text-transparent"
            />
          </h1>
          <p className="text-xl md:text-2xl text-muted-foreground max-w-3xl mx-auto leading-relaxed font-inter font-light mb-10">
            Upload any document and unlock AI-powered semantic search, intelligent Q&A, and instant insights. 
            Your personal knowledge companion that actually understands context.
          </p>
          <div className="mt-8">
            <SignedOut>
              <SignInButton mode="modal" >
                <Button 
                  size="xl" 
                  className="min-w-[240px] font-space font-semibold text-lg px-8 py-4 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 transition-all duration-300 transform hover:scale-105 shadow-2xl"
                >
                  Start Building Your Knowledge Base
                </Button>
              </SignInButton>
            </SignedOut>
            <SignedIn>
              <Button
                size="xl"
                className="min-w-[240px] font-space font-semibold text-lg px-8 py-4 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 transition-all duration-300 transform hover:scale-105 shadow-2xl"
                onClick={() => navigate('/dashboard')}
              >
                Start Building Your Knowledge Base
              </Button>
            </SignedIn>
          </div>
          <p className="text-sm text-muted-foreground mt-8 font-inter">
            PDF, DOCX, TXT supported • No credit card required • Start organizing your thoughts today
          </p>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-24 px-4">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-20">
            <h2 className="text-5xl md:text-6xl font-space font-bold mb-8 tracking-tight">
              Why Choose <span className="text-gradient bg-gradient-to-r from-purple-400 via-pink-500 to-blue-500 bg-clip-text text-transparent">docIQ</span>?
            </h2>
            <p className="text-xl text-muted-foreground max-w-3xl mx-auto font-inter font-light leading-relaxed">
              Transform how you interact with documents using cutting-edge AI technology. 
              Stop searching through endless files—start having conversations with your knowledge.
            </p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, idx) => (
              <GlassCard key={idx} variant="feature" className="p-8 h-full group hover:scale-105 transition-all duration-300">
                <div className="text-center">
                  <div className="w-16 h-16 bg-gradient-to-br from-primary/30 to-purple-600/30 rounded-2xl flex items-center justify-center mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                    <feature.icon className="h-8 w-8 text-primary" />
                  </div>
                  <h3 className="font-space font-bold text-xl mb-4 tracking-tight">{feature.title}</h3>
                  <p className="text-muted-foreground text-sm leading-relaxed font-inter">{feature.description}</p>
                </div>
              </GlassCard>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section id="pricing" className="py-24 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="mb-16">
            <h2 className="text-5xl md:text-6xl font-space font-bold mb-8 tracking-tight">
              Pricing? <span className="text-gradient bg-gradient-to-r from-green-400 to-blue-500 bg-clip-text text-transparent">It's Free!</span>
            </h2>
            <p className="text-xl text-muted-foreground font-inter font-light leading-relaxed mb-8">
              We're not here to drain your wallet. We're building the future of document intelligence, 
              and we want you to be part of the journey.
            </p>
          </div>
          
          <GlassCard className="p-12 max-w-2xl mx-auto group hover:scale-105 transition-all duration-300">
            <div className="flex items-center justify-center mb-8">
              <Heart className="h-12 w-12 text-red-500 animate-pulse" />
            </div>
            <h3 className="text-3xl font-space font-bold mb-6 tracking-tight">
              Pay us with your <span className="text-gradient bg-gradient-to-r from-yellow-400 to-orange-500 bg-clip-text text-transparent">honest feedback</span>
            </h3>
            <p className="text-lg text-muted-foreground mb-8 font-inter leading-relaxed">
              All we ask is for an unhinged, brutally honest review about your experience. 
              Tell us what's amazing, what sucks, and what would make you recommend us to your friends.
            </p>
            <div className="flex items-center justify-center gap-2 mb-8">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="h-6 w-6 text-yellow-400 fill-current" />
              ))}
            </div>
            <a 
              href="https://tally.so/r/nGAveo" 
              target="_blank" 
              rel="noopener noreferrer"
              className="inline-block"
            >
              <Button 
                size="lg" 
                className="font-space font-semibold bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 text-white px-8 py-4 text-lg transition-all duration-300 transform hover:scale-105 shadow-xl"
              >
                Leave Your Review <ExternalLink className="w-5 h-5 ml-2" />
              </Button>
            </a>
          </GlassCard>
        </div>
      </section>

      {/* Explainer Section */}
      <section className="max-w-6xl mx-auto px-6 pb-24">
        <div className="grid grid-cols-1 md:grid-cols-2 items-center gap-16">
          <div className="max-w-prose">
            <h2 className="text-4xl font-space font-bold mb-6 tracking-tight">Meet docIQ</h2>
            <p className="text-muted-foreground text-lg leading-relaxed font-inter mb-6">
              Upload any PDF, slide deck, or webpage, and docIQ transforms it into a live,
              searchable, ask-me-anything knowledge base.
            </p>
            <p className="text-muted-foreground text-lg leading-relaxed font-inter">
              No more hunting for answers in endless documents. No more losing track of important insights. 
              Just type your question and get instant, AI-backed responses from your personal knowledge vault.
            </p>
          </div>
          <div className="bg-white/5 p-6 rounded-2xl shadow-2xl backdrop-blur-sm border border-white/10 hover:scale-105 transition-all duration-300">
            <img src={DrawnExplanation} alt="docIQ flow" className="w-full rounded-lg" />
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="text-center pb-20">
        <div className="mb-8">
          <h3 className="text-3xl font-space font-bold mb-4 tracking-tight">
            Ready to revolutionize how you work with documents?
          </h3>
          <p className="text-lg text-muted-foreground font-inter font-light">
          </p>
        </div>
        <SignedOut>
          <SignInButton mode="modal" >
            <Button
              size="lg"
              className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white px-12 py-4 text-lg font-space font-semibold transition-all duration-300 transform hover:scale-105 shadow-2xl"
            >
              Experience docIQ Now <MessageSquare className="w-5 h-5 ml-2" />
            </Button>
          </SignInButton>
        </SignedOut>
        <SignedIn>
          <Button
            size="lg"
            onClick={() => navigate('/dashboard')}
            className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white px-12 py-4 text-lg font-space font-semibold transition-all duration-300 transform hover:scale-105 shadow-2xl"
          >
            Experience docIQ Now <MessageSquare className="w-5 h-5 ml-2" />
          </Button>
        </SignedIn>
      </section>
    </div>
  );
}