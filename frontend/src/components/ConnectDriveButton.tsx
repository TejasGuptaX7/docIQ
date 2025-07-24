// src/components/ConnectDriveButton.tsx

import { useEffect, useState } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { Button } from '@/components/ui/button';
import { Cloud, CheckCircle } from 'lucide-react';

interface Props {
  /** optional callback so the parent (Dashboard) can immediately refetch docs */
  onConnected?: () => void;
}

const API_BASE = 'https://api.dociq.tech/api';

export default function ConnectDriveButton({ onConnected }: Props) {
  const { getToken, isLoaded } = useAuth();
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);

  // 1) check if the user already has a Drive token
  const checkDriveStatus = async () => {
    setLoading(true);
    try {
      if (isLoaded) {
        const token = await getToken();
        const res = await fetch(`${API_BASE}/drive/status`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
          setConnected(await res.json());
          return;
        }
      }
      // fallback to demo (if you still support it)
      const res = await fetch(`${API_BASE}/fallback/drive/status`);
      if (res.ok) {
        setConnected(await res.json());
      }
    } catch (err) {
      console.error('[ConnectDriveButton] status error', err);
    } finally {
      setLoading(false);
    }
  };

  // 2) claim the tempKey and then trigger a full sync
  const claimToken = async (tempKey: string) => {
    try {
      const token = await getToken();
      const res = await fetch(`${API_BASE}/drive/claim?tempKey=${tempKey}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        setConnected(true);
        onConnected?.();

        // immediately kick off ingestion of all Drive files
        await fetch(`${API_BASE}/drive/sync`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` }
        });
      }
    } catch (err) {
      console.error('[ConnectDriveButton] claim error', err);
    }
  };

  // initial check + poll every 30s
  useEffect(() => {
    checkDriveStatus();
    const id = setInterval(checkDriveStatus, 30_000);
    return () => clearInterval(id);
  }, [isLoaded]);

  // handle OAuth callback params drive=connected&temp=…
  useEffect(() => {
    const url = new URL(window.location.href);
    const drive = url.searchParams.get('drive');
    const temp = url.searchParams.get('temp');
    if (drive === 'connected' && temp && isLoaded) {
      claimToken(temp);
      // clean up URL
      url.searchParams.delete('drive');
      url.searchParams.delete('temp');
      window.history.replaceState({}, '', url.toString());
    }
  }, [isLoaded]);

  if (loading) {
    return (
      <Button variant="outline" disabled>
        <Cloud className="mr-2 h-4 w-4" />
        Checking…
      </Button>
    );
  }

  return (
    <Button
      variant={connected ? 'secondary' : 'outline'}
      disabled={connected}
      onClick={() => (window.location.href = `${API_BASE}/drive/connect`)}
    >
      {connected ? (
        <>
          <CheckCircle className="mr-2 h-4 w-4 text-green-500" />
          Drive Connected
        </>
      ) : (
        <>
          <Cloud className="mr-2 h-4 w-4" />
          Connect&nbsp;Google&nbsp;Drive
        </>
      )}
    </Button>
  );
}
