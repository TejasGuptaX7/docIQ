import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect, useRef, useState } from "react";
import { Card } from "@/components/ui/card";

export default function EditorView() {
  const [hasTyped, setHasTyped] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const editor = useEditor({
    extensions: [StarterKit],
    content: `<p><span class="opacity-50">Start writing or ask something...</span></p>`,
    autofocus: true,
    onUpdate: ({ editor }) => {
      setHasTyped(editor.getText().trim().length > 0);
    },
  });

  // Force focus on load (more reliable than autofocus alone)
  useEffect(() => {
    if (editor) {
      setTimeout(() => {
        editor.commands.focus();
      }, 50);
    }
  }, [editor]);

  // Focus editor when clicking anywhere on the card
  const handleWrapperClick = (e: React.MouseEvent) => {
    if (editor && !editor.isFocused) {
      editor.commands.focus();
    }
  };

  // Focus editor when pressing Enter anywhere on the card
  const handleWrapperKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && editor && !editor.isFocused) {
      e.preventDefault();
      editor.commands.focus();
    }
  };

  if (!editor) return <p className="text-red-500 p-4">Editor failed to load.</p>;

  return (
    <Card
      ref={wrapperRef}
      className="w-full h-full p-6 bg-background/80 rounded-2xl shadow-md overflow-y-auto border-none outline-none ring-0 focus:outline-none focus:ring-0"
      onClick={handleWrapperClick}
      onKeyDown={handleWrapperKeyDown}
      tabIndex={0}
    >
      {!hasTyped && (
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center text-muted-foreground text-xl select-none opacity-60 transition-opacity duration-300 pointer-events-none">
          Press <kbd className="bg-card px-1 py-0.5 rounded border border-border text-sm">Enter</kbd> to start a new page...
        </div>
      )}

      <EditorContent
        editor={editor}
        className="prose dark:prose-invert max-w-none outline-none ring-0 border-none shadow-none focus:outline-none focus:ring-0 [contenteditable]:outline-none [contenteditable]:ring-0 [contenteditable]:border-none"
      />
    </Card>
  );
}
