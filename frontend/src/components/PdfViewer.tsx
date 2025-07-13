import { useEffect, useRef } from "react";

interface PdfEditorViewProps {
  docId: string;
}

const PdfEditorView = ({ docId }: PdfEditorViewProps) => {
  const viewerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const loadViewer = async () => {
      const WebViewer = (await import("@pdftron/pdfjs-express-viewer")).default;

      WebViewer(
        {
          path: "/pdfjs-express", // 👈 must match public folder
          initialDoc: `/api/${docId}.pdf`,
        },
        viewerRef.current!
      ).then((instance) => {
        console.log("PDF viewer ready!", instance);
      });
    };

    loadViewer();
  }, [docId]);

  return (
    <div className="w-full h-[80vh] border rounded shadow" ref={viewerRef} />
  );
};

export default PdfEditorView;
