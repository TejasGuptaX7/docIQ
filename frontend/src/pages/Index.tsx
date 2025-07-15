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
  Brain,
  Upload,
  MessageSquare,
  Zap,
  FileText,
  Search,
  ArrowRight,
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

  return (
    <div className="relative min-h-screen overflow-hidden bg-background">
      {/* background layers */}
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

      {/* foreground */}
      <div className="relative z-10">
        {/* header */}
        <header className="flex items-center justify-between p-6">
          <div className="flex items-center space-x-2">
            <Brain className="w-8 h-8 text-primary animate-glow" />
            <span className="text-2xl font-bold font-space text-gradient">DocIQ</span>
          </div>
          <div className="flex items-center space-x-4">
            <Button variant="ghost" size="icon" onClick={toggleTheme}>
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </Button>

            {/* show Sign In when signed out */}
            <SignedOut>
              <SignInButton mode="modal">
                <Button variant="outline" className="glass-morphism">Sign In</Button>
              </SignInButton>
            </SignedOut>

            {/* once signed in, show profile & Sign Out */}
            <SignedIn>
              <UserButton />
              <SignOutButton>
                <Button variant="outline" className="glass-morphism">Sign Out</Button>
              </SignOutButton>
            </SignedIn>
          </div>
        </header>

        {/* hero */}
        <section className="max-w-6xl mx-auto px-6 pt-20 pb-32">
          <div className="text-center space-y-8">
            <h1 className="text-6xl md:text-8xl font-bold font-space leading-tight">
              Transform Documents into
              <span className="block text-gradient animate-gradient-x">
                <AnimatedText words={['Intelligence', 'Conversations', 'Insights', 'Knowledge']} />
              </span>
            </h1>
            <p className="text-xl md:text-2xl text-muted-foreground max-w-3xl mx-auto">
              Upload any document and unlock AI-powered semantic search, intelligent Q&A,
              and instant insights.
            </p>

            {/* CTA card */}
            <div className="mt-12 max-w-2xl mx-auto">
              <Card className="p-8 glass-morphism">
                <div className="space-y-6">
                  <div className="flex flex-col md:flex-row gap-4">
                    <Button size="lg" className="flex-1 bg-primary text-primary-foreground">
                      <Upload className="w-5 h-5 mr-2" />
                      Upload Document
                    </Button>

                    {/* Try Demo button */}
                    <SignedOut>
                      <SignInButton mode="modal">
                        <Button size="lg" variant="outline" className="flex-1 glass-morphism">
                          <MessageSquare className="w-5 h-5 mr-2" />
                          Try Demo
                          <ArrowRight className="w-4 h-4 ml-2" />
                        </Button>
                      </SignInButton>
                    </SignedOut>
                    <SignedIn>
                      <Button
                        size="lg"
                        variant="outline"
                        className="flex-1 glass-morphism"
                        onClick={() => navigate('/dashboard')}
                      >
                        <MessageSquare className="w-5 h-5 mr-2" />
                        Try Demo
                        <ArrowRight className="w-4 h-4 ml-2" />
                      </Button>
                    </SignedIn>
                  </div>

                  <div className="relative">
                    <Input
                      placeholder="Ask anything about your documents…"
                      className="py-6 pr-12 glass-morphism"
                    />
                    <Button
                      size="icon"
                      className="absolute right-2 top-1/2 -translate-y-1/2 bg-primary"
                    >
                      <Search className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </section>

        {/* explainer */}
        <section className="max-w-6xl mx-auto px-6 pb-24">
          <div className="grid grid-cols-1 md:grid-cols-2 items-center gap-12">
            <div className="max-w-prose">
              <h2 className="text-3xl font-semibold font-space mb-4">Meet DocIQ</h2>
              <p className="text-muted-foreground text-lg leading-relaxed">
                Upload any PDF, slide deck, or webpage, and DocIQ transforms it into a live,
                searchable, ask-me-anything knowledge base.
                No more hunting for answers — just type your question and get instant,
                AI-backed responses.
              </p>
            </div>
            <div className="bg-white p-4 rounded-xl shadow-lg dark:invert">
              <img src={DrawnExplanation} alt="DocIQ flow" className="w-full" />
            </div>
          </div>
        </section>

        {/* features */}
        <section className="max-w-6xl mx-auto px-6 pb-20">
          <div className="grid md:grid-cols-3 gap-8">
            {[
              { icon: <Brain />, title: 'AI-Powered', desc: 'Semantic understanding', grad: 'from-purple-500 to-pink-500' },
              { icon: <Zap />, title: 'Fast', desc: 'Vector search', grad: 'from-blue-500 to-cyan-500' },
              { icon: <FileText />, title: 'Any Doc', desc: 'PDF, Word, URLs', grad: 'from-green-500 to-emerald-500' },
            ].map((f, i) => (
              <Card key={i} className="p-6 glass-morphism hover:scale-105 transition">
                <div className={`w-16 h-16 mb-4 rounded-2xl bg-gradient-to-r ${f.grad}
                                 flex items-center justify-center text-white`}>
                  {f.icon}
                </div>
                <h3 className="text-xl font-semibold mb-2">{f.title}</h3>
                <p className="text-muted-foreground">{f.desc}</p>
              </Card>
            ))}
          </div>
        </section>

        {/* final CTA */}
        <section className="text-center pb-20">
          <SignedOut>
            <SignInButton mode="modal">
              <Button
                size="lg"
                className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-8 py-4 text-lg font-semibold"
              >
                Experience DocIQ <ArrowRight className="w-5 h-5 ml-2" />
              </Button>
            </SignInButton>
          </SignedOut>
          <SignedIn>
            <Button
              size="lg"
              onClick={() => navigate('/dashboard')}
              className="bg-gradient-to-r from-purple-500 to-pink-500 text-white px-8 py-4 text-lg font-semibold"
            >
              Experience DocIQ <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
          </SignedIn>
        </section>
      </div>
    </div>
  );
}
