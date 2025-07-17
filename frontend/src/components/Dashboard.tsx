// src/components/Dashboard.tsx
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@clerk/clerk-react';
import {
  ArrowLeft, Search, Sparkles, Settings,
  User, FileText, Globe, Bug,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import WorkspaceSidebar from '@/components/WorkspaceSidebar';
import UploadButton from '@/components/UploadButton';
import PdfEditorView from '@/components/PdfEditorView';
import AssistantPanel from '@/components/AssistantPanel';
import SearchModel from '@/components/SearchModel';
import useDocTags from '@/hooks/useDocTags';
import ConnectDriveButton from '@/components/ConnectDriveButton';
import { UserButton } from '@clerk/clerk-react';

interface Doc {
  _additional: { id: string };
  title: string | null;
  pages: number | null;
  source?: string;
  workspace?: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { getToken, isLoaded } = useAuth();
  const [workspace, setWorkspace] = useState('default');
  const [selectedDoc, setSelectedDoc] = useState<string | null>(null);
  const [showSearch, setShowSearch] = useState(false);

  const fetchDocs = async (): Promise<Doc[]> => {
    const token = await getToken();
    const res   = await fetch('/api/documents', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.ok ? res.json() : [];
  };

  const { data: docs = [], refetch } = useQuery({
    queryKey: ['documents'],
    queryFn: fetchDocs,
    staleTime: 0,
  });

  const { tags, tagDoc } = useDocTags();
  const BASE = ['default','Research','Academic','Fun','Literature','Travel'];
  const workspaces = Array.from(new Set([...BASE, ...Object.values(tags)]));

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault(); setShowSearch(true);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

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

  // Debug function
  const debugWeaviate = async () => {
    try {
      const token = await getToken();
      console.log('Fetching Weaviate debug data...');
      
      const res = await fetch('/api/debug/weaviate-count', {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      if (!res.ok) {
        console.error('Debug request failed:', res.status, res.statusText);
        const errorText = await res.text();
        console.error('Error response:', errorText);
        return;
      }
      
      const data = await res.json();
      console.log('=== WEAVIATE DEBUG DATA ===');
      console.log(JSON.stringify(data, null, 2));
      console.log('=========================');
      
      // Parse and display counts
      if (data?.data?.Aggregate) {
        const docCount = data.data.Aggregate.Document?.meta?.count || 0;
        const chunkCount = data.data.Aggregate.Chunk?.meta?.count || 0;
        const sources = data.data.Aggregate.Document?.source?.topOccurrences || [];
        
        console.log(`Total Documents: ${docCount}`);
        console.log(`Total Chunks: ${chunkCount}`);
        console.log('Document Sources:');
        sources.forEach((s: any) => {
          console.log(`  - ${s.value}: ${s.occurs} documents`);
        });
      }
    } catch (error) {
      console.error('Debug error:', error);
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-background relative">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-md z-0" />
      <div className="relative z-10 flex h-full w-full">
        <WorkspaceSidebar
          docs={docs}
          tags={tags}
          selectedDoc={selectedDoc}
          selectedWorkspace={workspace}
          onSelectDoc={setSelectedDoc}
          onMoveDoc={(id, ws) => {
            if (id !== '__switch__') tagDoc(id, ws);
            setWorkspace(ws); setSelectedDoc(null);
          }}
          workspaces={workspaces}
          wsIcon={wsIcon}
          onNew={() => (document.getElementById('ws-sheet-btn') as HTMLButtonElement)?.click()}
        />
        <main className="flex-1 flex flex-col overflow-hidden">
          <div className="h-12 flex items-center justify-between px-6 border-b border-border/50 bg-black/30 backdrop-blur-sm">
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
                <ArrowLeft className="w-4 h-4 mr-1" /> Back
              </Button>
              <Badge className="bg-primary/10 text-primary border-primary/50 flex items-center">
                <Sparkles className="w-0 h-0 mr-1" /> AI&nbsp;Active
              </Badge>
            </div>
            <div className="flex items-center gap-4">
              <Button variant="ghost" className="flex items-center gap-2 px-3 py-2"
                onClick={() => setShowSearch(true)}>
                <Search className="w-4 h-4" /> Search&nbsp;Docs
                <kbd className="ml-1 px-1 text-[10px] bg-border/50 rounded">⌘K</kbd>
              </Button>
              <Button size="icon" variant="ghost" onClick={() => navigate('/uploads')}>
                <FileText className="w-4 h-4" />
              </Button>
              
              {/* Debug Button - TEMPORARY */}
              <Button 
                variant="ghost" 
                size="sm"
                onClick={debugWeaviate}
                className="bg-orange-500/10 hover:bg-orange-500/20 text-orange-500"
              >
                <Bug className="w-4 h-4 mr-1" />
                Debug
              </Button>
              
              <ConnectDriveButton onConnected={refetch} />
              <UploadButton
                currentWorkspace={workspace}
                workspaces={workspaces}
                onUploaded={refetch}
              />
              <Button size="sm" variant="ghost"><Settings className="w-4 h-4" /></Button>
              <UserButton afterSignOutUrl="/" />
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-6">
            {selectedDoc ? (
              <PdfEditorView docId={selectedDoc} />
            ) : (
              <div className="text-center mt-20">
                <p className="text-muted-foreground">
                  Choose or upload a document to begin.
                </p>
                {docs.length > 0 && (
                  <div className="mt-4 text-sm text-muted-foreground">
                    <p>You have {docs.length} document(s) available:</p>
                    <div className="mt-2 flex flex-wrap justify-center gap-2">
                      {docs.slice(0, 5).map(doc => (
                        <Badge key={doc._additional.id} variant="outline" className="text-xs">
                          {doc.title}{doc.source === 'drive' && ' (Drive)'}
                        </Badge>
                      ))}
                      {docs.length > 5 && <Badge variant="outline" className="text-xs">+{docs.length - 5} more</Badge>}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </main>
        <AssistantPanel selectedDoc={selectedDoc} />
        <SearchModel isOpen={showSearch} onClose={() => setShowSearch(false)} />
      </div>
    </div>
  );
}