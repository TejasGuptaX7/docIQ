import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect, useRef, useState, useCallback } from "react";
import { Card } from "@/components/ui/card";

export default function EditorView() {
  const [hasTyped, setHasTyped] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const editor = useEditor({
    extensions: [StarterKit],
    content: `<p><span class="opacity-50">Start writing or ask something...</span></p>`,
    autofocus: true,
    editorProps: {
      attributes: {
        class: 'prose dark:prose-invert max-w-none outline-none focus:outline-none min-h-[200px] p-4',
      },
    },
    onUpdate: ({ editor }) => {
      const text = editor.getText().trim();
      setHasTyped(text.length > 0);
      
      // Clear placeholder content when user starts typing
      if (text.length > 0 && editor.getHTML().includes('opacity-50')) {
        editor.commands.setContent('');
      }
    },
    onCreate: ({ editor }) => {
      setIsLoading(false);
    },
  });

  // Enhanced focus management
  const focusEditor = useCallback(() => {
    if (editor && !editor.isFocused) {
      editor.commands.focus('end');
    }
  }, [editor]);

  // Force focus on load with retry mechanism
  useEffect(() => {
    if (editor && !isLoading) {
      const focusWithRetry = (attempts = 0) => {
        if (attempts < 3) {
          setTimeout(() => {
            if (editor && !editor.isFocused) {
              editor.commands.focus('end');
              focusWithRetry(attempts + 1);
            }
          }, 50 * (attempts + 1));
        }
      };
      focusWithRetry();
    }
  }, [editor, isLoading]);

  // Handle wrapper interactions
  const handleWrapperClick = useCallback((e: React.MouseEvent) => {
    // Only focus if clicking on the wrapper itself, not editor content
    if (e.target === wrapperRef.current) {
      focusEditor();
    }
  }, [focusEditor]);

  const handleWrapperKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !editor?.isFocused) {
      e.preventDefault();
      focusEditor();
    }
  }, [editor, focusEditor]);

  // Handle paste events for better UX
  useEffect(() => {
    const handlePaste = (e: ClipboardEvent) => {
      if (editor && !editor.isFocused) {
        e.preventDefault();
        focusEditor();
        // Re-trigger paste after focus
        setTimeout(() => {
          document.execCommand('paste');
        }, 10);
      }
    };

    document.addEventListener('paste', handlePaste);
    return () => document.removeEventListener('paste', handlePaste);
  }, [editor, focusEditor]);

  if (isLoading) {
    return (
      <Card className="w-full h-full p-6 bg-background/80 rounded-2xl shadow-md border-none">
        <div className="flex items-center justify-center h-full">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      </Card>
    );
  }

  if (!editor) {
    return (
      <Card className="w-full h-full p-6 bg-background/80 rounded-2xl shadow-md border-none">
        <p className="text-red-500 text-center">Editor failed to load. Please refresh the page.</p>
      </Card>
    );
  }

  return (
    <div className="relative w-full h-full">
      <Card
        ref={wrapperRef}
        className="w-full h-full bg-background/80 rounded-2xl shadow-md overflow-y-auto border-none outline-none ring-0 focus:outline-none focus:ring-0 cursor-text"
        onClick={handleWrapperClick}
        onKeyDown={handleWrapperKeyDown}
        tabIndex={0}
        role="textbox"
        aria-label="Text editor"
      >
        {!hasTyped && (
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center text-muted-foreground text-xl select-none opacity-60 transition-opacity duration-300 pointer-events-none z-10">
            <div className="space-y-2">
              <p>Press <kbd className="bg-card px-2 py-1 rounded border border-border text-sm font-mono">Enter</kbd> to start writing...</p>
              <p className="text-sm opacity-75">or click anywhere to begin</p>
            </div>
          </div>
        )}

        <EditorContent
          editor={editor}
          className="h-full w-full [&_.ProseMirror]:outline-none [&_.ProseMirror]:ring-0 [&_.ProseMirror]:border-none [&_.ProseMirror]:min-h-full [&_.ProseMirror]:p-6"
        />
      </Card>
    </div>
  );
}