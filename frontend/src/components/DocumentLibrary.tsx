import { useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card   } from '@/components/ui/card';
import { Badge  } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { FileText, Clock, Zap } from 'lucide-react';
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area';

interface Props {
  onSelectDoc:(id:string)=>void;
  selectedDoc:string|null;
  noWorkspace?:boolean;
}
interface Doc {
  _additional:{ id:string };
  title:string|null;
  processed:boolean;
  pages:number|null;
  workspace?:string;
}
const fetchDocs = async():Promise<Doc[]> => {
  const r = await fetch('/api/documents');
  return r.ok ? r.json() : [];
};

export default function DocumentLibrary({onSelectDoc,selectedDoc,noWorkspace}:Props){
  const {data:docs=[],isLoading,refetch} = useQuery({
    queryKey:['documents'],
    queryFn: fetchDocs
  });
  useEffect(()=>{ (window as any).refetchDocs = refetch; },[refetch]);

  const groups = useMemo(()=>{
    if(noWorkspace) return { All:docs };
    return docs.reduce<Record<string,Doc[]>>((acc,d)=>{
      const w = d.workspace || 'default';
      (acc[w] ??= []).push(d);
      return acc;
    },{});
  },[docs,noWorkspace]);

  if(isLoading) return <Skeleton className="h-20"/>;
  if(!docs.length)  return <p className="text-sm text-muted-foreground">No documents.</p>;

  return (
    <div className="pr-1">
      {Object.entries(groups).map(([ws,list])=>(
        <div key={ws} className="mb-6">
          <p className="px-2 py-1 font-semibold text-primary">{ws}</p>
          {list.map(doc=>{
            const disabled = !doc.processed;
            return (
              <Card key={doc._additional.id}
                    onClick={()=>!disabled && onSelectDoc(doc._additional.id)}
                    className={`p-3 mb-2 cursor-pointer transition ${
                      disabled
                        ? 'opacity-50 cursor-not-allowed'
                        : selectedDoc===doc._additional.id
                          ? 'bg-primary/10 border-primary'
                          : 'hover:bg-card/70'
                    }`}>
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded bg-gradient-to-r from-purple-500 to-pink-500 flex justify-center items-center">
                    <FileText className="w-4 h-4 text-white"/>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm truncate">{doc.title||'Untitled'}</p>
                    <p className="text-xs text-muted-foreground">{doc.pages??'?'} pg</p>
                  </div>
                  <Badge variant="secondary" className="text-xs">
                    {doc.processed
                      ? <><Zap className="w-3 h-3 mr-1"/>Ready</>
                      : <><Clock className="w-3 h-3 mr-1"/>Processing</>}
                  </Badge>
                </div>
              </Card>
            );
          })}
        </div>
      ))}
    </div>
  );
}
