import { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Send, User, Brain, FileText, ExternalLink } from "lucide-react";

interface ChatInterfaceProps {
  selectedDoc: string | null;
}

interface Message {
  id: string;
  type: 'user' | 'ai';
  content: string;
  timestamp: Date;
  sources?: Array<{
    page: number;
    excerpt: string;
    confidence: number;
  }>;
}

const ChatInterface = ({ selectedDoc }: ChatInterfaceProps) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      type: 'ai',
      content: "Hello! I'm your AI assistant. I can help you analyze and understand your documents. What would you like to know?",
      timestamp: new Date(),
    }
  ]);
  const [inputValue, setInputValue] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSendMessage = async () => {
    if (!inputValue.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue("");
    setIsTyping(true);

    try {
      const response = await fetch(`/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: inputValue, docId: selectedDoc }),
      });
      if (!response.ok) throw new Error('Backend error');
      const data = await response.json();
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        type: 'ai',
        content: data.answer || 'No answer from backend.',
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      setMessages(prev => [...prev, {
        id: (Date.now() + 1).toString(),
        type: 'ai',
        content: 'Error contacting backend.',
        timestamp: new Date(),
      }]);
    }
    setIsTyping(false);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Welcome Section */}
      {!selectedDoc && (
        <div className="flex-1 flex items-center justify-center">
          <Card className="p-8 max-w-md glass-morphism text-center">
            <Brain className="w-12 h-12 text-primary mx-auto mb-4 animate-glow" />
            <h3 className="text-xl font-semibold mb-2 font-space">Ready to Analyze</h3>
            <p className="text-muted-foreground mb-4">
              Select a document from the sidebar to start asking questions and get AI-powered insights.
            </p>
            <Badge className="bg-primary/10 text-primary border-primary/20">
              Vector Search Enabled
            </Badge>
          </Card>
        </div>
      )}

      {/* Chat Messages */}
      {selectedDoc && (
        <>
          <ScrollArea className="flex-1 mb-4">
            <div className="space-y-6 pb-4">
              {messages.map((message) => (
                <div key={message.id} className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`flex items-start space-x-3 max-w-3xl ${message.type === 'user' ? 'flex-row-reverse space-x-reverse' : ''}`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                      message.type === 'user' 
                        ? 'bg-primary text-primary-foreground' 
                        : 'bg-gradient-to-r from-purple-500 to-pink-500 text-white'
                    }`}>
                      {message.type === 'user' ? <User className="w-4 h-4" /> : <Brain className="w-4 h-4" />}
                    </div>
                    
                    <div className={`flex-1 ${message.type === 'user' ? 'text-right' : ''}`}>
                      <Card className={`p-4 ${
                        message.type === 'user' 
                          ? 'bg-primary text-primary-foreground' 
                          : 'glass-morphism'
                      }`}>
                        <p className="whitespace-pre-wrap">{message.content}</p>
                        
                        {message.sources && (
                          <div className="mt-4 space-y-2">
                            <p className="text-sm font-medium text-muted-foreground">Sources:</p>
                            {message.sources.map((source, index) => (
                              <Card key={index} className="p-3 bg-muted/50">
                                <div className="flex items-center justify-between mb-2">
                                  <Badge variant="outline" className="text-xs">
                                    <FileText className="w-3 h-3 mr-1" />
                                    Page {source.page}
                                  </Badge>
                                  <Badge className="text-xs bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400">
                                    {Math.round(source.confidence * 100)}% match
                                  </Badge>
                                </div>
                                <p className="text-sm text-muted-foreground">{source.excerpt}</p>
                                <Button size="sm" variant="ghost" className="mt-2 text-xs h-6">
                                  <ExternalLink className="w-3 h-3 mr-1" />
                                  View in document
                                </Button>
                              </Card>
                            ))}
                          </div>
                        )}
                      </Card>
                      
                      <p className="text-xs text-muted-foreground mt-1">
                        {message.timestamp.toLocaleTimeString()}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
              
              {isTyping && (
                <div className="flex justify-start">
                  <div className="flex items-start space-x-3 max-w-3xl">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 flex items-center justify-center text-white">
                      <Brain className="w-4 h-4" />
                    </div>
                    <Card className="p-4 glass-morphism">
                      <div className="flex space-x-1">
                        <div className="w-2 h-2 bg-primary rounded-full animate-bounce"></div>
                        <div className="w-2 h-2 bg-primary rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                        <div className="w-2 h-2 bg-primary rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                      </div>
                    </Card>
                  </div>
                </div>
              )}
            </div>
            <div ref={messagesEndRef} />
          </ScrollArea>

          {/* Input Area */}
          <div className="border-t border-border/50 pt-4">
            <div className="flex space-x-4">
              <div className="flex-1 relative">
                <Input
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Ask anything about your document..."
                  className="pr-12 glass-morphism border-primary/20 focus:border-primary/50 py-3"
                />
                <Button
                  onClick={handleSendMessage}
                  size="icon"
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-primary hover:bg-primary/90 animate-glow"
                  disabled={!inputValue.trim() || isTyping}
                >
                  <Send className="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ChatInterface;
