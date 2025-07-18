// src/components/AssistantPanel.tsx
import { useState, useRef, useEffect } from "react";
import ChatInterface from "./ChatInterface";
import { GlassCard } from "@/components/GlassCard";
import { GripVertical } from "lucide-react";

interface Props {
  selectedDoc: string | null;
  panelWidth?: number;
}

export default function AssistantPanel({ selectedDoc, panelWidth = 380 }: Props) {
  const [width, setWidth] = useState(panelWidth);
  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef<HTMLDivElement>(null);

  // Update width when panelWidth prop changes
  useEffect(() => {
    setWidth(panelWidth);
  }, [panelWidth]);

  // Handle mouse movement during drag
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging) return;
      
      const newWidth = window.innerWidth - e.clientX;
      // Constrain width between 280px and 600px
      const constrainedWidth = Math.max(280, Math.min(600, newWidth));
      setWidth(constrainedWidth);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isDragging]);

  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  return (
    <div 
      className="flex-shrink-0 relative"
      style={{ width: `${width}px` }}
    >
      {/* Resize handle */}
      <div
        ref={dragRef}
        className="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary/20 transition-colors z-10"
        onMouseDown={handleMouseDown}
      >
        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 opacity-0 hover:opacity-100 transition-opacity">
          <GripVertical className="h-4 w-4 text-primary/50" />
        </div>
      </div>
      
      {/* Glass Card wrapping the entire chat */}
      <GlassCard variant="feature" className="h-full m-2 ml-0 flex flex-col overflow-hidden">
        <ChatInterface selectedDoc={selectedDoc} />
      </GlassCard>
    </div>
  );
}