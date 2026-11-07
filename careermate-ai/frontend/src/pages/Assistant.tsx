import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Bot, Eraser, Loader2, MessageSquarePlus, Send, Trash2 } from 'lucide-react';
import { assistantApi } from '../api';
import { errorMessage } from '../api/client';
import type { ChatMessage, Conversation } from '../api/types';
import { SimpleMarkdown } from '../components/SimpleMarkdown';
import { Alert, PageHeader } from '../components/ui';
import { timeAgo } from '../lib/format';

export const SUGGESTED = [
  'What jobs are suitable for my qualifications?',
  'How can I improve my resume?',
  'What skills should I learn for my career?',
  'Help me prepare for a software engineering interview',
  'Give me common interview questions for an accountant',
  'How should I answer salary expectation questions?',
  'Write a professional job application email',
  'Help me create a career development plan',
];

export default function Assistant() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [aiEnabled, setAiEnabled] = useState<boolean | null>(null);
  const [useProfile, setUseProfile] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bottom = useRef<HTMLDivElement>(null);

  const refresh = useCallback(() => assistantApi.conversations().then(setConversations).catch(() => undefined), []);

  useEffect(() => {
    void refresh();
    assistantApi.status().then((s) => setAiEnabled(s.aiEnabled)).catch(() => setAiEnabled(false));
  }, [refresh]);

  useEffect(() => { bottom.current?.scrollIntoView?.({ behavior: 'smooth' }); }, [messages, sending]);

  const open = async (id: number) => {
    setActiveId(id);
    setError(null);
    try {
      setMessages(await assistantApi.messages(id));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const newChat = () => { setActiveId(null); setMessages([]); setError(null); };

  const send = async (content: string) => {
    const msg = content.trim();
    if (!msg || sending) return;
    setError(null);
    setText('');
    setSending(true);
    setMessages((m) => [...m, { id: `tmp-${Date.now()}`, role: 'USER', content: msg }]);
    try {
      const res = await assistantApi.send(msg, activeId, useProfile);
      setActiveId(res.conversationId);
      setMessages((m) => [...m, { id: `a-${Date.now()}`, role: 'ASSISTANT', content: res.reply, aiPowered: res.aiPowered }]);
      void refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSending(false);
    }
  };

  const submit = (e: FormEvent) => { e.preventDefault(); void send(text); };

  const clear = async () => {
    if (activeId == null) return newChat();
    try {
      await assistantApi.remove(activeId);
      newChat();
      void refresh();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <PageHeader title="CareerMate AI Assistant" subtitle="Ask about jobs, resumes, interviews, salaries and career plans." />
      {aiEnabled === false && <div className="mb-4"><Alert kind="info">The assistant is in <strong>offline mode</strong> (no Gemini API key configured). Add your key in <strong>Settings</strong> for personalised AI answers; until then it gives general guidance.</Alert></div>}
      <div className="grid gap-4 lg:grid-cols-[16rem_1fr]">
        <aside className="card hidden max-h-[70vh] overflow-y-auto p-3 lg:block" aria-label="Conversation history">
          <button className="btn-primary mb-3 w-full" onClick={newChat}><MessageSquarePlus className="h-4 w-4" />New chat</button>
          {conversations.length === 0 && <p className="px-1 text-xs text-slate-400">No conversations yet.</p>}
          <ul className="space-y-1">
            {conversations.map((c) => (
              <li key={c.id}><button onClick={() => open(c.id)} className={`w-full rounded-xl px-3 py-2 text-left text-sm ${c.id === activeId ? 'bg-navy-50 font-medium text-navy-700' : 'hover:bg-slate-50'}`}>
                <span className="block truncate">{c.title}</span><span className="text-xs text-slate-400">{timeAgo(c.updatedAt)}</span></button></li>
            ))}
          </ul>
        </aside>

        <section className="card flex h-[70vh] flex-col" aria-label="Chat">
          <div className="flex items-center justify-between gap-2 border-b border-slate-100 px-4 py-2">
            <label className="flex items-center gap-2 text-xs text-slate-600"><input type="checkbox" checked={useProfile} onChange={(e) => setUseProfile(e.target.checked)} />Use my profile and resume for personalised advice</label>
            <div className="flex gap-1">
              <button className="btn-ghost btn-sm lg:hidden" onClick={newChat}><MessageSquarePlus className="h-4 w-4" />New</button>
              <button className="btn-ghost btn-sm" onClick={clear}>{activeId == null ? <Eraser className="h-4 w-4" /> : <Trash2 className="h-4 w-4" />}Clear conversation</button>
            </div>
          </div>
          <div className="flex-1 space-y-4 overflow-y-auto p-4" aria-live="polite">
            {messages.length === 0 && (
              <div className="mx-auto max-w-xl py-6 text-center">
                <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-50 text-accent-700"><Bot className="h-6 w-6" /></div>
                <h2 className="font-semibold">How can I help with your career today?</h2>
                <div className="mt-4 flex flex-wrap justify-center gap-2">
                  {SUGGESTED.map((s) => <button key={s} className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:border-accent-600 hover:bg-accent-50" onClick={() => send(s)}>{s}</button>)}
                </div>
              </div>
            )}
            {messages.map((m) => (
              <div key={m.id} className={`flex ${m.role === 'USER' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm ${m.role === 'USER' ? 'bg-navy-700 text-white' : 'bg-slate-100 text-slate-800'}`}>
                  {m.role === 'USER' ? <p className="whitespace-pre-wrap">{m.content}</p> : <SimpleMarkdown text={m.content} />}
                </div>
              </div>
            ))}
            {sending && <div className="flex items-center gap-2 text-sm text-slate-500" role="status"><Loader2 className="h-4 w-4 animate-spin" />CareerMate is thinking...</div>}
            <div ref={bottom} />
          </div>
          {error && <div className="px-4 pb-2"><Alert>{error}</Alert></div>}
          <form onSubmit={submit} className="flex gap-2 border-t border-slate-100 p-3">
            <textarea aria-label="Message" rows={1} className="input max-h-32 resize-none" placeholder="Type your career question..." value={text} maxLength={4000}
              onChange={(e) => setText(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void send(text); } }} />
            <button className="btn-primary" disabled={sending || !text.trim()} aria-label="Send message"><Send className="h-4 w-4" />Send</button>
          </form>
        </section>
      </div>
    </div>
  );
}
