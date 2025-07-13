// src/components/PdfEditorView.tsx
import { useEffect, useRef } from 'react';

interface Props { docId: string | null; }

export default function PdfEditorView({ docId }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const instance  = useRef<any>(null);

  // — initialise WebViewer once —
  useEffect(() => {
    const boot = async () => {
      if (!container.current || instance.current) return;

      const WebViewer = (await import('@pdftron/pdfjs-express-viewer')).default;

      instance.current = await WebViewer(
        {
          path: '/pdfjs-express',                 // folder in /public
          licenseKey: '7VPVv7vHAjudWJUtAoEU',     // key works on localhost + dociq.tech
        },
        container.current
      );

      // load first doc if already selected
      if (docId) instance.current.UI.loadDocument(`/api/${docId}.pdf`);
    };

    boot();
  }, []);                                        // ← only on mount

  // — load new doc when selection changes —
  useEffect(() => {
    if (instance.current && docId) {
      instance.current.UI.loadDocument(`/api/${docId}.pdf`);
    }
  }, [docId]);

  return <div ref={container} className="w-full h-[85vh] rounded-xl shadow-md" />;
}
