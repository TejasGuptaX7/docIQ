// src/components/WorkspaceSidebar.tsx
import { useMemo } from 'react';
import {
  Folder, BookOpen, Sparkles, FileText, Globe,
  EllipsisVertical, Move, Plus,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuTrigger,
  DropdownMenuItem, DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';

interface Doc {
  _additional: { id: string };
  title: string | null;
  pages: number | null;
}

interface Props {
  docs: Doc[];
  tags: Record<string, string>;
  selectedDoc: string | null;
  selectedWorkspace: string;
  onSelectDoc: (id: string) => void;
  onMoveDoc: (docId: string, ws: string) => void;
  workspaces: string[];
  wsIcon: (w: string) => JSX.Element;
  onNew: () => void;
}

const DEFAULT_TEMPLATES = ['Research', 'Academic', 'Fun', 'Literature', 'Travel'];

export default function WorkspaceSidebar({
  docs,
  tags,
  selectedDoc,
  selectedWorkspace,
  onSelectDoc,
  onMoveDoc,
  workspaces,
  wsIcon,
  onNew,
}: Props) {
  // Get docs visible in current workspace
  const docsInWs = useMemo(
    () =>
      docs.filter(
        (d) => (tags[d._additional.id] || 'default') === selectedWorkspace
      ),
    [docs, tags, selectedWorkspace]
  );

  // Helper to switch workspace from the rail or menu
  const switchTo = (ws: string) => onMoveDoc('__switch__', ws);

  // Handle document move with proper state update
  const handleMoveDoc = async (docId: string, targetWs: string) => {
    // Call the move function
    await onMoveDoc(docId, targetWs);
    
    // If moving the currently selected doc, deselect it
    if (docId === selectedDoc && targetWs !== selectedWorkspace) {
      onSelectDoc('');
    }
  };

  return (
    <aside className="w-full h-full flex flex-col bg-card/60">
      {/* Document list */}
      <ScrollArea className="flex-1 p-3">
        {docsInWs.length ? (
          docsInWs.map((doc) => (
            <Card
              key={doc._additional.id}
              onClick={() => onSelectDoc(doc._additional.id)}
              className={`p-2.5 mb-1.5 cursor-pointer transition-all ${
                selectedDoc === doc._additional.id
                  ? 'bg-primary/10 border-primary'
                  : 'hover:bg-card/70'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 min-w-0">
                  {wsIcon(selectedWorkspace)}
                  <span className="text-xs truncate">
                    {doc.title || 'Untitled'}
                  </span>
                </div>

                {/* Move to menu */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                    <Button size="icon" variant="ghost" className="h-6 w-6 flex-shrink-0">
                      <EllipsisVertical className="w-3 h-3" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" side="left">
                    <DropdownMenuItem disabled>
                      <Move className="w-3 h-3 mr-2" />
                      Move to…
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    {workspaces.map((ws) => (
                      <DropdownMenuItem
                        key={ws}
                        disabled={ws === selectedWorkspace}
                        onSelect={() => handleMoveDoc(doc._additional.id, ws)}
                      >
                        {wsIcon(ws)}
                        <span className="ml-2 text-xs">{ws}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </Card>
          ))
        ) : (
          <p className="text-xs text-muted-foreground text-center p-4">
            No documents in "{selectedWorkspace}"
          </p>
        )}
      </ScrollArea>

      {/* Bottom rail */}
      <div className="border-t p-2 flex items-center justify-between">
        {/* Existing workspace icons */}
        <div className="flex gap-0.5 flex-wrap">
          {workspaces.map((ws) => (
            <Button
              key={ws}
              size="icon"
              variant={ws === selectedWorkspace ? 'default' : 'ghost'}
              onClick={() => switchTo(ws)}
              title={ws}
              className="h-7 w-7"
            >
              {wsIcon(ws)}
            </Button>
          ))}
        </div>

        {/* + menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button size="icon" variant="ghost" className="h-7 w-7">
              <Plus className="w-4 h-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="end" className="w-40">
            <DropdownMenuItem onSelect={onNew} className="text-xs">
              ＋ New Workspace
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            {DEFAULT_TEMPLATES.map((tpl) => (
              <DropdownMenuItem
                key={tpl}
                onSelect={() => switchTo(tpl)}
                disabled={workspaces.includes(tpl)}
                className="text-xs"
              >
                {wsIcon(tpl)}
                <span className="ml-2">{tpl}</span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </aside>
  );
}