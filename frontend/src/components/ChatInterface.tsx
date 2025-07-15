// src/components/ChatInterface.tsx
import { useState, useRef, useEffect } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { Button, Input, Card, Badge } from '@/components/ui';
import { Send, User, Brain, FileText, X } from 'lucide-react';
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
  id:       string;
  docId:    string;
  filename: string;
  text:     string;
  page:     number;
  start:    number;
  end:      number;
}

/* helper: shorten long filenames */
const shorten = (s?: string) =>
  s && s.length > 18 ? `${s.slice(0, 14)}…${s.slice(-3)}` : (s ?? 'untitled');

export default function ChatInterface({ selectedDoc }: Props) {
  const { getToken } = useAuth();
  const [messages, setMessages] = useState<Msg[]>([{
    id: 'sys', type: 'ai',
    content: "Hello! I'm your AI assistant. Ask me about your documents.",
    timestamp: new Date(),
  }]);
  const [snips, setSnips]   = useState<Snip[]>([]);
  const [input, setInput]   = useState('');
  const [typing, setTyping] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => endRef.current?.scrollIntoView({ behavior: 'smooth' }), [messages]);

  useEffect(() => {
    const onAdd = (e: any) => {
      const d = e.detail;
      const s: Snip = {
        id: crypto.randomUUID(),
        docId: d.docId,
        filename: d.filename || 'untitled.pdf',
        text: d.text,
        page: d.page,
        start: d.start,
        end: d.end
      };
      setSnips(p => [...p, s]);
    };
    window.addEventListener('add-selection-to-chat', onAdd);
    return () => window.removeEventListener('add-selection-to-chat', onAdd);
  }, []);

  const ctxString = () =>
    snips.map(s => `[${s.filename} p.${s.page} ${s.start}-${s.end}] ${s.text}`)
         .join('\n\n');

  const send = async () => {
    const q = (ctxString() + '\n\n' + input).trim();
    if (!q || !selectedDoc) return;

    setMessages(m => [...m, {
      id: crypto.randomUUID(),
      type: 'user',
      content: input,
      timestamp: new Date()
    }]);
    setInput(''); setSnips([]); setTyping(true);

    try {
      const token = await getToken();
      const r = await fetch('/api/search', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ query: q, docId: selectedDoc }),
      });
      const data = await r.json();
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
            m.map(x =>
              x.id === aiId
                ? { ...x, content: answer.slice(0, i + 1) }
                : x
            )
          )
        , i * 20)
      );
      setTimeout(() => setTyping(false), answer.length * 20 + 100);

    } catch {
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
      {!selectedDoc ? (
        <div className="flex-1 flex items-center justify-center">
          <Card className="p-8 max-w-md text-center">
            <Brain className="w-12 h-12 mx-auto mb-4" />
            <p>Select or upload a document to begin.</p>
          </Card>
        </div>
      ) : (
        <>
          <ScrollArea className="flex-1 mb-4">
            <div className="space-y-6 pb-4">
              {messages.map(m => (
                <div key={m.id} className={`flex ${m.type==='user'?'justify-end':''}`}>
                  <div className={`flex items-start space-x-3 max-w-3xl ${m.type==='user'?'flex-row-reverse space-x-reverse':''}`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center ${m.type==='user'?'bg-primary text-white':'bg-gradient-to-r from-purple-500 to-pink-500 text-white'}`}>
                      {m.type==='user' ? <User className="w-4 h-4"/> : <Brain className="w-4 h-4"/>}
                    </div>
                    <div className="flex-1">
                      <Card className={m.type==='user'?'bg-primary text-white p-4':'p-4'}>
                        <p className="whitespace-pre-wrap">{m.content}</p>
                        {m.sources?.length && (
                          <div className="mt-3 space-y-2">
                            {m.sources.map((s, i) => (
                              <Card key={i} className="p-3 bg-muted/50">
                                <Badge variant="outline" className="text-xs mr-2">
                                  <FileText className="w-3 h-3 mr-1"/>Page {s.page}
                                </Badge>
                                <span className="text-xs">{s.excerpt}</span>
                              </Card>
                            ))}
                          </div>
                        )}
                      </Card>
                      <p className="text-xs text-muted-foreground mt-1">
                        {m.timestamp.toLocaleTimeString()}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
              {typing && <p className="px-4 text-sm text-muted-foreground">AI is typing…</p>}
            </div>
            <div ref={endRef} />
            <ScrollBar orientation="vertical" />
          </ScrollArea>

          <div className="border-t pt-3">
            {!!snips.length && (
              <div className="flex flex-wrap gap-2 px-4 pb-2">
                {snips.map(s => (
                  <Badge key={s.id} variant="secondary" className="py-1 pr-1 pl-2 flex items-center gap-1">
                    {shorten(s.filename)} (p{s.page} • {s.start}-{s.end})
                    <button onClick={() => setSnips(x => x.filter(y => y.id!==s.id))}>
                      <X className="w-3 h-3" />
                    </button>
                  </Badge>
                ))}
              </div>
            )}
            <div className="flex gap-3 px-4 pb-4">
              <Input
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => { if (e.key==='Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
                placeholder="Ask anything…"
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
