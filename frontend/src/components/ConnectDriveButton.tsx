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
  const [error, setError] = useState<string | null>(null);

  // 1) check if the user already has a Drive token
  const checkDriveStatus = async () => {
    setLoading(true);
    setError(null);
    
    try {
      if (isLoaded) {
        const token = await getToken();
        if (token) {
          const res = await fetch(`${API_BASE}/drive/status`, {
            method: 'GET',
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            credentials: 'include'
          });
          
          if (res.ok) {
            const isConnected = await res.json();
            setConnected(isConnected);
            return;
          } else if (res.status === 401) {
            // User not authenticated, try fallback
            console.log('User not authenticated, trying fallback');
          } else {
            console.error('Drive status check failed:', res.status, res.statusText);
          }
        }
      }
      
      // fallback to demo (if you still support it)
      try {
        const res = await fetch(`${API_BASE}/fallback/drive/status`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include'
        });
        
        if (res.ok) {
          const isConnected = await res.json();
          setConnected(isConnected);
        } else {
          console.error('Fallback drive status failed:', res.status, res.statusText);
          setError('Unable to check Drive connection status');
        }
      } catch (fallbackErr) {
        console.error('Fallback request failed:', fallbackErr);
        setError('Service temporarily unavailable');
      }
      
    } catch (err) {
      console.error('[ConnectDriveButton] status error', err);
      setError('Connection error');
    } finally {
      setLoading(false);
    }
  };

  // 2) claim the tempKey and then trigger a full sync
  const claimToken = async (tempKey: string) => {
    try {
      const token = await getToken();
      if (!token) {
        console.error('No auth token available');
        return;
      }
      
      const res = await fetch(`${API_BASE}/drive/claim?tempKey=${encodeURIComponent(tempKey)}`, {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        credentials: 'include'
      });
      
      if (res.ok) {
        setConnected(true);
        onConnected?.();

        // immediately kick off ingestion of all Drive files
        try {
          await fetch(`${API_BASE}/drive/sync`, {
            method: 'POST',
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            credentials: 'include'
          });
        } catch (syncErr) {
          console.error('Drive sync failed:', syncErr);
          // Don't fail the whole flow if sync fails
        }
      } else {
        console.error('Token claim failed:', res.status, res.statusText);
      }
    } catch (err) {
      console.error('[ConnectDriveButton] claim error', err);
    }
  };

  // initial check + poll every 30s (but only if not in error state)
  useEffect(() => {
    checkDriveStatus();
    const id = setInterval(() => {
      if (!error) {
        checkDriveStatus();
      }
    }, 30_000);
    return () => clearInterval(id);
  }, [isLoaded, error]);

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

  const handleConnect = () => {
    window.location.href = `${API_BASE}/drive/connect`;
  };

  if (loading) {
    return (
      <Button variant="outline" disabled>
        <Cloud className="mr-2 h-4 w-4" />
        Checking…
      </Button>
    );
  }

  if (error) {
    return (
      <Button variant="outline" onClick={checkDriveStatus}>
        <Cloud className="mr-2 h-4 w-4" />
        Retry Connection
      </Button>
    );
  }

  return (
    <Button
      variant={connected ? 'secondary' : 'outline'}
      disabled={connected}
      onClick={handleConnect}
    >
      {connected ? (
        <>
          <CheckCircle className="mr-2 h-4 w-4 text-green-500" />
          Drive Connected
        </>
      ) : (
        <>
          <Cloud className="mr-2 h-4 w-4" />
          Connect Google Drive
        </>
      )}
    </Button>
  );
}