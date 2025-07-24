// src/components/ChatInterface.tsx
import { useState, useRef, useEffect } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Send, Brain, Globe } from 'lucide-react';
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area';

interface Props { selectedDoc: string | null }

interface Msg {
  id: string;
  type: 'user' | 'ai';
  content: string;
  timestamp: Date;
  sources?: { page: number; excerpt: string; confidence: number }[];
}

interface Snip {
  id: string;
  docId: string;
  filename: string;
  text: string;
  page: number;
  start: number;
  end: number;
}

const shorten = (s?: string) =>
  s && s.length > 18 ? `${s.slice(0, 14)}…${s.slice(-3)}` : (s ?? 'untitled');

export default function ChatInterface({ selectedDoc }: Props) {
  const { getToken } = useAuth();
  const [messages, setMessages] = useState<Msg[]>([{
    id: 'sys',
    type: 'ai',
    content: "Hello! I'm your AI assistant. Ask me about your documents.",
    timestamp: new Date(),
  }]);
  const [snips, setSnips]   = useState<Snip[]>([]);
  const [input, setInput]   = useState('');
  const [typing, setTyping] = useState(false);
  const [global, setGlobal] = useState(false);

  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (endRef.current) {
      endRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  useEffect(() => {
    const onAdd = (e: any) => {
      const d = e.detail as Snip;
      setSnips(p => [...p, { ...d, id: crypto.randomUUID() }]);
    };
    window.addEventListener('add-selection-to-chat', onAdd);
    return () => window.removeEventListener('add-selection-to-chat', onAdd);
  }, []);

  const ctxString = () =>
    snips
      .map(s => `[${shorten(s.filename)} p.${s.page} ${s.start}-${s.end}] ${s.text}`)
      .join('\n\n');

  const send = async () => {
    const q = (ctxString() + '\n\n' + input).trim();
    if (!q) return;
    if (!global && !selectedDoc) return;

    setMessages(m => [...m, {
      id: crypto.randomUUID(),
      type: 'user',
      content: input,
      timestamp: new Date()
    }]);
    setInput('');
    setSnips([]);
    setTyping(true);

    try {
      const token = await getToken();
      const res = await fetch('https://api.dociq.tech/api/search', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          query: q,
          docId: global ? '' : selectedDoc
        }),
      });

      const data    = await res.json();
      const answer  = data.error ?? data.answer ?? 'No response.';
      const sources = data.sources;

      const aiId = crypto.randomUUID();
      setMessages(m => [...m, {
        id: aiId,
        type: 'ai',
        content: '',
        timestamp: new Date(),
        sources
      }]);

      // Typing animation
      [...answer].forEach((ch, i) =>
        setTimeout(() =>
          setMessages(m =>
            m.map(msg =>
              msg.id === aiId
                ? { ...msg, content: answer.slice(0, i + 1) }
                : msg
            )
          ), i * 18)
      );

      setTimeout(() => setTyping(false), answer.length * 18 + 100);

    } catch (err) {
      console.error(err);
      setMessages(m => [...m, {
        id: crypto.randomUUID(),
        type: 'ai',
        content: '⚠️ Backend error',
        timestamp: new Date()
      }]);
      setTyping(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Toggle at the top - now properly visible */}
      <div className="px-3 py-2 border-b border-glass-border flex justify-between items-center">
        <h3 className="text-sm font-mono font-semibold">AI Assistant</h3>
        <div className="flex items-center gap-2">
          <Globe className="w-3 h-3 opacity-60" />
          <Switch 
            id="scope" 
            checked={global} 
            onCheckedChange={setGlobal}
            className="scale-75"
          />
          <Label htmlFor="scope" className="cursor-pointer select-none text-xs">
            {global ? 'All docs' : 'Current doc'}
          </Label>
        </div>
      </div>

      {!selectedDoc && !global ? (
        <div className="flex-1 flex items-center justify-center p-4">
          <Card className="p-6 max-w-sm text-center bg-glass/20 border-glass-border">
            <Brain className="w-10 h-10 mx-auto mb-3 text-primary" />
            <p className="text-sm">Select a document or switch to "All docs" mode to begin.</p>
          </Card>
        </div>
      ) : (
        <>
          {/* Messages area with proper scroll */}
          <ScrollArea className="flex-1 px-3 py-2" ref={scrollAreaRef}>
            <div className="space-y-3">
              {messages.map(msg => (
                <div key={msg.id} className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[80%] ${msg.type === 'user' ? 'order-2' : ''}`}>
                    <div className={`rounded-lg px-3 py-2 text-sm ${
                      msg.type === 'ai' 
                        ? 'bg-glass/20 border border-glass-border' 
                        : 'bg-primary/20 border border-primary/30'
                    }`}>
                      {msg.content}
                    </div>
                    {msg.sources && msg.sources.length > 0 && (
                      <div className="text-[10px] text-muted-foreground mt-1 px-1">
                        Sources:{' '}
                        {msg.sources.map((s, i) => (
                          <span key={i}>
                            Page {s.page} ({Math.round(s.confidence * 100)}%)
                            {i < msg.sources.length - 1 && ', '}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              <div ref={endRef} />
            </div>
            <ScrollBar orientation="vertical" />
          </ScrollArea>

          {/* Snippets preview if any */}
          {snips.length > 0 && (
            <div className="px-3 py-2 border-t border-glass-border">
              <div className="text-[10px] text-muted-foreground mb-1">
                Selected text ({snips.length}):
              </div>
              <div className="max-h-20 overflow-y-auto">
                {snips.map((snip, i) => (
                  <div key={snip.id} className="text-[11px] text-muted-foreground truncate">
                    {i + 1}. {snip.text.substring(0, 50)}...
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Input area */}
          <div className="border-t border-glass-border p-3">
            <div className="flex gap-2 items-center">
              <Input
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    send();
                  }
                }}
                placeholder={global ? "Ask about all your documents…" : "Ask about this document…"}
                className="text-xs h-8 bg-[#1a1a1a] text-white border border-glass-border placeholder:text-gray-400"
                disabled={typing}
              />
              <Button 
                size="icon" 
                disabled={typing || (!input.trim() && !snips.length)} 
                onClick={send}
                className="h-8 w-8"
              >
                <Send className="w-3 h-3" />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}