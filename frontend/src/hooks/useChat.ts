import { useState } from "react";

export function useChat(docId: string | null) {
  const [messages, setMessages] = useState<
    { id: string; role: "user" | "ai"; content: string; sources?: any[] }[]
  >([]);
  const [loading, setLoading] = useState(false);

  async function sendMessage(input: string) {
    if (!docId || !input.trim()) return;
    setMessages((prev) => [...prev, { id: Date.now().toString(), role: "user", content: input }]);
    setLoading(true);

    const res = await fetch(`/api/search`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ query: input, docId }),
    });

    const data = await res.json();
    setMessages((prev) => [
      ...prev,
      {
        id: Date.now().toString(),
        role: "ai",
        content: data.answer || "No answer from backend.",
        sources: data.sources || [],
      },
    ]);
    setLoading(false);
  }

  return { messages, sendMessage, loading };
}
