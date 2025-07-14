import { useMemo } from 'react';
import {
  Folder, BookOpen, Sparkles, FileText, Globe,
  EllipsisVertical, Move, Plus,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card   } from '@/components/ui/card';
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
  onNew: () => void; // opens WorkspaceSheet
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
  /* docs visible in current workspace */
  const docsInWs = useMemo(
    () =>
      docs.filter(
        (d) => (tags[d._additional.id] || 'default') === selectedWorkspace
      ),
    [docs, tags, selectedWorkspace]
  );

  /* helper to switch workspace from the rail or menu */
  const switchTo = (ws: string) => onMoveDoc('__switch__', ws);

  return (
    <aside className="w-80 flex flex-col border-r bg-card/60">
      {/* document list */}
      <ScrollArea className="flex-1 p-4">
        {docsInWs.length ? (
          docsInWs.map((doc) => (
            <Card
              key={doc._additional.id}
              onClick={() => onSelectDoc(doc._additional.id)}
              className={`p-3 mb-2 cursor-pointer ${
                selectedDoc === doc._additional.id
                  ? 'bg-primary/10 border-primary'
                  : 'hover:bg-card/70'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {wsIcon(selectedWorkspace)}
                  <span className="text-sm truncate w-40">
                    {doc.title || 'Untitled'}
                  </span>
                </div>

                {/* “Move to …” */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button size="icon" variant="ghost">
                      <EllipsisVertical className="w-4 h-4" />
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
                        onSelect={() => onMoveDoc(doc._additional.id, ws)}
                      >
                        {wsIcon(ws)}
                        <span className="ml-2">{ws}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </Card>
          ))
        ) : (
          <p className="text-xs text-muted-foreground p-2">
            No docs in “{selectedWorkspace}”
          </p>
        )}
      </ScrollArea>

      {/* bottom rail */}
      <div className="border-t p-2 flex items-center justify-between">
        {/* existing workspace icons */}
        <div className="flex gap-1">
          {workspaces.map((ws) => (
            <Button
              key={ws}
              size="icon"
              variant={ws === selectedWorkspace ? 'default' : 'ghost'}
              onClick={() => switchTo(ws)}
              title={ws}
            >
              {wsIcon(ws)}
            </Button>
          ))}
        </div>

        {/* ＋ menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button size="icon" variant="ghost">
              <Plus className="w-5 h-5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="end" className="w-44">
            <DropdownMenuItem onSelect={onNew}>＋ New Workspace</DropdownMenuItem>
            <DropdownMenuSeparator />
            {DEFAULT_TEMPLATES.map((tpl) => (
              <DropdownMenuItem
                key={tpl}
                onSelect={() => switchTo(tpl)}
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
