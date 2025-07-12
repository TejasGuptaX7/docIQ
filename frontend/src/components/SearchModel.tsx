import React, { useState, useEffect } from "react";
import { Search, Clock, File, Brain, Zap } from "lucide-react";
import { Card } from "@/components/ui/card";            // tailwind-styled card wrapper

interface SearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SearchModal: React.FC<SearchModalProps> = ({ isOpen, onClose }) => {
  const [query, setQuery] = useState("");

  // demo “recent” data – swap for real history later
  const recentSearches = [
    "project proposal draft",
    "meeting notes Q3",
    "research on AI trends",
    "budget spreadsheet",
  ];

  /* --- Esc to close ------------------------------------------------------ */
  useEffect(() => {
    const escListener = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (isOpen) {
      document.addEventListener("keydown", escListener);
      return () => document.removeEventListener("keydown", escListener);
    }
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-start justify-center pt-32">
      <Card className="w-full max-w-2xl mx-4 border-border glass-morphism shadow-2xl">
        {/* Search input row ------------------------------------------------- */}
        <div className="flex items-center p-4 border-b border-border">
          <Search className="w-5 h-5 text-muted-foreground mr-3" />
          <input
            autoFocus
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search documents, queries, and canvas content…"
            className="flex-1 bg-transparent outline-none placeholder-muted-foreground"
          />
          <kbd className="px-2 py-1 text-xs bg-muted rounded border text-muted-foreground">
            ESC
          </kbd>
        </div>

        {/* Results / recent / quick actions -------------------------------- */}
        <div className="p-4 max-h-96 overflow-y-auto">
          {query ? (
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground mb-3">Search results</p>
              {/* ⇨ TODO: map real results here */}
              <div className="p-3 rounded-lg hover:bg-muted/50 cursor-pointer flex items-center space-x-3">
                <File className="w-4 h-4 text-indigo-400" />
                <div>
                  <p className="font-medium">Project Proposal.pdf</p>
                  <p className="text-sm text-muted-foreground">
                    Contains “{query}”
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Recent ---------------------------------------------------- */}
              <div>
                <p className="flex items-center text-sm text-muted-foreground mb-2">
                  <Clock className="w-4 h-4 mr-2" />
                  Recent searches
                </p>
                {recentSearches.map((text) => (
                  <button
                    key={text}
                    onClick={() => setQuery(text)}
                    className="block w-full text-left p-3 rounded-lg hover:bg-muted/50 text-sm"
                  >
                    {text}
                  </button>
                ))}
              </div>

              {/* Quick actions -------------------------------------------- */}
              <div>
                <p className="flex items-center text-sm text-muted-foreground mb-2">
                  <Zap className="w-4 h-4 mr-2" />
                  Quick actions
                </p>
                <div className="space-y-1">
                  <button className="w-full p-3 rounded-lg hover:bg-muted/50 flex items-center text-sm">
                    <Brain className="w-4 h-4 mr-3 text-indigo-400" />
                    Ask AI assistant
                  </button>
                  <button className="w-full p-3 rounded-lg hover:bg-muted/50 flex items-center text-sm">
                    <File className="w-4 h-4 mr-3 text-teal-400" />
                    Upload documents
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
};

export default SearchModal;
