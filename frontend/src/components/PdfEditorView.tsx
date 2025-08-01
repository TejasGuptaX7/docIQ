import { useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from '@clerk/clerk-react';

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

// Import WebViewer from PDFjs Express
declare const WebViewer: any;

export default function PdfEditorView({ docId, docs = [], onSelectionAdd }: Props) {
  const viewerDiv = useRef<HTMLDivElement>(null);
  const instance = useRef<any>(null);
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const cleanupRef = useRef<(() => void) | null>(null);
  
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { getToken } = useAuth();

  // Cleanup function
  const cleanup = useCallback(() => {
    if (cleanupRef.current) {
      cleanupRef.current();
      cleanupRef.current = null;
    }
    
    if (buttonRef.current) {
      buttonRef.current.remove();
      buttonRef.current = null;
    }
    
    if (instance.current) {
      try {
        instance.current.UI.dispose();
      } catch (e) {
        console.warn('Error disposing WebViewer:', e);
      }
      instance.current = null;
    }
    
    setIsInitialized(false);
  }, []);

  // Initialize WebViewer
  useEffect(() => {
    if (!viewerDiv.current || isInitialized) return;

    const initializeViewer = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Import the WebViewer script if not already loaded
        if (typeof WebViewer === 'undefined') {
          await new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = '/pdfjs-express/webviewer.min.js';
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
          });
        }

        const inst = await WebViewer(
          {
            path: '/pdfjs-express',
            licenseKey: '7VPVv7vHAjudWJUtAoEU',
            disabledElements: [
              'leftPanelButton',
              'selectToolButton',
              'stickyToolButton',
              'calloutToolButton',
            ],
          },
          viewerDiv.current
        );

        instance.current = inst;
        
        // Apply dark theme
        inst.UI.setTheme('dark');
        
        // Set up text selection functionality
        const { documentViewer } = inst.Core;
        const iframeDoc = inst.UI.iframeWindow.document;
        
        // Create floating button with improved styling
        const btn = iframeDoc.createElement('button');
        btn.innerHTML = `
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Add to chat
        `;
        btn.style.cssText = `
          position: absolute;
          padding: 6px 12px;
          font-size: 12px;
          font-weight: 500;
          background: #7C3AED;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          display: none;
          z-index: 10000;
          box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
          transition: all 0.2s ease;
          font-family: system-ui, -apple-system, sans-serif;
          display: flex;
          align-items: center;
          gap: 6px;
          white-space: nowrap;
        `;
        
        // Add hover effects
        btn.addEventListener('mouseenter', () => {
          btn.style.background = '#6D28D9';
          btn.style.transform = 'translateY(-1px)';
        });
        
        btn.addEventListener('mouseleave', () => {
          btn.style.background = '#7C3AED';
          btn.style.transform = 'translateY(0)';
        });
        
        iframeDoc.body.appendChild(btn);
        buttonRef.current = btn;

        // Debounced selection handler
        let selectionTimeout: NodeJS.Timeout;
        const handleSelection = () => {
          clearTimeout(selectionTimeout);
          selectionTimeout = setTimeout(() => {
            const selection = inst.UI.iframeWindow.getSelection();
            if (!selection || !selection.toString().trim()) {
              btn.style.display = 'none';
              return;
            }

            const range = selection.getRangeAt(0);
            const rect = range.getBoundingClientRect();
            const iframeRect = inst.UI.iframeWindow.frameElement.getBoundingClientRect();
            
            // Smart positioning to keep button in viewport
            const buttonWidth = 120;
            const margin = 10;
            
            let left = Math.max(margin, Math.min(
              rect.left + (rect.width - buttonWidth) / 2,
              iframeRect.width - buttonWidth - margin
            ));
            
            let top = rect.bottom + 8;
            if (top + 40 > iframeRect.height) {
              top = rect.top - 40;
            }
            
            btn.style.left = `${left}px`;
            btn.style.top = `${top}px`;
            btn.style.display = 'flex';
          }, 100);
        };

        // Enhanced button click handler
        const handleButtonClick = () => {
          const selection = inst.UI.iframeWindow.getSelection();
          const text = selection?.toString().trim();
          if (!text || !docId) return;

          const currentPage = documentViewer.getCurrentPage();
          const doc = docs.find(d => d._additional.id === docId);
          const filename = doc?.title || 'Document';

          const selectionData: SelectionData = {
            docId,
            filename,
            text,
            page: currentPage,
            start: 0,
            end: text.length
          };

          // Use callback if provided, otherwise use custom event
          if (onSelectionAdd) {
            onSelectionAdd(selectionData);
          } else {
            window.dispatchEvent(new CustomEvent('add-selection-to-chat', {
              detail: selectionData
            }));
          }

          btn.style.display = 'none';
          selection?.removeAllRanges();
        };

        btn.addEventListener('click', handleButtonClick);

        // Event listeners
        const events = [
          { target: iframeDoc, event: 'mouseup', handler: handleSelection },
          { target: iframeDoc, event: 'touchend', handler: handleSelection },
          { target: inst.UI.iframeWindow, event: 'scroll', handler: () => btn.style.display = 'none' },
          { target: iframeDoc, event: 'click', handler: (e: Event) => {
            if (e.target !== btn) btn.style.display = 'none';
          }}
        ];

        events.forEach(({ target, event, handler }) => {
          target.addEventListener(event, handler);
        });

        // Store cleanup function
        cleanupRef.current = () => {
          clearTimeout(selectionTimeout);
          events.forEach(({ target, event, handler }) => {
            target.removeEventListener(event, handler);
          });
        };

        setIsInitialized(true);
        setIsLoading(false);

        // Load initial document if provided
        if (docId) {
          await loadDocument(docId);
        }

      } catch (err) {
        console.error('Error initializing WebViewer:', err);
        setError('Failed to load PDF viewer. Please try refreshing the page.');
        setIsLoading(false);
      }
    };

    initializeViewer();

    return cleanup;
  }, []); // Only run once on mount

  // Load document function
  const loadDocument = async (documentId: string) => {
    if (!instance.current) return;
    
    try {
      setIsLoading(true);
      const token = await getToken();
      
      // Load document using the API endpoint
      await instance.current.UI.loadDocument(`/api/pdf/${documentId}`, {
        customHeaders: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      // Hide the button when switching documents
      if (buttonRef.current) {
        buttonRef.current.style.display = 'none';
      }
      
      setIsLoading(false);
    } catch (err) {
      console.error('Error loading document:', err);
      setError('Failed to load document. Please check if the file exists.');
      setIsLoading(false);
    }
  };

  // Handle document changes
  useEffect(() => {
    if (instance.current && docId && isInitialized && docId !== instance.current.lastDocId) {
      instance.current.lastDocId = docId;
      loadDocument(docId);
    }
  }, [docId, isInitialized]);

  // Loading state
  if (isLoading && !isInitialized) {
    return (
      <div className="w-full h-full flex items-center justify-center bg-gray-50 dark:bg-gray-900 rounded-lg">
        <div className="text-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">Loading PDF viewer...</p>
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
          <button 
            onClick={() => window.location.reload()} 
            className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
          >
            Refresh Page
          </button>
        </div>
      </div>
    );
  }

  return (
    <div 
      ref={viewerDiv} 
      className="w-full h-full rounded-lg overflow-hidden border border-border"
      style={{ minHeight: '400px' }}
    />
  );
}