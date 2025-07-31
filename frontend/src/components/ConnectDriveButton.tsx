// src/components/ConnectDriveButton.tsx

import { useEffect, useState } from 'react';
import { useAuth } from '@clerk/clerk-react';
import { Button } from '@/components/ui/button';
import { Cloud, CheckCircle, RefreshCw, Unlink } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from '@/components/ui/use-toast';

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
  const [syncing, setSyncing] = useState(false);

  // Check if the user already has a Drive token
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
      
      // Fallback to demo (if you still support it)
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

  // Disconnect from Google Drive
  const disconnectDrive = async () => {
    try {
      const token = await getToken();
      if (!token) {
        toast({
          title: 'Error',
          description: 'Authentication required',
          variant: 'destructive',
        });
        return;
      }

      // Clear the token from localStorage (if stored)
      localStorage.removeItem('googleAccessToken');
      
      // You might want to add an API endpoint to clear the token server-side
      // For now, we'll just update the local state
      setConnected(false);
      
      toast({
        title: 'Disconnected',
        description: 'Google Drive has been disconnected',
      });
      
      // Refresh the status
      await checkDriveStatus();
      
    } catch (err) {
      console.error('[ConnectDriveButton] disconnect error', err);
      toast({
        title: 'Error',
        description: 'Failed to disconnect from Google Drive',
        variant: 'destructive',
      });
    }
  };

  // Sync/refresh Google Drive files
  const syncDrive = async () => {
    try {
      setSyncing(true);
      const token = await getToken();
      if (!token) {
        toast({
          title: 'Error',
          description: 'Authentication required',
          variant: 'destructive',
        });
        return;
      }

      const res = await fetch(`${API_BASE}/drive/sync`, {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        credentials: 'include'
      });

      if (res.ok) {
        toast({
          title: 'Syncing',
          description: 'Google Drive files are being synced...',
        });
        
        // Trigger the callback after a delay to allow sync to process
        setTimeout(() => {
          onConnected?.();
        }, 2000);
      } else {
        throw new Error('Sync failed');
      }
      
    } catch (err) {
      console.error('[ConnectDriveButton] sync error', err);
      toast({
        title: 'Error',
        description: 'Failed to sync Google Drive files',
        variant: 'destructive',
      });
    } finally {
      setSyncing(false);
    }
  };

  // Claim the tempKey and then trigger a full sync
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

        // Immediately kick off ingestion of all Drive files
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

  // Initial check + poll every 30s (but only if not in error state)
  useEffect(() => {
    checkDriveStatus();
    const id = setInterval(() => {
      if (!error) {
        checkDriveStatus();
      }
    }, 30_000);
    return () => clearInterval(id);
  }, [isLoaded, error]);

  // Handle OAuth callback params drive=connected&temp=…
  useEffect(() => {
    const url = new URL(window.location.href);
    const drive = url.searchParams.get('drive');
    const temp = url.searchParams.get('temp');
    if (drive === 'connected' && temp && isLoaded) {
      claimToken(temp);
      // Clean up URL
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
      <Button variant="outline" disabled size="sm">
        <Cloud className="mr-2 h-4 w-4 animate-pulse" />
        Checking…
      </Button>
    );
  }

  if (error) {
    return (
      <Button variant="outline" onClick={checkDriveStatus} size="sm">
        <Cloud className="mr-2 h-4 w-4" />
        Retry
      </Button>
    );
  }

  if (connected) {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="secondary" size="sm" className="font-mono">
            <CheckCircle className="mr-2 h-4 w-4 text-green-500" />
            Drive Connected
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={syncDrive} disabled={syncing}>
            <RefreshCw className={`mr-2 h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
            {syncing ? 'Syncing...' : 'Sync Files'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={disconnectDrive} className="text-destructive">
            <Unlink className="mr-2 h-4 w-4" />
            Disconnect
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={handleConnect}
      className="font-mono"
    >
      <Cloud className="mr-2 h-4 w-4" />
      Connect Drive
    </Button>
  );
}