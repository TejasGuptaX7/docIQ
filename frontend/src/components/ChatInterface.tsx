import { useState, useRef, useEffect } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { Button, Input, Card, Badge, Switch, Label } from '@/components/ui';
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

  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
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
      const res = await fetch('/api/search', {
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
        content: '⚠️ backend error',
        timestamp: new Date()
      }]);
      setTyping(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Always show the toggle at the top */}
      <div className="px-4 py-2 border-b flex justify-end items-center gap-2">
        <Globe className="w-4 h-4 opacity-60" />
        <Switch id="scope" checked={global} onCheckedChange={setGlobal} />
        <Label htmlFor="scope" className="cursor-pointer select-none">
          {global ? 'All docs' : 'Current doc'}
        </Label>
      </div>

      {!selectedDoc && !global ? (
        <div className="flex-1 flex items-center justify-center">
          <Card className="p-8 max-w-md text-center">
            <Brain className="w-12 h-12 mx-auto mb-4" />
            <p>Select a document or switch to "All docs" mode above to begin.</p>
          </Card>
        </div>
      ) : (
        <>
          <ScrollArea className="flex-1 mb-4">
            {messages.map(msg => (
              <div key={msg.id} className={`px-4 py-2 ${msg.type === 'user' ? 'text-right' : ''}`}>
                <span className={`inline-block ${msg.type === 'ai' ? 'bg-gray-800' : 'bg-gray-600'} rounded px-3 py-2`}>
                  {msg.content}
                </span>
                {msg.sources && msg.sources.length > 0 && (
                  <div className="text-xs text-gray-400 mt-1">
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
            ))}
            <div ref={endRef} />
            <ScrollBar orientation="vertical" />
          </ScrollArea>
          <div className="border-t pt-3">
            <div className="flex gap-3 px-4 pb-4 items-center">
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
              />
              <Button size="icon" disabled={typing || (!input.trim() && !snips.length)} onClick={send}>
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}