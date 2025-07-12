
import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search, X, FileText, MessageSquare, Zap } from "lucide-react";

interface FloatingCommandInputProps {
  onClose: () => void;
}

const FloatingCommandInput = ({ onClose }: FloatingCommandInputProps) => {
  const [query, setQuery] = useState("");

  const quickActions = [
    {
      icon: <FileText className="w-4 h-4" />,
      label: "Search Documents",
      action: "search docs"
    },
    {
      icon: <MessageSquare className="w-4 h-4" />,
      label: "Ask AI",
      action: "ask"
    },
    {
      icon: <Zap className="w-4 h-4" />,
      label: "Quick Analysis",
      action: "analyze"
    }
  ];

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-start justify-center pt-32">
      <Card className="w-full max-w-2xl mx-4 glass-morphism neon-glow animate-scale-in">
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold font-space">Quick Command</h3>
            <Button variant="ghost" size="icon" onClick={onClose}>
              <X className="w-4 h-4" />
            </Button>
          </div>
          
          <div className="relative mb-6">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Type a command or question..."
              className="pl-10 py-3 text-lg glass-morphism border-primary/20 focus:border-primary/50"
              autoFocus
            />
          </div>
          
          <div className="space-y-2">
            <p className="text-sm text-muted-foreground mb-3">Quick Actions:</p>
            {quickActions.map((action, index) => (
              <Button
                key={index}
                variant="ghost"
                className="w-full justify-start glass-morphism hover:neon-glow"
                onClick={() => {
                  setQuery(action.action + " ");
                }}
              >
                {action.icon}
                <span className="ml-3">{action.label}</span>
              </Button>
            ))}
          </div>
        </div>
      </Card>
    </div>
  );
};

export default FloatingCommandInput;
