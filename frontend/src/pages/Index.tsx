import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Brain, Upload, MessageSquare, Zap, FileText, Search, ArrowRight, Moon, Sun } from "lucide-react";

import Aurora from "@/components/reactbit/backgrounds/Aurora/Aurora";
import AnimatedText from "@/components/AnimatedText";
import DrawnExplanation from "@/assets/DrawnExplanation.png";
import FloatingShapes from "@/components/FloatingShapes";
import GradientMesh from "@/components/GradientMesh";

const Index = () => {
  const [isDark, setIsDark] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    document.documentElement.classList.add("dark");
  }, []);

  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle("dark");
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-background">
      {/* Background Layers */}
      <div className="absolute top-0 inset-x-0 h-[50vh] z-0 pointer-events-none">
        <Aurora
          colorStops={["#3A29FF", "#FF94B4", "#FF3232"]}
          blend={0.5}
          amplitude={1.0}
          speed={0.5}
        />
      </div>
      <GradientMesh />
      <FloatingShapes />

      {/* Foreground */}
      <div className="relative z-10">
        {/* Header */}
        <header className="flex justify-between items-center p-6">
          <div className="flex items-center space-x-2">
            <Brain className="w-8 h-8 text-primary animate-glow" />
            <span className="text-2xl font-bold font-space text-gradient">DocIQ</span>
          </div>
          <div className="flex items-center space-x-4">
            <Button variant="ghost" size="icon" onClick={toggleTheme} className="hover:bg-white/10 transition-all duration-300">
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </Button>
            <Button variant="outline" className="glass-morphism hover:neon-glow transition-all duration-300">Sign In</Button>
          </div>
        </header>

        {/* Hero Section */}
        <section className="max-w-6xl mx-auto px-6 pt-20 pb-32">
          <div className="text-center space-y-8">
            <h1 className="text-6xl md:text-8xl font-bold font-space leading-tight">
              Transform Documents into
              <span className="block text-gradient animate-gradient-x">
                <AnimatedText words={["Intelligence", "Conversations", "Insights", "Knowledge"]} />
              </span>
            </h1>
            <p className="text-xl md:text-2xl text-muted-foreground max-w-3xl mx-auto leading-relaxed">
              Upload any document and unlock AI-powered semantic search, intelligent Q&A, and instant insights that understand context like never before.
            </p>

            {/* CTA */}
            <div className="mt-12 max-w-2xl mx-auto">
              <Card className="p-8 glass-morphism neon-glow">
                <div className="space-y-6">
                  <div className="flex flex-col md:flex-row gap-4">
                    <Button size="lg" className="flex-1 bg-primary hover:bg-primary/90 text-primary-foreground animate-pulse-glow transition-all duration-300">
                      <Upload className="w-5 h-5 mr-2" />
                      Upload Document
                    </Button>
                    <Button size="lg" variant="outline" className="flex-1 glass-morphism hover:neon-glow transition-all duration-300" onClick={() => navigate("/dashboard")}>
                      <MessageSquare className="w-5 h-5 mr-2" />
                      Try Demo
                      <ArrowRight className="w-4 h-4 ml-2" />
                    </Button>
                  </div>

                  <div className="relative">
                    <Input placeholder="Ask anything about your documents..." className="glass-morphism text-lg py-6 pr-12 border-primary/20 focus:border-primary/50 transition-all duration-300" />
                    <Button size="icon" className="absolute right-2 top-1/2 -translate-y-1/2 bg-primary hover:bg-primary/90 animate-glow">
                      <Search className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </section>

        {/* Explainer Section */}
        <section className="max-w-6xl mx-auto px-6 pb-24">
          <div className="grid grid-cols-1 md:grid-cols-2 items-center gap-12">
            <div className="max-w-prose">
              <h2 className="text-2xl md:text-3xl font-semibold font-space mb-4">Meet DocIQ</h2>
              <p className="text-muted-foreground text-base md:text-lg leading-relaxed">
                Upload any PDF, slide deck, or webpage, and DocIQ transforms it into a live, searchable, ask-me-anything knowledge base. <br /><br />
                No more hunting for answers — just type your question and get instant AI-backed responses, complete with smart citations and deep contextual understanding.
              </p>
            </div>
            <div className="bg-white p-4 rounded-xl shadow-lg dark:invert">
              <img src={DrawnExplanation} alt="DocIQ flow diagram" className="w-full h-auto" />
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="max-w-6xl mx-auto px-6 pb-20">
          <div className="grid md:grid-cols-3 gap-8">
            {[
              {
                icon: <Brain className="w-8 h-8" />,
                title: "AI-Powered Intelligence",
                description: "Advanced semantic understanding that goes beyond keyword matching",
                gradient: "from-purple-500 to-pink-500",
              },
              {
                icon: <Zap className="w-8 h-8" />,
                title: "Lightning Fast",
                description: "Get instant answers from thousands of pages with vector search",
                gradient: "from-blue-500 to-cyan-500",
              },
              {
                icon: <FileText className="w-8 h-8" />,
                title: "Any Document Type",
                description: "PDFs, Word docs, text files, URLs - we handle it all seamlessly",
                gradient: "from-green-500 to-emerald-500",
              },
            ].map((feature, index) => (
              <Card key={index} className="p-6 glass-morphism hover:neon-glow transition-all duration-500 hover:scale-105 group">
                <div className={`w-16 h-16 rounded-2xl bg-gradient-to-r ${feature.gradient} flex items-center justify-center mb-4 text-white group-hover:animate-float`}>
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold mb-2 font-space">{feature.title}</h3>
                <p className="text-muted-foreground">{feature.description}</p>
              </Card>
            ))}
          </div>
        </section>

        {/* Final CTA */}
        <section className="text-center pb-20">
          <Button
            size="lg"
            onClick={() => navigate("/dashboard")}
            className="bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white px-8 py-4 text-lg font-semibold animate-pulse-glow transition-all duration-300"
          >
            Experience DocIQ
            <ArrowRight className="w-5 h-5 ml-2" />
          </Button>
        </section>
      </div>
    </div>
  );
};

export default Index;
