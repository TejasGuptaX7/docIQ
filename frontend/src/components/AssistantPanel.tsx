import { useState, useRef, useEffect } from "react";
import ChatInterface from "./ChatInterface";

interface Props {
  selectedDoc: string | null;
}

export default function AssistantPanel({ selectedDoc }: Props) {
  const [width, setWidth] = useState(360);
  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef<HTMLDivElement>(null);

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
    <aside 
      className="border-l border-border/50 bg-card/60 backdrop-blur-sm flex"
      style={{ width: `${width}px` }}
    >
      {/* Draggable handle */}
      <div
        ref={dragRef}
        className="w-1 bg-border/50 hover:bg-border cursor-col-resize transition-colors"
        onMouseDown={handleMouseDown}
      />
      
      {/* Chat content */}
      <div className="flex-1 p-4">
        <ChatInterface selectedDoc={selectedDoc} />
      </div>
    </aside>
  );
}
