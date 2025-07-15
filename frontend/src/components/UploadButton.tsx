import { useRef, useState } from 'react';
import { toast } from '@/components/ui/use-toast';
import { Upload, UploadCloud } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select, SelectContent, SelectItem,
  SelectTrigger, SelectValue,
} from '@/components/ui/select';

interface Props {
  currentWorkspace: string;
  workspaces: string[];
  onUploaded: () => void;
}

declare global {
  interface Window {
    uploadcare: any;
  }
}

export default function UploadButton({ currentWorkspace, workspaces, onUploaded }: Props) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [ws, setWs] = useState(currentWorkspace);

  const handleSelect = () => fileRef.current?.click();

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setLoading(true);

    const body = new FormData();
    body.append('file', file);
    body.append('workspace', ws);

    try {
      const res = await fetch('/api/upload', { method:'POST', body });
      if (!res.ok) throw new Error('Upload failed');
      toast({ title:'Uploaded ✔︎', description:file.name });
      onUploaded();
    } catch(err:any){
      toast({ title:'Upload error', description:err.message, variant:'destructive'});
    } finally { setLoading(false); }
  };

  const handleCloudConnect = () => {
    const widget = window.uploadcare.openDialog(null, {
      tabs: 'file url gdrive dropbox onedrive',
    });
    widget.done((fileInfo: any) => {
      fetch('/api/upload/external', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          url: fileInfo.cdnUrl,
          name: fileInfo.name,
          workspace: ws,
        }),
      }).then(() => {
        toast({ title: 'Cloud file connected ✔︎', description: fileInfo.name });
        onUploaded();
      });
    });
  };

  return (
    <>
      <input type="file" hidden ref={fileRef} onChange={handleUpload}
             accept=".pdf,.doc,.docx,.txt"/>
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
          <Upload className="w-4 h-4 mr-1"/>{loading ? '…' : 'Upload'}
        </Button>

        <Button size="sm" variant="outline" onClick={handleCloudConnect}>
          <UploadCloud className="w-4 h-4 mr-1"/> Connect Cloud
        </Button>
      </div>
    </>
  );
}
