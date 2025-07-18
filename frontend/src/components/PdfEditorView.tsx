// src/components/PdfEditorView.tsx
import { useEffect, useRef, useState } from 'react';
import WebViewer from '@pdftron/pdfjs-express-viewer';

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
}

export default function PdfEditorView({ docId, docs = [] }: Props) {
  const viewerDiv = useRef<HTMLDivElement>(null);
  const instance = useRef<any>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const buttonRef = useRef<HTMLButtonElement | null>(null);

  // Initialize WebViewer only once
  useEffect(() => {
    if (!viewerDiv.current || isInitialized) return;

    WebViewer(
      {
        path: '/pdfjs-express',
        licenseKey: '7VPVv7vHAjudWJUtAoEU',
        initialDoc: docId ? `/api/${docId}.pdf` : undefined,
      },
      viewerDiv.current
    ).then((inst: any) => {
      instance.current = inst;
      setIsInitialized(true);
      
      // Apply dark theme
      inst.UI.setTheme('dark');
      
      // Set up the floating button for text selection
      const { documentViewer } = inst.Core;
      const iframeDoc = inst.UI.iframeWindow.document;
      
      // Create floating button
      const btn = iframeDoc.createElement('button');
      btn.textContent = '➕ Add to chat';
      btn.style.cssText = `
        position: absolute;
        padding: 4px 8px;
        font-size: 11px;
        background: #7C3AED;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        display: none;
        z-index: 10000;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      `;
      iframeDoc.body.appendChild(btn);
      buttonRef.current = btn;

      // Handle text selection
      const handleMouseUp = () => {
        setTimeout(() => {
          const selection = inst.UI.iframeWindow.getSelection();
          if (!selection || !selection.toString().trim()) {
            btn.style.display = 'none';
            return;
          }

          const range = selection.getRangeAt(0);
          const rect = range.getBoundingClientRect();
          
          // Calculate position to keep button in viewport
          const iframeRect = inst.UI.iframeWindow.frameElement.getBoundingClientRect();
          let left = rect.left;
          let top = rect.bottom + 6;
          
          // Adjust if button would go off-screen
          if (left + 100 > iframeRect.width) {
            left = iframeRect.width - 110;
          }
          if (left < 10) {
            left = 10;
          }
          
          btn.style.left = `${left}px`;
          btn.style.top = `${top}px`;
          btn.style.display = 'block';
        }, 10);
      };

      // Button click handler
      btn.addEventListener('click', () => {
        const selection = inst.UI.iframeWindow.getSelection();
        const text = selection?.toString().trim();
        if (!text || !docId) return;

        const currentPage = documentViewer.getCurrentPage();
        const doc = docs.find(d => d._additional.id === docId);
        const filename = doc?.title || 'Document';

        window.dispatchEvent(new CustomEvent('add-selection-to-chat', {
          detail: {
            docId,
            filename,
            text,
            page: currentPage,
            start: 0,
            end: text.length
          }
        }));

        btn.style.display = 'none';
        selection?.removeAllRanges();
      });

      iframeDoc.addEventListener('mouseup', handleMouseUp);
      iframeDoc.addEventListener('touchend', handleMouseUp);
      
      // Hide button on scroll
      inst.UI.iframeWindow.addEventListener('scroll', () => {
        if (btn.style.display === 'block') {
          btn.style.display = 'none';
        }
      });
    });

    // Cleanup function
    return () => {
      if (instance.current) {
        instance.current.UI.dispose();
        instance.current = null;
        setIsInitialized(false);
      }
    };
  }, []); // Only run once on mount

  // Handle document changes
  useEffect(() => {
    if (instance.current && docId && isInitialized) {
      instance.current.UI.loadDocument(`/api/${docId}.pdf`);
      // Hide the button when switching documents
      if (buttonRef.current) {
        buttonRef.current.style.display = 'none';
      }
    }
  }, [docId, isInitialized]);

  return (
    <div 
      ref={viewerDiv} 
      className="w-full h-full rounded-lg overflow-hidden"
      style={{ minHeight: '400px' }}
    />
  );
}