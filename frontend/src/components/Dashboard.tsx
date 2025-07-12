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
import EditorView from "@/components/EditorView";
import SearchModel from "@/components/SearchModel";

const Dashboard = () => {
  const navigate = useNavigate();

  const [selectedDoc, setSelectedDoc] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);

  /* ⌘K / Ctrl+K to open search */
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setShowSearch(true);
      }
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, []);

  return (
    <div className="relative min-h-screen w-full overflow-hidden bg-transparent">
      <div className="relative z-10 flex">
        {/* ─────── SIDEBAR ──────────────────────────────────── */}
        <div className="w-80 border-r border-border/50 bg-card/50 backdrop-blur-sm">
          <div className="p-6 border-b border-border/50">
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
          </div>

          {/* Document Library + Upload */}
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-lg">Document Library</h3>
              <UploadButton onUploaded={() => (window as any).refetchDocs?.()} />
            </div>

            {/* quick filters */}
            <div className="flex items-center space-x-2 mb-4">
              <Button size="sm" variant="ghost" className="text-xs">
                <Filter className="w-3 h-3 mr-1" />
                All
              </Button>
              <Button size="sm" variant="ghost" className="text-xs">
                PDFs
              </Button>
              <Button size="sm" variant="ghost" className="text-xs">
                Recent
              </Button>
            </div>

            {/* list */}
            <DocumentLibrary
              onSelectDoc={setSelectedDoc}
              selectedDoc={selectedDoc}
            />
          </div>
        </div>

        {/* ─────── MAIN COLUMN ─────────────────────────────── */}
        <div className="flex-1 flex flex-col">
          {/* Top bar */}
          <div className="h-16 border-b border-border/50 bg-card/50 backdrop-blur-sm flex items-center justify-between px-6">
            <Badge className="bg-primary/10 text-primary border-primary/20 flex items-center">
              <Sparkles className="w-3 h-3 mr-1" />
              AI Assistant Active
            </Badge>

            <div className="flex items-center space-x-4">
              {/* Search Docs button */}
              <Button
                variant="ghost"
                className="group flex items-center space-x-2 px-3 py-2 border border-border/30 rounded-full bg-card/60 hover:bg-primary/10 transition-all"
                onClick={() => setShowSearch(true)}
              >
                <Search className="w-4 h-4 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">Search Docs</span>
                <kbd className="ml-2 px-2 py-0.5 text-[10px] font-mono text-muted-foreground bg-border/50 rounded border border-border">
                  ⌘ K
                </kbd>
              </Button>

              <Button size="sm" variant="ghost">
                <Settings className="w-4 h-4" />
              </Button>
              <Button size="sm" variant="ghost">
                <User className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Editor view */}
          <div className="flex-1 px-6 py-4 overflow-y-auto">
            <EditorView />
          </div>
        </div>
      </div>

      {/* search modal */}
      <SearchModel isOpen={showSearch} onClose={() => setShowSearch(false)} />
    </div>
  );
};

export default Dashboard;
