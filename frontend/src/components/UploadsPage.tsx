import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import DocumentLibrary from '@/components/DocumentLibrary';
import PdfEditorView   from '@/components/PdfEditorView';
import AssistantPanel  from '@/components/AssistantPanel';
import { Button }      from '@/components/ui/button';

export default function UploadsPage() {
  const [docId, setDocId] = useState<string | null>(null);
  const nav = useNavigate();

  return (
    <div className="flex h-screen">
      <aside className="w-80 border-r p-4 overflow-y-auto space-y-4 bg-black/30 backdrop-blur-lg">
        <Button variant="ghost" onClick={() => nav(-1)}>
          <ArrowLeft className="w-4 h-4 mr-1" /> Back
        </Button>
        <h2 className="font-semibold text-lg">All Uploaded Docs</h2>

        {/* noWorkspace prop -> flat list */}
        <DocumentLibrary
          selectedDoc={docId}
          onSelectDoc={setDocId}
          noWorkspace
        />
      </aside>

      <main className="flex-1 p-6 overflow-y-auto">
        {docId ? (
          <PdfEditorView docId={docId} />
        ) : (
          <p className="text-center text-muted-foreground mt-24">
            Click a document…
          </p>
        )}
      </main>

      <AssistantPanel selectedDoc={docId} />
    </div>
  );
}
