import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { FileText, Clock, Zap } from "lucide-react";

interface DocumentLibraryProps {
  onSelectDoc: (docId: string) => void;
  selectedDoc: string | null;
}

/* shape returned by /api/documents */
interface DocMeta {
  _additional: { id: string };
  title: string | null;   // ← can be null
  processed: boolean;
  pages: number | null;
}

const fetchDocs = async (): Promise<DocMeta[]> =>
  (await fetch("/api/documents")).json();

const DocumentLibrary = ({ onSelectDoc, selectedDoc }: DocumentLibraryProps) => {
  const { data: docs = [], isLoading, refetch } = useQuery({
    queryKey: ["documents"],
    queryFn: fetchDocs,
  });

  /* refetch after successful upload */
  useEffect(() => {
    (window as any).refetchDocs = refetch;
  }, [refetch]);

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-20" />
        <Skeleton className="h-20" />
      </div>
    );
  }

  if (docs.length === 0) {
    return <p className="text-muted-foreground text-sm">No documents yet.</p>;
  }

  return (
    <div className="space-y-3">
      {docs.map((doc) => {
        /* defensive fallbacks */
        const id        = doc._additional?.id ?? crypto.randomUUID();
        const name      = doc.title?.trim() || "Untitled";
        const processed = Boolean(doc.processed);
        const pages     = doc.pages ?? "?";

        /* safe ext parse */
        const ext = name.includes(".") ? name.split(".").pop() : "";
        const type = ext ? ext!.toUpperCase() : "DOC";
        const size = `${pages} pg`;

        return (
          <Card
            key={id}
            onClick={() => onSelectDoc(id)}
            className={`p-4 cursor-pointer transition-all duration-300 hover:scale-[1.02] ${
              selectedDoc === id
                ? "bg-primary/10 border-primary/30 neon-glow"
                : "glass-morphism hover:bg-card/80"
            }`}
          >
            <div className="flex items-start space-x-3">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-r from-purple-500 to-pink-500 flex items-center justify-center text-white flex-shrink-0">
                <FileText className="w-5 h-5" />
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <h4 className="font-medium text-sm truncate">{name}</h4>

                  <Badge
                    variant="secondary"
                    className={`text-xs ${
                      processed
                        ? "bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400"
                        : "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/20 dark:text-yellow-400"
                    }`}
                  >
                    {processed ? (
                      <Zap className="w-3 h-3 mr-1" />
                    ) : (
                      <Clock className="w-3 h-3 mr-1" />
                    )}
                    {processed ? "Ready" : "Processing"}
                  </Badge>
                </div>

                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span>
                    {type} • {size}
                  </span>
                </div>
              </div>
            </div>
          </Card>
        );
      })}
    </div>
  );
};

export default DocumentLibrary;
