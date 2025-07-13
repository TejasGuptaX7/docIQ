import { useEffect, useRef } from "react";

const PdfEditorView = ({ docId }: { docId: string }) => {
  const viewerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const load = async () => {
      const WebViewer = (await import('@pdftron/pdfjs-express-viewer')).default;
      WebViewer(
        {
          path: "/pdfjs-express",  // ✅ folder inside public
          initialDoc: `/api/${docId}.pdf`, // ✅ your Spring Boot PDF endpoint
        },
        viewerRef.current!
      );
    };
    load();
  }, [docId]);

  return <div ref={viewerRef} className="w-full h-[85vh] rounded-xl shadow-md" />;
};

export default PdfEditorView;
