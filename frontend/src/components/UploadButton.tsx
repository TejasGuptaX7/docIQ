import { useRef, useState } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { toast } from '@/components/ui/use-toast';
import { Upload, UploadCloud } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select, SelectContent, SelectItem,
  SelectTrigger, SelectValue,
} from '@/components/ui/select';

declare global {
  interface Window {
    uploadcare: any;
  }
}

interface Props {
  currentWorkspace: string;
  workspaces: string[];
  onUploaded: () => void;          // ← callback from parent
}

export default function UploadButton({ currentWorkspace, workspaces, onUploaded }: Props) {
  const { getToken }          = useAuth();
  const fileRef               = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [ws, setWs]           = useState(currentWorkspace);

  /* — Local helpers — */
  const handleSelect = () => fileRef.current?.click();

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setLoading(true);

    try {
      const token = await getToken();
      const body  = new FormData();
      body.append('file', file);
      body.append('workspace', ws);

      const res = await fetch('/api/upload', {
        method : 'POST',
        body,
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!res.ok) throw new Error(await res.text());

      const result = await res.json();
      toast({
        title      : 'Uploaded ✔︎',
        description: `${file.name} (${result.chunks} chunks, ${result.words} words)`
      });

      onUploaded();                       // ← HERE  🎉
    } catch (err: any) {
      toast({
        title      : 'Upload error',
        description: err.message,
        variant    : 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCloudConnect = async () => {
    const token = await getToken();
    const widget = window.uploadcare.openDialog(null, {
      tabs: 'file url gdrive dropbox onedrive',
    });

    widget.done(async (fileInfo: any) => {
      try {
        const res = await fetch('/api/upload/external', {
          method : 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization : `Bearer ${token}`
          },
          body: JSON.stringify({
            url      : fileInfo.cdnUrl,
            name     : fileInfo.name,
            workspace: ws,
          }),
        });

        if (!res.ok) throw new Error('Cloud upload failed');
        toast({ title: 'Cloud file connected ✔︎', description: fileInfo.name });

        onUploaded();                     // ← HERE  🎉
      } catch (err: any) {
        toast({
          title      : 'Cloud upload error',
          description: err.message,
          variant    : 'destructive',
        });
      }
    });
  };

  /* — JSX — */
  return (
    <>
      <input type="file" hidden ref={fileRef} onChange={handleUpload}
             accept=".pdf,.doc,.docx,.txt" />

      <div className="flex items-center gap-2">
        <Select value={ws} onValueChange={setWs}>
          <SelectTrigger className="w-28 h-8 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {workspaces.map(w => (
              <SelectItem key={w} value={w}>{w}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Button size="sm" variant="outline" onClick={handleSelect} disabled={loading}>
          <Upload className="w-4 h-4 mr-1" />
          {loading ? '…' : 'Upload'}
        </Button>

        <Button size="sm" variant="outline" onClick={handleCloudConnect}>
          <UploadCloud className="w-4 h-4 mr-1" /> Connect&nbsp;Cloud
        </Button>
      </div>
    </>
  );
}
