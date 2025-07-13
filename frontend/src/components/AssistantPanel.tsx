import ChatInterface from "./ChatInterface";

interface Props {
  selectedDoc: string | null;
}

export default function AssistantPanel({ selectedDoc }: Props) {
  return (
    <aside className="w-[360px] border-l border-border/50 p-4 bg-card/60 backdrop-blur-sm">
      <ChatInterface selectedDoc={selectedDoc} />
    </aside>
  );
}
