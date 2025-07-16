import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Cloud } from 'lucide-react';

export default function ConnectDriveButton() {
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    // Optionally, you could call your backend to check if tokens exist
    fetch('/api/drive/status')
      .then(r => r.json())
      .then(data => setConnected(data.connected));
  }, []);

  return (
    <Button
      variant={connected ? 'secondary' : 'outline'}
      onClick={() => window.location.href = '/api/drive/connect'}
    >
      <Cloud className="mr-2" />
      {connected ? 'Drive Connected' : 'Connect Google Drive'}
    </Button>
  );
}
