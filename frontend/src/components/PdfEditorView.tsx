import { useEffect, useRef } from 'react';

interface Props { docId: string | null; }

export default function PdfEditorView({ docId }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const instance  = useRef<any>(null);

  /** initialise once */
  useEffect(() => {
    const boot = async () => {
      if (!container.current || instance.current) return;

      const WebViewer = (await import('@pdftron/pdfjs-express-viewer')).default;

      instance.current = await WebViewer(
        {
          path: '/pdfjs-express',
          licenseKey: '7VPVv7vHAjudWJUtAoEU',
        },
        container.current
      );

      /** add text-selection event → floating “Add to chat” button */
      instance.current.Core.documentViewer.addEventListener('textSelected', () => {
        const sel = instance.current.Core.documentViewer.getSelectedText();
        if (!sel.trim()) return;

        const button = document.createElement('button');
        button.textContent = '➕ Add to chat';
        button.className =
          'px-2 py-1 bg-primary text-white rounded shadow z-50';
        const quad = instance.current.Core.documentViewer
          .getSelectionManager()
          .getQuads(0)[0]
          ?.getBoundingBox();
        if (!quad) return;

        const iframeDoc = instance.current.UI.iframeWindow.document;
        button.style.position = 'absolute';
        button.style.left = `${quad.x1}px`;
        button.style.top  = `${quad.y2 + 6}px`;
        iframeDoc.body.appendChild(button);

        button.onclick = () => {
          window.dispatchEvent(
            new CustomEvent('add-selection-to-chat', {
              detail: { docId, text: sel },
            })
          );
          button.remove();
          instance.current.Core.documentViewer.clearSelection();
        };
      });

      if (docId) instance.current.UI.loadDocument(`/api/${docId}.pdf`);
    };
    boot();
  }, []);          // ← only first mount

  /** load when user selects new doc */
  useEffect(() => {
    if (instance.current && docId) {
      instance.current.UI.loadDocument(`/api/${docId}.pdf`);
    }
  }, [docId]);

  return (
    <div ref={container} className="w-full h-[85vh] rounded-xl shadow-md" />
  );
}
