import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import UploadButton from "@/components/UploadButton";
import {
  ArrowLeft,
  Search,
  Filter,
  Brain,
  Sparkles,
  Settings,
  User,
} from "lucide-react";

import DocumentLibrary from "@/components/DocumentLibrary";
import PdfEditorView from "@/components/PdfEditorView";
import AssistantPanel from "@/components/AssistantPanel";
import SearchModel from "@/components/SearchModel";

const Dashboard = () => {
  const navigate = useNavigate();
  const [selectedDoc, setSelectedDoc] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);

  // Cmd/Ctrl + K to open search modal
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setShowSearch(true);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* ─── Sidebar ───────────────────────────────────────────── */}
      <aside className="w-80 flex-shrink-0 border-r border-border/50 bg-card/50 backdrop-blur-sm">
        <header className="p-6 border-b border-border/50">
          <div className="flex items-center justify-between mb-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate(-1)}
              className="hover:bg-primary/10"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back
            </Button>
            <div className="flex items-center space-x-2">
              <Brain className="w-6 h-6 text-primary" />
              <span className="font-bold font-space text-gradient">DocIQ</span>
            </div>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="Search documents…"
              className="pl-10 glass-morphism border-primary/20 focus:border-primary/50"
            />
          </div>
        </header>

        <section className="p-6 overflow-y-auto h-[calc(100%-149px)]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-lg">Document Library</h3>
            <UploadButton onUploaded={() => (window as any).refetchDocs?.()} />
          </div>

          <div className="flex items-center space-x-2 mb-4">
            <Button size="sm" variant="ghost" className="text-xs">
              <Filter className="w-3 h-3 mr-1" /> All
            </Button>
            <Button size="sm" variant="ghost" className="text-xs">PDFs</Button>
            <Button size="sm" variant="ghost" className="text-xs">Recent</Button>
          </div>

          <DocumentLibrary selectedDoc={selectedDoc} onSelectDoc={setSelectedDoc} />
        </section>
      </aside>

      {/* ─── Main Canvas (PDF) ─────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Top Bar */}
        <div className="h-12 flex items-center justify-between mb-6">
          <Badge className="bg-primary/10 text-primary border-primary/20 flex items-center">
            <Sparkles className="w-3 h-3 mr-1" />
            AI Assistant Active
          </Badge>
          <div className="flex items-center space-x-4">
            <Button
              variant="ghost"
              className="group flex items-center space-x-2 px-3 py-2 border border-border/30 rounded-full bg-card/60 hover:bg-primary/10"
              onClick={() => setShowSearch(true)}
            >
              <Search className="w-4 h-4 text-muted-foreground" />
              <span className="text-sm text-muted-foreground">Search Docs</span>
              <kbd className="ml-2 px-2 py-0.5 text-[10px] font-mono text-muted-foreground bg-border/50 rounded border border-border">
                ⌘ K
              </kbd>
            </Button>
            <Button size="sm" variant="ghost"><Settings className="w-4 h-4" /></Button>
            <Button size="sm" variant="ghost"><User className="w-4 h-4" /></Button>
          </div>
        </div>

        {/* PDF Viewer */}
        {selectedDoc ? (
          <PdfEditorView docId={selectedDoc} />
        ) : (
          <p className="text-muted-foreground text-center mt-20">
            Upload or choose a document to start editing…
          </p>
        )}
      </main>

      {/* ─── AI Assistant ─────────────────────────────────────── */}
      <AssistantPanel selectedDoc={selectedDoc} />

      {/* Search Modal */}
      <SearchModel isOpen={showSearch} onClose={() => setShowSearch(false)} />
    </div>
  );
};

export default Dashboard;
