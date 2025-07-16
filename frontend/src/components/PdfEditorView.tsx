import { useEffect, useRef } from 'react';

interface Props { docId: string | null }

/**
 * WebViewer wrapper that:
 *   ① boots PDF.js Express
 *   ② shows a floating “➕ Add to chat” button immediately after a highlight
 *   ③ dispatches filename + page + line-range with the selected text
 */
export default function PdfEditorView({ docId }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const viewer    = useRef<any>(null);
  const btnRef    = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const init = async () => {
      if (!container.current || viewer.current) return;

      // 1️⃣ Load viewer once
      const WebViewer = (await import('@pdftron/pdfjs-express-viewer')).default;
      viewer.current = await WebViewer(
        { path: '/pdfjs-express', licenseKey: '7VPVv7vHAjudWJUtAoEU' },
        container.current
      );
      viewer.current.UI.setTheme('dark');

      // 2️⃣ Ensure our host div can show floating children
      Object.assign(container.current.style, { position: 'relative', overflow: 'visible' });

      // 3️⃣ Create “Add to chat” button inside the viewer iframe
      const iwin = viewer.current.UI.iframeWindow;
      const idoc = iwin.document;
      const btn  = idoc.createElement('button');
      btn.textContent = '➕ Add to chat';
      Object.assign(btn.style, {
        position: 'absolute',
        padding:  '4px 8px',
        fontSize: '12px',
        background: '#7C3AED',
        color:   '#fff',
        border:  'none',
        borderRadius: '4px',
        cursor:  'pointer',
        display: 'none',
        zIndex:  10_000,
      });
      idoc.body.appendChild(btn);
      btnRef.current = btn;

      // 4️⃣ Click → fire event + hide
      btn.addEventListener('click', () => {
        const dv   = viewer.current.Core.documentViewer as any;
        const sel  = iwin.getSelection()?.toString().trim() || '';
        if (!sel || !docId) return;

        /* filename via Express API, fallback to docId */
        const filename: string =
          dv.getDocument()?.getFilename?.() || docId;

        /* page # */
        const quads = dv.getSelectionQuads?.() ?? [];
        const page  = quads.length ? quads[0].PageNumber : dv.getCurrentPage?.() || 1;

        /* rough start / end line-numbers from quad Y-offsets */
        let start = 0, end = 0;
        if (quads.length) {
          const lineH = 10;                                 // px estimate
          const ys    = quads.flatMap((q:any) => q.Quads.map((qq:any)=>qq.y1));
          start = Math.round(Math.min(...ys) / lineH) + 1;
          end   = Math.round(Math.max(...ys) / lineH) + 1;
        }

        window.dispatchEvent(new CustomEvent('add-selection-to-chat', {
          detail: { docId, filename, text: sel, page, start, end }
        }));

        btn.style.display = 'none';
        iwin.getSelection()?.removeAllRanges();
      });

      // 5️⃣ Show button immediately after mouse-up if something is selected
      idoc.addEventListener('mouseup', () => {
        setTimeout(() => {
          const sel = iwin.getSelection()?.toString().trim() || '';
          if (!sel) { btn.style.display = 'none'; return; }

          const range = iwin.getSelection()!.getRangeAt(0);
          const rect  = range.getBoundingClientRect();
          btn.style.left   = `${rect.left}px`;
          btn.style.top    = `${rect.bottom + 6}px`;
          btn.style.display = 'block';
        }, 10);
      });

      /* first load */
      if (docId) viewer.current.UI.loadDocument(`/api/${docId}.pdf`);
    };
    init();
  }, []);

  /* when user picks another doc */
  useEffect(() => {
    if (viewer.current && docId) {
      viewer.current.UI.loadDocument(`/api/${docId}.pdf`);
      if (btnRef.current) btnRef.current.style.display = 'none';
    }
  }, [docId]);

  return <div ref={container} className="w-full h-[85vh] rounded-xl shadow-md"/>;
}
