import { useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Download, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface Doc {
  _additional: { id: string };
  title: string | null;
  pages: number | null;
  source?: string;
  workspace?: string;
}

interface Props { 
  docId: string | null;
  docs?: Doc[];
  onSelectionAdd?: (selection: SelectionData) => void;
}

interface SelectionData {
  docId: string;
  filename: string;
  text: string;
  page: number;
  start: number;
  end: number;
}

export default function PdfEditorView({ docId, docs = [], onSelectionAdd }: Props) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const iframeRef = useRef<HTMLIFrameElement>(null);
  
  const { getToken } = useAuth();

  // Load document
  const loadDocument = async (documentId: string) => {
    if (!documentId) return;
    
    try {
      setIsLoading(true);
      setError(null);
      
      const token = await getToken();
      
      // Create PDF URL with auth token
      const url = `/api/pdf/${documentId}?token=${encodeURIComponent(token || '')}`;
      setPdfUrl(url);
      
      setIsLoading(false);
    } catch (err) {
      console.error('Error loading document:', err);
      setError('Failed to load document. Please try again.');
      setIsLoading(false);
    }
  };

  // Handle document changes
  useEffect(() => {
    if (docId) {
      loadDocument(docId);
    } else {
      setPdfUrl(null);
    }
  }, [docId]);

  // Handle selection functionality
  useEffect(() => {
    if (!iframeRef.current || !pdfUrl) return;

    const handleMessage = (event: MessageEvent) => {
      // Handle selection messages from iframe if needed
      if (event.data.type === 'selection' && onSelectionAdd && docId) {
        const doc = docs.find(d => d._additional.id === docId);
        const filename = doc?.title || 'Document';
        
        onSelectionAdd({
          docId,
          filename,
          text: event.data.text,
          page: event.data.page || 1,
          start: 0,
          end: event.data.text.length
        });
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [pdfUrl, docId, docs, onSelectionAdd]);

  // Loading state
  if (isLoading) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-900 rounded-lg">
        <div className="text-center space-y-4">
          <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto" />
          <p className="text-muted-foreground">Loading PDF...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-900 rounded-lg">
        <div className="text-center space-y-4 p-6">
          <div className="text-red-500 text-2xl">⚠️</div>
          <p className="text-red-600 dark:text-red-400 font-medium">{error}</p>
          <Button 
            onClick={() => docId && loadDocument(docId)} 
            className="px-4 py-2"
          >
            Try Again
          </Button>
        </div>
      </div>
    );
  }

  // No document selected
  if (!docId || !pdfUrl) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-900 rounded-lg">
        <div className="text-center space-y-4 p-6">
          <p className="text-muted-foreground">Select a document to view</p>
        </div>
      </div>
    );
  }

  // PDF viewer using native browser PDF rendering
  return (
    <div className="w-full h-full flex flex-col bg-gray-50 dark:bg-gray-900 rounded-lg overflow-hidden">
      {/* Simple toolbar */}
      <div className="flex items-center justify-between p-3 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {docs.find(d => d._additional.id === docId)?.title || 'Document'}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              const link = document.createElement('a');
              link.href = pdfUrl;
              link.download = docs.find(d => d._additional.id === docId)?.title || 'document.pdf';
              link.click();
            }}
          >
            <Download className="h-4 w-4" />
          </Button>
        </div>
      </div>
      
      {/* PDF iframe */}
      <div className="flex-1 relative">
        <iframe
          ref={iframeRef}
          src={pdfUrl}
          className="w-full h-full border-0"
          title="PDF Viewer"
          style={{ 
            backgroundColor: 'white',
            minHeight: '500px'
          }}
        />
        
        {/* Overlay message for text selection */}
        <div className="absolute bottom-4 right-4 bg-black/75 text-white text-xs px-3 py-2 rounded-md pointer-events-none">
          💡 Tip: Use Ctrl+C to copy text from the PDF
        </div>
      </div>
    </div>
  );
}