import { useState } from 'react';
import { Plus } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetTrigger,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

interface Props {
  onCreate: (name: string) => void;
}

export default function WorkspaceSheet({ onCreate }: Props) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');

  const handleCreate = () => {
    if (!name.trim()) return;
    onCreate(name.trim());
    setName('');
    setOpen(false);
  };

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      {/* ⚠️  id is required so the sidebar can click this button invisibly */}
      <SheetTrigger id="ws-sheet-btn" asChild>
        <Button size="icon" variant="ghost">
          <Plus className="w-5 h-5" />
        </Button>
      </SheetTrigger>

      <SheetContent side="left" className="w-72">
        <SheetHeader>
          <SheetTitle>Create a Space</SheetTitle>
          <SheetDescription>
            Separate your documents for work, projects, and more.
          </SheetDescription>
        </SheetHeader>

        <div className="mt-6 space-y-4">
          <Input
            placeholder="Space name…"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          {/* theme picker stub could go here */}

          <Button className="w-full" onClick={handleCreate}>
            Create Space
          </Button>
          <Button
            variant="ghost"
            className="w-full"
            onClick={() => setOpen(false)}
          >
            Cancel
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
