import { useEffect, useRef } from 'react';
import WebViewer from '@pdftron/pdfjs-express-viewer';

interface Props { docId: string | null }

export default function PdfEditorView({ docId }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const viewer    = useRef<any>(null);
  const btnRef    = useRef<HTMLButtonElement | null>(null);

  // Initialize/dispose WebViewer
  useEffect(() => {
    if (!container.current) return;

    // Clean up any existing instance before creating a new one
    if (viewer.current) {
      viewer.current.dispose();
      viewer.current = null;
    }

    WebViewer(
      {
        path: '/pdfjs-express',
        licenseKey: '7VPVv7vHAjudWJUtAoEU',
        config: { theme: 'dark' }
      },
      container.current
    ).then((inst: any) => {
      viewer.current = inst;
      inst.UI.setTheme('dark');

      // Allow floating button inside iframe
      Object.assign(container.current!.style, {
        position:  'relative',
        overflow:  'visible'
      });

      // Create the “➕ Add to chat” button
      const iwin = inst.UI.iframeWindow;
      const idoc = iwin.document;
      const btn  = idoc.createElement('button');
      btn.textContent = '➕ Add to chat';
      Object.assign(btn.style, {
        position:    'absolute',
        padding:     '4px 8px',
        fontSize:    '12px',
        background:  '#7C3AED',
        color:       '#fff',
        border:      'none',
        borderRadius:'4px',
        cursor:      'pointer',
        display:     'none',
        zIndex:      10000,
      });
      idoc.body.appendChild(btn);
      btnRef.current = btn;

      // On click → dispatch selection event
      btn.addEventListener('click', () => {
        const dv  = inst.Core.documentViewer as any;
        const sel = iwin.getSelection()?.toString().trim() || '';
        if (!sel || !docId) return;

        const filename = dv.getDocument()?.getFilename?.() || docId;
        const quads    = dv.getSelectionQuads?.() ?? [];
        const page     = quads.length
          ? quads[0].PageNumber
          : dv.getCurrentPage?.() || 1;

        let start = 0, end = 0;
        if (quads.length) {
          const lineH = 10;
          const ys    = quads.flatMap((q: any) =>
            q.Quads.map((qq: any) => qq.y1)
          );
          start = Math.round(Math.min(...ys) / lineH) + 1;
          end   = Math.round(Math.max(...ys) / lineH) + 1;
        }

        window.dispatchEvent(new CustomEvent('add-selection-to-chat', {
          detail: { docId, filename, text: sel, page, start, end }
        }));

        btn.style.display = 'none';
        iwin.getSelection()?.removeAllRanges();
      });

      // Show button on text selection
      idoc.addEventListener('mouseup', () => {
        setTimeout(() => {
          const sel = iwin.getSelection()?.toString().trim() || '';
          if (!sel) {
            btn.style.display = 'none';
            return;
          }
          const range = iwin.getSelection()!.getRangeAt(0);
          const rect  = range.getBoundingClientRect();
          btn.style.left    = `${rect.left}px`;
          btn.style.top     = `${rect.bottom + 6}px`;
          btn.style.display = 'block';
        }, 10);
      });

      // Load initial document
      if (docId) {
        inst.UI.loadDocument(`/api/${docId}.pdf`);
      }
    });

    // Dispose on unmount
    return () => {
      if (viewer.current) {
        viewer.current.dispose();
        viewer.current = null;
      }
    };
  }, [docId]);

  return (
    <div
      ref={container}
      className="w-full h-[85vh] rounded-2xl shadow-lg bg-black/30 backdrop-blur-lg overflow-hidden"
    />
  );
}
