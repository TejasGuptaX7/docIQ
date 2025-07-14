import { useState, useRef, useEffect } from 'react';
import {
  Button, Input, Card, Badge,
} from '@/components/ui';
import {
  Send, User, Brain, FileText, ExternalLink,
} from 'lucide-react';
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area';

interface Props { selectedDoc: string | null; }

interface Msg {
  id: string;
  type: 'user' | 'ai';
  content: string;
  timestamp: Date;
  sources?: { page:number; excerpt:string; confidence:number }[];
}

export default function ChatInterface({ selectedDoc }: Props) {
  const [messages, setMessages] = useState<Msg[]>([{
    id: 'sys', type: 'ai',
    content: "Hello! I'm your AI assistant. Ask me about your documents.",
    timestamp: new Date(),
  }]);
  const [input, setInput]   = useState('');
  const [typing, setTyping] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  /* auto-scroll */
  useEffect(() => endRef.current?.scrollIntoView({ behavior:'smooth' }), [messages]);

  /* handle “Add to chat” from viewer */
  useEffect(() => {
    const h = (e: any) => {
      setMessages(m => [...m, {
        id: crypto.randomUUID(),
        type: 'user',
        content: e.detail.text,
        timestamp: new Date(),
      }]);
    };
    window.addEventListener('add-selection-to-chat', h);
    return () => window.removeEventListener('add-selection-to-chat', h);
  }, []);

  const send = async (text: string) => {
    if (!text.trim() || !selectedDoc) return;
    setMessages(m => [...m, {
      id: crypto.randomUUID(), type:'user', content:text, timestamp:new Date(),
    }]);
    setInput(''); setTyping(true);

    try {
      const res = await fetch('/api/search', {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        body: JSON.stringify({ query:text, docId:selectedDoc }),
      });
      const data = await res.json();
      setMessages(m => [...m, {
        id: crypto.randomUUID(),
        type:'ai',
        content: data.error ?? data.answer ?? 'No response.',
        timestamp: new Date(),
        sources: data.sources,
      }]);
    } catch (e) {
      setMessages(m => [...m, {
        id: crypto.randomUUID(),
        type:'ai',
        content:'⚠️ backend error',
        timestamp:new Date(),
      }]);
    }
    setTyping(false);
  };

  return (
    <div className="h-full flex flex-col">
      {!selectedDoc ? (
        /* welcome */
        <div className="flex-1 flex items-center justify-center">
          <Card className="p-8 max-w-md text-center">
            <Brain className="w-12 h-12 mx-auto mb-4" />
            <p>Select or upload a document to begin.</p>
          </Card>
        </div>
      ) : (
        <>
          <ScrollArea className="flex-1 mb-4">
            {/* message list */}
            <div className="space-y-6 pb-4">
              {messages.map(m => (
                <div key={m.id} className={`flex ${m.type==='user'?'justify-end':''}`}>
                  <div className={`flex items-start space-x-3 max-w-3xl ${m.type==='user'?'flex-row-reverse space-x-reverse':''}`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                      m.type==='user'?'bg-primary text-white':'bg-gradient-to-r from-purple-500 to-pink-500 text-white'
                    }`}>
                      {m.type==='user' ? <User className="w-4 h-4"/>:<Brain className="w-4 h-4"/>}
                    </div>
                    <div className="flex-1">
                      <Card className={m.type==='user'?'bg-primary text-white p-4':'p-4'}>
                        <p className="whitespace-pre-wrap">{m.content}</p>
                        {m.sources && (
                          <div className="mt-3 space-y-2">
                            {m.sources.map((s,i)=>(
                              <Card key={i} className="p-3 bg-muted/50">
                                <Badge variant="outline" className="text-xs mr-2">
                                  Page {s.page}
                                </Badge>
                                <span className="text-xs">{s.excerpt}</span>
                              </Card>
                            ))}
                          </div>
                        )}
                      </Card>
                      <p className="text-xs text-muted-foreground mt-1">{m.timestamp.toLocaleTimeString()}</p>
                    </div>
                  </div>
                </div>
              ))}
              {typing && <p className="px-4 text-sm text-muted-foreground">AI is typing…</p>}
            </div>
            <div ref={endRef}/>
          </ScrollArea>

          {/* input */}
          <div className="border-t pt-3">
            <div className="flex gap-3">
              <Input
                value={input}
                onChange={e=>setInput(e.target.value)}
                onKeyDown={e=>{ if(e.key==='Enter' && !e.shiftKey){ e.preventDefault(); send(input);} }}
                placeholder="Ask anything…"
              />
              <Button size="icon" disabled={!input.trim()||typing} onClick={()=>send(input)}>
                <Send className="w-4 h-4"/>
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
