import { FileText, Brain, Zap, MessageSquare } from "lucide-react";

const FloatingShapes = () => {
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none">
      {/* Large floating documents */}
      <div className="floating-shape top-20 left-10 animate-float" style={{ animationDelay: '0s' }}>
        <FileText className="w-32 h-32 text-primary/10" />
      </div>
      
      <div className="floating-shape top-40 right-16 animate-float" style={{ animationDelay: '2s' }}>
        <Brain className="w-28 h-28 text-blue-500/10" />
      </div>
      
      <div className="floating-shape bottom-32 left-20 animate-float" style={{ animationDelay: '4s' }}>
        <Zap className="w-24 h-24 text-cyan-500/10" />
      </div>
      
      <div className="floating-shape bottom-20 right-32 animate-float" style={{ animationDelay: '1s' }}>
        <MessageSquare className="w-20 h-20 text-pink-500/10" />
      </div>

      {/* Smaller geometric shapes */}
      <div className="absolute top-32 left-1/3 w-4 h-4 bg-primary/20 rounded-full animate-float" style={{ animationDelay: '3s' }}></div>
      <div className="absolute top-60 right-1/4 w-6 h-6 bg-blue-500/20 rotate-45 animate-float" style={{ animationDelay: '1.5s' }}></div>
      <div className="absolute bottom-40 left-1/2 w-3 h-3 bg-cyan-500/20 rounded-full animate-float" style={{ animationDelay: '2.5s' }}></div>
      <div className="absolute bottom-60 right-1/3 w-5 h-5 bg-pink-500/20 rotate-12 animate-float" style={{ animationDelay: '0.5s' }}></div>
    </div>
  );
};

export default FloatingShapes;
