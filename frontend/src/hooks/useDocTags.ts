// src/hooks/useDocTags.ts
import { useEffect, useState } from 'react';

/** maps docId → workspace */
export default function useDocTags() {
  const [tags, setTags] = useState<Record<string, string>>({});

  /* load once */
  useEffect(() => {
    const raw = localStorage.getItem('vectormind.tags');
    if (raw) setTags(JSON.parse(raw));
  }, []);

  /* persist whenever tags change */
  useEffect(() => {
    localStorage.setItem('vectormind.tags', JSON.stringify(tags));
  }, [tags]);

  const tagDoc = (id: string, ws: string) =>
    setTags((t) => ({ ...t, [id]: ws }));

  return { tags, tagDoc };
}
