import React, { useState, useEffect } from 'react';
import {
  Search, Clock, File, Brain, Zap, Cloud,
} from 'lucide-react';
import { Card } from '@/components/ui/card';

interface Props { isOpen: boolean; onClose: () => void; }
const recentDemo = ['project proposal draft', 'meeting notes Q3', 'AI trends', 'budget spreadsheet'];

export default function SearchModal({ isOpen, onClose }: Props) {
  const [query, setQuery] = useState('');

  /* ESC to close */
  useEffect(() => {
    const f = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    if (isOpen) { document.addEventListener('keydown', f); return () => document.removeEventListener('keydown', f); }
  }, [isOpen, onClose]);

  /* — Uploadcare dialog — */
  const openUploader = () => {
    // @ts-ignore   (global from <script … uploadcare…>)
    const dialog = window.uploadcare.openDialog(null, {
      multiple: true,
      tabs:     'file url gdrive dropbox onedrive',
    });

    dialog.done((group: any) => {
      group.files().forEach(async (p: any) => {
        const file = await p;                                 // CDN file JSON
        await fetch('/api/upload/external', {                 // ⇦ tiny endpoint, same as before
          method:  'POST',
          headers: { 'Content-Type': 'application/json' },
          body:    JSON.stringify({ url: file.cdnUrl, name: file.name }),
        });
        // Let sidebar refresh
        (window as any).refetchDocs?.();
      });
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-start justify-center pt-32">
      <Card className="w-full max-w-2xl mx-4 border-border glass-morphism shadow-2xl animate-scale-in">
        {/* Search bar */}
        <div className="flex items-center p-4 border-b border-border">
          <Search className="w-5 h-5 text-muted-foreground mr-3" />
          <input
            autoFocus
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search documents, queries, and canvas content…"
            className="flex-1 bg-transparent outline-none placeholder-muted-foreground"
          />
          <kbd className="px-2 py-1 text-xs bg-muted rounded border text-muted-foreground">ESC</kbd>
        </div>

        {/* Body */}
        <div className="p-4 max-h-96 overflow-y-auto space-y-6">
          {query ? (
            <>
              <p className="text-sm text-muted-foreground mb-2">Search results</p>
              {/* TODO: real search */}
              <button className="w-full p-3 rounded-lg hover:bg-muted/50 flex items-center space-x-3">
                <File className="w-4 h-4 text-indigo-400" />
                <span className="font-medium">Project Proposal.pdf</span>
              </button>
            </>
          ) : (
            <>
              {/* Recent */}
              <div>
                <p className="flex items-center text-sm text-muted-foreground mb-2">
                  <Clock className="w-4 h-4 mr-2" /> Recent searches
                </p>
                {recentDemo.map(t => (
                  <button key={t} onClick={() => setQuery(t)}
                          className="block w-full text-left p-3 rounded-lg hover:bg-muted/50 text-sm">
                    {t}
                  </button>
                ))}
              </div>

              {/* Quick actions */}
              <div>
                <p className="flex items-center text-sm text-muted-foreground mb-2">
                  <Zap className="w-4 h-4 mr-2" /> Quick actions
                </p>
                <div className="space-y-1">
                  <button className="w-full p-3 rounded-lg hover:bg-muted/50 flex items-center text-sm">
                    <Brain className="w-4 h-4 mr-3 text-indigo-400" /> Ask AI assistant
                  </button>
                  <button
                    onClick={openUploader}
                    className="w-full p-3 rounded-lg hover:bg-muted/50 flex items-center text-sm"
                  >
                    <Cloud className="w-4 h-4 mr-3 text-teal-400" /> Upload / Connect cloud storage
                  </button>
                </div>
              </div>

              <p className="text-xs text-muted-foreground">
                Powered by free Uploadcare widget (3 GB quota).
              </p>
            </>
          )}
        </div>
      </Card>
    </div>
  );
}
