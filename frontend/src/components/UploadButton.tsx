import { useRef, useState } from "react";
import { toast } from "@/components/ui/use-toast";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  onUploaded: () => void;        // callback → refetch docs
}

export default function UploadButton({ onUploaded }: Props) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);

  const handleSelect = () => fileRef.current?.click();

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setLoading(true);

    const body = new FormData();
    body.append("file", file);

    try {
      const res = await fetch(`/api/upload`, {
        method: "POST",
        body,
      });
      if (!res.ok) throw new Error("Upload failed");
      toast({ title: "Uploaded ✔︎", description: file.name });
      onUploaded();               // refetch sidebar list
    } catch (err) {
      toast({
        title: "Upload error",
        description: (err as Error).message,
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <input
        type="file"
        accept=".pdf,.doc,.docx,.txt"
        hidden
        ref={fileRef}
        onChange={handleUpload}
      />
      <Button
        size="sm"
        variant="outline"
        className="glass-morphism"
        onClick={handleSelect}
        disabled={loading}
      >
        <Upload className="w-4 h-4 mr-2" />
        {loading ? "Uploading…" : "Upload"}
      </Button>
    </>
  );
}
