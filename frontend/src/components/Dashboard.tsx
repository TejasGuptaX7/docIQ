// src/components/Dashboard.tsx
import { useEffect, useState, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@clerk/clerk-react';
import {
  ArrowLeft, Search, Sparkles, Settings,
  User, FileText, Globe, Bug, Plus, MoreVertical,
  Trash2, Move
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { GlassCard } from '@/components/GlassCard';
import UploadButton from '@/components/UploadButton';
import PdfEditorView from '@/components/PdfEditorView';
import AssistantPanel from '@/components/AssistantPanel';
import SearchModel from '@/components/SearchModel';
import useDocTags from '@/hooks/useDocTags';
import ConnectDriveButton from '@/components/ConnectDriveButton';
import { UserButton } from '@clerk/clerk-react';
import WorkspaceSheet from '@/components/WorkspaceSheet';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuTrigger,
  DropdownMenuItem, DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { toast } from '@/components/ui/use-toast';

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

  // ** NEW: Load custom workspaces from localStorage **
  const [customWorkspaces, setCustomWorkspaces] = useState<string[]>(() => {
    return JSON.parse(localStorage.getItem('vectormind.workspaces') || '[]');
  });
  const saveWorkspaces = (list: string[]) => {
    setCustomWorkspaces(list);
    localStorage.setItem('vectormind.workspaces', JSON.stringify(list));
  };

  // Resizable panel states
  const [leftPanelWidth, setLeftPanelWidth] = useState(320);
  const [rightPanelWidth, setRightPanelWidth] = useState(380);
  const [isResizingLeft, setIsResizingLeft] = useState(false);
  const [isResizingRight, setIsResizingRight] = useState(false);

  const fetchDocs = async (): Promise<Doc[]> => {
    const token = await getToken();
    const res = await fetch('/api/documents', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.ok ? res.json() : [];
  };

  const { data: docs = [], refetch } = useQuery({
    queryKey: ['documents'],
    queryFn: fetchDocs,
    staleTime: 0,
    refetchInterval: 5000,
  });

  const { tags, tagDoc } = useDocTags();
  const BASE = ['default','Research','Academic','Fun','Literature','Travel'];

  // ** UPDATED to include custom workspaces and tag-derived ones **
  const workspaces = Array.from(new Set([
    ...BASE,
    ...customWorkspaces,
    ...Object.values(tags),
  ]));

  // Handle keyboard shortcut
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

  // Handle resizing
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isResizingLeft) {
        const newWidth = Math.max(280, Math.min(500, e.clientX));
        setLeftPanelWidth(newWidth);
      }
      if (isResizingRight) {
        const newWidth = Math.max(300, Math.min(600, window.innerWidth - e.clientX));
        setRightPanelWidth(newWidth);
      }
    };

    const handleMouseUp = () => {
      setIsResizingLeft(false);
      setIsResizingRight(false);
    };

    if (isResizingLeft || isResizingRight) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isResizingLeft, isResizingRight]);

  const wsIcon = (w: string) => {
    switch (w.toLowerCase()) {
      case 'research':   return <Search className="w-3 h-3" />;
      case 'academic':   return <Sparkles className="w-3 h-3" />;
      case 'fun':        return <User className="w-3 h-3" />;
      case 'literature': return <FileText className="w-3 h-3" />;
      case 'travel':     return <Globe className="w-3 h-3" />;
      default:           return <FileText className="w-3 h-3" />;
    }
  };

  // Debug function - KEEP THIS
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

  // Delete document
  const deleteDocument = async (docId: string) => {
    try {
      const token = await getToken();
      const res = await fetch(`/api/documents/${docId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      });
      
      if (res.ok) {
        toast({
          title: 'Document deleted',
          description: 'The document has been removed.',
        });
        if (selectedDoc === docId) {
          setSelectedDoc(null);
        }
        refetch();
      } else {
        throw new Error('Failed to delete document');
      }
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to delete document',
        variant: 'destructive',
      });
    }
  };

  // Get documents for current workspace
  const docsInWorkspace = docs.filter(
    doc => (tags[doc._additional.id] || 'default') === workspace
  );

  // Handle document move with persistence
  const handleMoveDoc = async (docId: string, ws: string) => {
    if (docId === '__switch__') {
      setWorkspace(ws);
      setSelectedDoc(null);
    } else {
      await tagDoc(docId, ws);
      setTimeout(() => refetch(), 100);
    }
  };

  // ** UPDATED: Handle creating new workspace persistently **
  const handleCreateWorkspace = (name: string) => {
    if (!customWorkspaces.includes(name)) {
      const updated = [...customWorkspaces, name];
      saveWorkspaces(updated);
    }
    setWorkspace(name);
  };

  return (
    <div className="flex h-screen overflow-hidden bg-gradient-main">
      <div className="flex h-full w-full">
        {/* Document Library Sidebar with Glass Design - Resizable */}
        <div 
          className="flex-shrink-0 relative"
          style={{ width: `${leftPanelWidth}px` }}
        >
          <GlassCard variant="feature" className="h-full m-2 p-3 flex flex-col">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-mono font-semibold">Documents</h3>
              <WorkspaceSheet onCreate={handleCreateWorkspace} />
            </div>
            
            <div className="mb-3">
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 transform -translate-y-1/2 h-3 w-3 text-muted-foreground" />
                <Input 
                  placeholder="Search documents..." 
                  className="pl-8 h-8 text-xs bg-glass/10 border-glass-border"
                />
              </div>
            </div>

            <ScrollArea className="flex-1">
              <div className="space-y-1.5">
                {docsInWorkspace.length > 0 ? (
                  docsInWorkspace.map((doc) => (
                    <div 
                      key={doc._additional.id}
                      onClick={() => setSelectedDoc(doc._additional.id)}
                      className={`p-2.5 rounded-lg bg-glass/10 hover:bg-glass/20 transition-colors cursor-pointer ${
                        selectedDoc === doc._additional.id ? 'bg-glass/30 border border-primary/50' : ''
                      }`}
                    >
                      <div className="flex items-start gap-2">
                        <FileText className="h-3 w-3 text-primary mt-0.5 flex-shrink-0" />
                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-medium truncate">{doc.title || 'Untitled'}</p>
                          <p className="text-[10px] text-muted-foreground">
                            {doc.pages || '?'} pages • {doc.source === 'drive' ? 'Drive' : 'Upload'}
                          </p>
                        </div>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button variant="ghost" size="icon" className="h-5 w-5">
                              <MoreVertical className="h-2.5 w-2.5" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem disabled>
                              <Move className="w-3 h-3 mr-2" />
                              Move to
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            {workspaces.map((ws) => (
                              <DropdownMenuItem
                                key={ws}
                                disabled={ws === workspace}
                                onSelect={() => handleMoveDoc(doc._additional.id, ws)}
                              >
                                {wsIcon(ws)}
                                <span className="ml-2 text-xs">{ws}</span>
                              </DropdownMenuItem>
                            ))}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              className="text-destructive"
                              onSelect={() => {
                                if (confirm('Are you sure you want to delete this document?')) {
                                  deleteDocument(doc._additional.id);
                                }
                              }}
                            >
                              <Trash2 className="w-3 h-3 mr-2" />
                              Delete
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-muted-foreground text-center py-4">
                    No documents in {workspace}
                  </p>
                )}
              </div>
            </ScrollArea>

            {/* Workspace switcher at bottom */}
            <div className="border-t border-glass-border pt-2 mt-2">
              <div className="flex items-center gap-1 flex-wrap">
                {workspaces.map((ws) => (
                  <Button
                    key={ws}
                    size="sm"
                    variant={workspace === ws ? "default" : "ghost"}
                    className={`h-7 text-xs px-2 ${workspace === ws ? "" : "hover:bg-glass/20"}`}
                    onClick={() => handleMoveDoc('__switch__', ws)}
                  >
                    {wsIcon(ws)}
                    <span className="ml-1">{ws}</span>
                  </Button>
                ))}
              </div>
            </div>
          </GlassCard>
          
          {/* Resize handle */}
          <div
            className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-primary/20 transition-colors"
            onMouseDown={() => setIsResizingLeft(true)}
          />
        </div>

        {/* Main Content Area */}
        <main className="flex-1 flex flex-col overflow-hidden min-w-0">
          {/* Header Bar with Glass Design */}
          <GlassCard variant="nav" className="m-2 mb-0 px-4 py-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => navigate(-1)}>
                  <ArrowLeft className="w-3 h-3 mr-1" /> Back
                </Button>
              </div>
              
              <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide">
                <Button 
                  variant="ghost" 
                  size="sm"
                  className="flex items-center gap-1.5 px-2 h-7 text-xs hover:bg-glass/20"
                  onClick={() => setShowSearch(true)}
                >
                  <Search className="w-3 h-3" /> Search
                  <kbd className="ml-1 px-1 text-[9px] bg-border/50 rounded">⌘K</kbd>
                </Button>
                
                <Button size="icon" variant="ghost" className="h-7 w-7" onClick={() => navigate('/uploads')}>
                  <FileText className="w-3 h-3" />
                </Button>
        
                <ConnectDriveButton onConnected={refetch} />
                <UploadButton
                  currentWorkspace={workspace}
                  workspaces={workspaces}
                  onUploaded={refetch}
                />
                <Button size="icon" variant="ghost" className="h-7 w-7">
                  <Settings className="w-3 h-3" />
                </Button>
                <UserButton />
              </div>
            </div>
          </GlassCard>

          {/* PDF Viewer Area with Glass Design */}
          <div className="flex-1 p-2">
            <GlassCard variant="feature" className="h-full p-3">
              {selectedDoc ? (
                <div className="h-full">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-mono font-semibold">
                      {docs.find(d => d._additional.id === selectedDoc)?.title || 'Document'}
                    </h3>
                  </div>
                  <div className="h-[calc(100%-2rem)]">
                    <PdfEditorView docId={selectedDoc} docs={docs} />
                  </div>
                </div>
              ) : (
                <div className="h-full flex items-center justify-center">
                  <div className="text-center">
                    <FileText className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
                    <p className="text-sm text-muted-foreground">Choose or upload a document to begin.</p>
                  </div>
                </div>
              )}
            </GlassCard>
          </div>
        </main>

        {/* AI Chat Panel with Glass Design - Resizable */}
        <AssistantPanel selectedDoc={selectedDoc} panelWidth={rightPanelWidth} />
      </div>

      {/* Search Modal */}
      <SearchModel isOpen={showSearch} onClose={() => setShowSearch(false)} />
    </div>
  );
}
