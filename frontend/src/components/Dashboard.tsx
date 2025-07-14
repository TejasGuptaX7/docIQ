// src/pages/Dashboard.tsx
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Search,
  Sparkles,
  Settings,
  User,
  FileText,
  Globe,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import WorkspaceSidebar from '@/components/WorkspaceSidebar';
import UploadButton from '@/components/UploadButton';
import PdfEditorView from '@/components/PdfEditorView';
import AssistantPanel from '@/components/AssistantPanel';
import SearchModel from '@/components/SearchModel';
import useDocTags from '@/hooks/useDocTags';

// ——— Types & Fetch —————————————————————————————————————————————————————

interface Doc {
  _additional: { id: string };
  title: string | null;
  pages: number | null;
}

const fetchDocs = async (): Promise<Doc[]> => {
  const res = await fetch('/api/documents');
  return res.ok ? res.json() : [];
};

// ——— Dashboard Component ——————————————————————————————————————————————————

export default function Dashboard() {
  const navigate = useNavigate();

  const [workspace, setWorkspace] = useState('default');
  const [selectedDoc, setSelectedDoc] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);

  const { data: docs = [], refetch } = useQuery({
    queryKey: ['documents'],
    queryFn: fetchDocs,
    staleTime: 0,
  });

  const { tags, tagDoc } = useDocTags();

  // always include our built-ins, then any user-created tags
  const BASE = ['default', 'Research', 'Academic', 'Fun', 'Literature', 'Travel'];
  const workspaces = Array.from(new Set([...BASE, ...Object.values(tags)]));

  // ⌘/Ctrl + K → open search
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setShowSearch(true);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  // map workspace name → icon
  const wsIcon = (w: string) => {
    switch (w.toLowerCase()) {
      case 'research':   return <Search className="w-4 h-4" />;
      case 'academic':   return <Sparkles className="w-4 h-4" />;
      case 'fun':        return <User className="w-4 h-4" />;
      case 'literature': return <FileText className="w-4 h-4" />;
      case 'travel':     return <Globe className="w-4 h-4" />;
      default:           return <FileText className="w-4 h-4" />;
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* — Sidebar with workspaces & docs */}
      <WorkspaceSidebar
        docs={docs}
        tags={tags}
        selectedDoc={selectedDoc}
        selectedWorkspace={workspace}
        onSelectDoc={setSelectedDoc}
        onMoveDoc={(id, ws) => {
          // if real doc-move, tag it; always switch view
          if (id !== '__switch__') tagDoc(id, ws);
          setWorkspace(ws);
          setSelectedDoc(null);
        }}
        workspaces={workspaces}
        wsIcon={wsIcon}
        onNew={() =>
          // trigger the sheet open button inside the sidebar
          (document.getElementById('ws-sheet-btn') as HTMLButtonElement)
            ?.click()
        }
      />

      {/* — Main PDF & controls */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <div className="h-12 flex items-center justify-between px-6 border-b border-border/50">
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
              <ArrowLeft className="w-4 h-4 mr-1" /> Back
            </Button>
            <Badge className="bg-primary/10 text-primary border-primary/20 flex items-center">
              <Sparkles className="w-3 h-3 mr-1" /> AI Assistant Active
            </Badge>
          </div>

          <div className="flex items-center gap-4">
            {/* Search Docs */}
            <Button
              variant="ghost"
              className="flex items-center gap-2 px-3 py-2"
              onClick={() => setShowSearch(true)}
            >
              <Search className="w-4 h-4" />
              <span>Search Docs</span>
              <kbd className="ml-1 px-1 text-[10px] bg-border/50 rounded">⌘K</kbd>
            </Button>

            {/* Uploads nav */}
            <Button size="icon" variant="ghost" onClick={() => navigate('/uploads')}>
              <FileText className="w-4 h-4" />
            </Button>

            {/* Inline uploader */}
            <UploadButton
              currentWorkspace={workspace}
              workspaces={workspaces}
              onUploaded={refetch}
            />

            <Button size="sm" variant="ghost">
              <Settings className="w-4 h-4" />
            </Button>
            <Button size="sm" variant="ghost">
              <User className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {/* PDF canvas or placeholder */}
        <div className="flex-1 overflow-y-auto p-6">
          {selectedDoc ? (
            <PdfEditorView docId={selectedDoc} />
          ) : (
            <p className="text-muted-foreground text-center mt-20">
              Choose or upload a document…
            </p>
          )}
        </div>
      </main>

      {/* — AI assistant panel */}
      <AssistantPanel selectedDoc={selectedDoc} />

      {/* — Search modal */}
      <SearchModel isOpen={showSearch} onClose={() => setShowSearch(false)} />
    </div>
  );
}
