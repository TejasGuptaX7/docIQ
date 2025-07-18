// src/components/FeatureSection.tsx
import { GlassCard } from "@/components/GlassCard";
import { Search, MessageSquare, Zap, Lock } from "lucide-react";

export function FeatureSection() {
  const features = [
    {
      icon: Search,
      title: "Semantic Search",
      description: "Find information using natural language queries, not just keywords."
    },
    {
      icon: MessageSquare,
      title: "Intelligent Q&A",
      description: "Ask questions about your documents and get contextual answers instantly."
    },
    {
      icon: Zap,
      title: "Instant Insights",
      description: "Extract key themes, summaries, and actionable insights automatically."
    },
    {
      icon: Lock,
      title: "Privacy First",
      description: "Your documents stay private and secure, processed locally when possible."
    }
  ];

  return (
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
          {features.map((feature, index) => (
            <GlassCard key={index} variant="feature" className="p-6 h-full">
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
  );
}