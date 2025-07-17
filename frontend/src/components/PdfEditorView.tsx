import { useEffect, useRef } from 'react';
import WebViewer from '@pdftron/pdfjs-express-viewer';

interface Props { docId: string | null }

export default function PdfEditorView({ docId }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const instance  = useRef<any>(null);
  const btnRef    = useRef<HTMLButtonElement | null>(null);

  /* ─ Initialise ONCE (React-18 strict-mode proof) ─ */
  useEffect(() => {
    if (!container.current || instance.current) return;   // already initialised

    WebViewer(
      {
        path      : '/pdfjs-express',      // make sure this folder is in /public
        licenseKey: '7VPVv7vHAjudWJUtAoEU',
        config    : { theme: 'dark' },
      },
      container.current
    ).then((inst: any) => {
      instance.current = inst;
      inst.UI.setTheme('dark');

      /* style tweaks so our floating button can escape iframe bounds */
      Object.assign(container.current!.style, {
        position : 'relative',
        overflow : 'visible',
      });

      /* floating “Add to chat” button ------------------------------ */
      const iwin = inst.UI.iframeWindow;
      const idoc = iwin.document;
      const btn  = idoc.createElement('button');
      btn.textContent = '➕ Add to chat';
      Object.assign(btn.style, {
        position      : 'absolute',
        padding       : '4px 8px',
        fontSize      : '12px',
        background    : '#7C3AED',
        color         : '#fff',
        border        : 'none',
        borderRadius  : '4px',
        cursor        : 'pointer',
        display       : 'none',
        zIndex        : 10_000,
      });
      idoc.body.appendChild(btn);
      btnRef.current = btn;

      btn.addEventListener('click', () => {
        if (!docId) return;
        const sel = iwin.getSelection()?.toString().trim();
        if (!sel) return;

        const dv       = inst.Core.documentViewer as any;
        const filename = dv.getDocument()?.getFilename?.() || docId;
        const quads    = dv.getSelectionQuads?.() ?? [];
        const page     = quads.length ? quads[0].PageNumber
                                      : dv.getCurrentPage?.() || 1;

        window.dispatchEvent(new CustomEvent('add-selection-to-chat', {
          detail: { docId, filename, text: sel, page, start: 0, end: 0 }
        }));

        btn.style.display = 'none';
        iwin.getSelection()?.removeAllRanges();
      });

      /* show button when user selects text */
      idoc.addEventListener('mouseup', () => {
        setTimeout(() => {
          const r = iwin.getSelection();
          if (!r || r.toString().trim() === '') {
            btn.style.display = 'none';
            return;
          }
          const rect = r.getRangeAt(0).getBoundingClientRect();
          btn.style.left    = `${rect.left}px`;
          btn.style.top     = `${rect.bottom + 6}px`;
          btn.style.display = 'block';
        }, 10);
      });

      /* initial load */
      if (docId) inst.UI.loadDocument(`/api/${docId}.pdf`);
    });

    /* dispose on unmount */
    return () => {
      instance.current?.dispose();
      instance.current = null;
    };
  }, []);

  /* ─ Switch PDF when docId changes ─ */
  useEffect(() => {
    if (instance.current && docId) {
      instance.current.UI.loadDocument(`/api/${docId}.pdf`);
      btnRef.current && (btnRef.current.style.display = 'none');
    }
  }, [docId]);

  return (
    <div
      ref={container}
      className="w-full h-[85vh] rounded-2xl shadow-lg bg-black/30 backdrop-blur-lg overflow-hidden"
    />
  );
}
