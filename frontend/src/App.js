import React, { useState, useEffect, useRef } from "react";

const API = "http://localhost:8080";

// ─── Helpers ─────────────────────────────────────────────────────────────────
const SC = {
  OPEN: { bg: "#dcfce7", color: "#15803d" },
  RESOLVED: { bg: "#dbeafe", color: "#1d4ed8" },
  NEEDS_HUMAN: { bg: "#fee2e2", color: "#b91c1c" },
};

function Badge({ status }) {
  const s = SC[status] || SC.OPEN;
  return (
    <span style={{ fontSize: 11, fontWeight: 700, color: s.color, background: s.bg,
      padding: "2px 9px", borderRadius: 12 }}>
      {status?.replace("_", " ")}
    </span>
  );
}

function Bubble({ msg }) {
  const isUser = msg.sender === "USER";
  const isAdmin = msg.sender === "ADMIN";
  return (
    <div style={{ display: "flex", flexDirection: "column",
      alignItems: isUser ? "flex-end" : "flex-start", margin: "5px 0" }}>
      {!isUser && (
        <span style={{ fontSize: 10, color: "#94a3b8", marginBottom: 2 }}>
          {isAdmin ? "🛡 Admin" : "🤖 AI"}
        </span>
      )}
      <div style={{
        maxWidth: "75%", padding: "9px 13px", fontSize: 13.5, lineHeight: 1.6,
        borderRadius: isUser ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
        background: isUser ? "#2563eb" : isAdmin ? "#7c3aed" : "#f1f5f9",
        color: isUser || isAdmin ? "#fff" : "#1e293b",
        whiteSpace: "pre-line",
      }}>{msg.message || msg.text}</div>
    </div>
  );
}

// ─── Auth Screen ──────────────────────────────────────────────────────────────
function AuthScreen({ onLogin }) {
  const [mode, setMode] = useState("login"); // login | signup
  const [form, setForm] = useState({ name: "", email: "", password: "", phone: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const submit = async () => {
    setError(""); setLoading(true);
    try {
      const endpoint = mode === "login" ? "/auth/login" : "/auth/signup";
      const body = mode === "login"
        ? { email: form.email, password: form.password }
        : { name: form.name, email: form.email, password: form.password, phone: form.phone };

      const r = await fetch(API + endpoint, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await r.json();
      if (!r.ok) { setError(data.error || "Something went wrong"); return; }

      // Seed sample orders on first signup so demo works
      if (mode === "signup") {
        await fetch(`${API}/orders/seed/${data.userId}`, { method: "POST" });
      }
      onLogin(data);
    } catch { setError("Cannot connect to server. Is Spring Boot running?"); }
    finally { setLoading(false); }
  };

  const inp = (placeholder, key, type = "text") => (
    <input type={type} placeholder={placeholder} value={form[key]}
      onChange={e => set(key, e.target.value)}
      onKeyDown={e => e.key === "Enter" && submit()}
      style={{ width: "100%", padding: "11px 14px", marginBottom: 10, fontSize: 14,
        border: "1.5px solid #e2e8f0", borderRadius: 10, outline: "none",
        fontFamily: "inherit", background: "#f8fafc" }} />
  );

  return (
    <div style={{ height: "100vh", display: "flex", alignItems: "center",
      justifyContent: "center", background: "linear-gradient(135deg,#1e3a8a,#2563eb,#3b82f6)" }}>
      <div style={{ background: "#fff", borderRadius: 20, padding: "36px 32px",
        width: 360, boxShadow: "0 20px 60px rgba(0,0,0,0.2)" }}>

        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div style={{ fontSize: 36, marginBottom: 6 }}>🧠</div>
          <div style={{ fontSize: 22, fontWeight: 800, color: "#1e293b" }}>MindX Support</div>
          <div style={{ fontSize: 13, color: "#64748b", marginTop: 4 }}>
            {mode === "login" ? "Sign in to your account" : "Create your account"}
          </div>
        </div>

        {mode === "signup" && inp("Full Name", "name")}
        {inp("Email address", "email", "email")}
        {inp("Password", "password", "password")}
        {mode === "signup" && inp("Phone (optional)", "phone", "tel")}

        {error && (
          <div style={{ background: "#fef2f2", color: "#b91c1c", fontSize: 12,
            padding: "8px 12px", borderRadius: 8, marginBottom: 12 }}>⚠️ {error}</div>
        )}

        <button onClick={submit} disabled={loading} style={{
          width: "100%", background: "linear-gradient(135deg,#1e40af,#2563eb)",
          color: "#fff", border: "none", borderRadius: 10, padding: "12px",
          fontSize: 14, fontWeight: 700, cursor: "pointer", marginBottom: 14 }}>
          {loading ? "Please wait..." : mode === "login" ? "Sign In" : "Create Account"}
        </button>

        <div style={{ textAlign: "center", fontSize: 13, color: "#64748b" }}>
          {mode === "login" ? "Don't have an account? " : "Already have an account? "}
          <span onClick={() => { setMode(mode === "login" ? "signup" : "login"); setError(""); }}
            style={{ color: "#2563eb", fontWeight: 700, cursor: "pointer" }}>
            {mode === "login" ? "Sign Up" : "Sign In"}
          </span>
        </div>
      </div>
    </div>
  );
}

// ─── Customer Chat ────────────────────────────────────────────────────────────
function Chat({ user }) {
  const [chat, setChat] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [ticketId, setTicketId] = useState(null);
  const [escalated, setEscalated] = useState(false);
  const [orders, setOrders] = useState([]);
  const bottom = useRef(null);

  useEffect(() => { bottom.current?.scrollIntoView({ behavior: "smooth" }); }, [chat, loading]);

  useEffect(() => {
    // Load user orders on mount
    fetch(`${API}/orders/user/${user.userId}`)
      .then(r => r.json()).then(setOrders).catch(() => {});
  }, [user.userId]);

  const send = async (q = input.trim()) => {
    if (!q || loading) return;
    setInput(""); setLoading(true);
    setChat(p => [...p, { sender: "USER", message: q }]);
    try {
      let data;
      const body = { query: q, userId: user.userId };
      if (!ticketId) {
        const r = await fetch(`${API}/tickets`, { method: "POST",
          headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
        data = await r.json();
        setTicketId(data.ticketId); setEscalated(data.escalated);
      } else {
        const r = await fetch(`${API}/tickets/${ticketId}/messages`, { method: "POST",
          headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
        data = await r.json();
        if (data.escalated) setEscalated(true);
      }
      setChat(p => [...p, { sender: "AI", message: data.aiResponse }]);
    } catch {
      setChat(p => [...p, { sender: "AI", message: "Sorry, something went wrong. Please try again." }]);
    } finally { setLoading(false); }
  };

  const chips = ["Where is my order?", "Track ORD-1001", "I want a refund", "Payment issue", "My orders"];

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Header */}
      <div style={{ background: "linear-gradient(135deg,#1e40af,#2563eb)", padding: "12px 18px",
        display: "flex", alignItems: "center", gap: 10 }}>
        <span style={{ fontSize: 22 }}>🤖</span>
        <div style={{ flex: 1 }}>
          <div style={{ color: "#fff", fontWeight: 700, fontSize: 14 }}>MindX Support</div>
          <div style={{ color: "#bfdbfe", fontSize: 11 }}>● Always online · Hi, {user.name}!</div>
        </div>
        {ticketId && (
          <span style={{ fontSize: 11, color: "#bfdbfe", background: "rgba(255,255,255,0.15)",
            padding: "3px 8px", borderRadius: 8 }}>Ticket #{ticketId}</span>
        )}
        {ticketId && (
          <button onClick={() => { setChat([]); setTicketId(null); setEscalated(false); }}
            style={{ background: "rgba(255,255,255,0.15)", border: "none", color: "#fff",
              padding: "4px 10px", borderRadius: 8, fontSize: 12, cursor: "pointer" }}>
            New Chat
          </button>
        )}
      </div>

      {/* Escalation banner */}
      {escalated && (
        <div style={{ background: "#fef2f2", color: "#b91c1c", fontSize: 12,
          padding: "7px 16px", borderBottom: "1px solid #fecaca" }}>
          ⚠️ Your ticket has been escalated to a human agent. You'll be contacted soon.
        </div>
      )}

      {/* Orders bar */}
      {orders.length > 0 && !ticketId && (
        <div style={{ padding: "8px 14px", borderBottom: "1px solid #f1f5f9",
          background: "#fafafa", display: "flex", gap: 8, overflowX: "auto" }}>
          <span style={{ fontSize: 11, color: "#94a3b8", whiteSpace: "nowrap", alignSelf: "center" }}>
            Your orders:
          </span>
          {orders.map(o => (
            <button key={o.orderId} onClick={() => send(`Track my order ${o.orderId}`)} style={{
              fontSize: 11, padding: "4px 10px", borderRadius: 12, cursor: "pointer",
              border: "1px solid #e2e8f0", background: "#fff", color: "#374151",
              whiteSpace: "nowrap",
            }}>
              {o.orderId} · {o.status}
            </button>
          ))}
        </div>
      )}

      {/* Messages */}
      <div style={{ flex: 1, overflowY: "auto", padding: "14px 16px" }}>
        {chat.length === 0 && (
          <div style={{ textAlign: "center", padding: "30px 0", color: "#94a3b8" }}>
            <div style={{ fontSize: 38, marginBottom: 8 }}>💬</div>
            <div style={{ fontWeight: 600, color: "#64748b", fontSize: 15, marginBottom: 4 }}>
              How can we help you, {user.name}?
            </div>
            <div style={{ fontSize: 12, marginBottom: 16 }}>Ask about orders, refunds, shipping and more.</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 7, justifyContent: "center" }}>
              {chips.map(s => (
                <button key={s} onClick={() => send(s)} style={{
                  background: "#f1f5f9", border: "1px solid #e2e8f0", color: "#475569",
                  padding: "6px 13px", borderRadius: 20, fontSize: 12, cursor: "pointer" }}>{s}</button>
              ))}
            </div>
          </div>
        )}
        {chat.map((m, i) => <Bubble key={i} msg={m} />)}
        {loading && (
          <div style={{ display: "flex", alignItems: "center", gap: 6, color: "#94a3b8", fontSize: 12, padding: "4px 0" }}>
            <span>🤖</span>
            <span style={{ animation: "pulse 1s infinite" }}>AI is typing...</span>
          </div>
        )}
        <div ref={bottom} />
      </div>

      {/* Input */}
      <div style={{ padding: "10px 14px", borderTop: "1px solid #e2e8f0", display: "flex", gap: 8 }}>
        <input value={input} onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === "Enter" && send()}
          placeholder="Type your message... (e.g. Track ORD-1001)"
          style={{ flex: 1, border: "1.5px solid #e2e8f0", borderRadius: 10,
            padding: "9px 13px", fontSize: 13, outline: "none", fontFamily: "inherit" }} />
        <button onClick={() => send()} disabled={!input.trim() || loading} style={{
          background: !input.trim() || loading ? "#cbd5e1" : "#2563eb",
          color: "#fff", border: "none", borderRadius: 10,
          width: 40, height: 40, fontSize: 17, cursor: "pointer" }}>➤</button>
      </div>
    </div>
  );
}

// ─── Admin Dashboard ──────────────────────────────────────────────────────────
function Admin() {
  const [tickets, setTickets] = useState([]);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [adminMsg, setAdminMsg] = useState("");
  const [filter, setFilter] = useState("ALL");

  const load = async () => {
    const [t, a] = await Promise.all([fetch(`${API}/tickets`), fetch(`${API}/tickets/analytics`)]);
    setTickets(await t.json()); setAnalytics(await a.json());
  };

  useEffect(() => { load(); }, []);

  const pick = async (t) => {
    setSelected(t);
    const r = await fetch(`${API}/tickets/${t.id}`);
    const d = await r.json();
    setMessages(d.messages);
  };

  const updateStatus = async (id, status) => {
    await fetch(`${API}/tickets/${id}/status`, { method: "PATCH",
      headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status }) });
    load(); setSelected(p => p?.id === id ? { ...p, status } : p);
  };

  const sendAdmin = async () => {
    if (!adminMsg.trim() || !selected) return;
    await fetch(`${API}/tickets/${selected.id}/admin-message`, { method: "POST",
      headers: { "Content-Type": "application/json" }, body: JSON.stringify({ message: adminMsg }) });
    setAdminMsg("");
    const r = await fetch(`${API}/tickets/${selected.id}`);
    setMessages((await r.json()).messages);
  };

  const filtered = tickets.filter(t => filter === "ALL" || t.status === filter);

  return (
    <div style={{ display: "flex", height: "100%", background: "#f8fafc" }}>
      {/* Sidebar */}
      <div style={{ width: 290, minWidth: 290, borderRight: "1px solid #e2e8f0",
        display: "flex", flexDirection: "column", background: "#fff" }}>

        <div style={{ padding: "14px 16px", background: "linear-gradient(135deg,#1e1b4b,#312e81)" }}>
          <div style={{ color: "#fff", fontWeight: 700, fontSize: 14 }}>🛡️ Admin Panel</div>
          <div style={{ color: "#a5b4fc", fontSize: 11 }}>Ticket Management</div>
        </div>

        {/* Analytics */}
        {analytics && (
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6,
            padding: "10px 12px", borderBottom: "1px solid #e2e8f0" }}>
            {[["Total", analytics.total, "#475569"], ["Open", analytics.open, "#16a34a"],
              ["Resolved", analytics.resolved, "#2563eb"], ["Escalated", analytics.needsHuman, "#dc2626"]
            ].map(([l, v, c]) => (
              <div key={l} style={{ background: "#f8fafc", border: "1px solid #e2e8f0",
                borderRadius: 8, padding: "7px 10px" }}>
                <div style={{ fontSize: 20, fontWeight: 800, color: c }}>{v}</div>
                <div style={{ fontSize: 10, color: "#94a3b8", fontWeight: 600 }}>{l}</div>
              </div>
            ))}
          </div>
        )}

        {/* Filter */}
        <div style={{ display: "flex", gap: 4, padding: "8px 12px",
          borderBottom: "1px solid #e2e8f0", flexWrap: "wrap" }}>
          {["ALL", "OPEN", "RESOLVED", "NEEDS_HUMAN"].map(s => (
            <button key={s} onClick={() => setFilter(s)} style={{
              fontSize: 10, padding: "3px 8px", borderRadius: 10, cursor: "pointer", fontWeight: 600,
              border: "1px solid", background: filter === s ? "#2563eb" : "#fff",
              color: filter === s ? "#fff" : "#64748b",
              borderColor: filter === s ? "#2563eb" : "#e2e8f0" }}>
              {s.replace("_", " ")}
            </button>
          ))}
        </div>

        {/* Ticket list */}
        <div style={{ flex: 1, overflowY: "auto" }}>
          {filtered.map(t => (
            <div key={t.id} onClick={() => pick(t)} style={{
              padding: "10px 14px", cursor: "pointer", borderBottom: "1px solid #f1f5f9",
              background: selected?.id === t.id ? "#eff6ff" : "#fff",
              borderLeft: selected?.id === t.id ? "3px solid #2563eb" : "3px solid transparent" }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 3 }}>
                <span style={{ fontSize: 12, fontWeight: 700, color: "#2563eb" }}>#{t.id}</span>
                <Badge status={t.status} />
              </div>
              <div style={{ fontSize: 12, color: "#374151", overflow: "hidden",
                whiteSpace: "nowrap", textOverflow: "ellipsis" }}>{t.query}</div>
              {t.userId && <div style={{ fontSize: 10, color: "#94a3b8", marginTop: 2 }}>User #{t.userId}</div>}
            </div>
          ))}
        </div>

        <button onClick={load} style={{ margin: 10, background: "#f1f5f9",
          border: "1px solid #e2e8f0", borderRadius: 8, padding: 8,
          fontSize: 12, cursor: "pointer", color: "#475569" }}>🔄 Refresh</button>
      </div>

      {/* Detail */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
        {!selected ? (
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <div style={{ textAlign: "center", color: "#94a3b8" }}>
              <div style={{ fontSize: 40 }}>📋</div>
              <div style={{ marginTop: 8, color: "#64748b" }}>Select a ticket to view</div>
            </div>
          </div>
        ) : (
          <>
            <div style={{ padding: "12px 18px", borderBottom: "1px solid #e2e8f0",
              background: "#fff", display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
              <div>
                <div style={{ fontWeight: 700, color: "#1e293b", fontSize: 15 }}>Ticket #{selected.id}</div>
                <div style={{ fontSize: 12, color: "#64748b" }}>{selected.query}</div>
              </div>
              <div style={{ marginLeft: "auto", display: "flex", gap: 6, flexWrap: "wrap", alignItems: "center" }}>
                <Badge status={selected.status} />
                {["OPEN", "RESOLVED", "NEEDS_HUMAN"].map(s => (
                  <button key={s} onClick={() => updateStatus(selected.id, s)}
                    disabled={selected.status === s} style={{
                      fontSize: 11, padding: "4px 10px", borderRadius: 8, cursor: "pointer",
                      border: "1px solid #e2e8f0",
                      background: selected.status === s ? "#f1f5f9" : "#fff",
                      color: selected.status === s ? "#94a3b8" : "#374151",
                      opacity: selected.status === s ? 0.5 : 1 }}>
                    {s.replace("_", " ")}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ flex: 1, overflowY: "auto", padding: "14px 20px" }}>
              {messages.map((m, i) => <Bubble key={i} msg={m} />)}
            </div>

            <div style={{ padding: "10px 18px", borderTop: "1px solid #e2e8f0", background: "#f8fafc" }}>
              <div style={{ fontSize: 11, color: "#64748b", marginBottom: 5, fontWeight: 600 }}>
                🛡️ Reply as Admin
              </div>
              <div style={{ display: "flex", gap: 8 }}>
                <input value={adminMsg} onChange={e => setAdminMsg(e.target.value)}
                  onKeyDown={e => e.key === "Enter" && sendAdmin()}
                  placeholder="Type your response to the customer..."
                  style={{ flex: 1, border: "1.5px solid #e2e8f0", borderRadius: 10,
                    padding: "8px 13px", fontSize: 13, outline: "none", fontFamily: "inherit" }} />
                <button onClick={sendAdmin} disabled={!adminMsg.trim()} style={{
                  background: "linear-gradient(135deg,#1e1b4b,#312e81)", color: "#fff",
                  border: "none", borderRadius: 10, padding: "8px 16px",
                  fontSize: 13, fontWeight: 600, cursor: "pointer" }}>Send</button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ─── App Shell ────────────────────────────────────────────────────────────────
export default function App() {
  const [user, setUser] = useState(() => {
    // Persist login across refresh
    const saved = localStorage.getItem("mindx_user");
    return saved ? JSON.parse(saved) : null;
  });
  const [view, setView] = useState("chat");

  const login = (data) => {
    localStorage.setItem("mindx_user", JSON.stringify(data));
    setUser(data);
  };

  const logout = () => {
    localStorage.removeItem("mindx_user");
    setUser(null);
  };

  if (!user) return <AuthScreen onLogin={login} />;

  return (
    <>
      <style>{`
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #e2e8f0; }
        ::-webkit-scrollbar { width: 4px; }
        ::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
        button:hover:not(:disabled) { opacity: 0.85; }
        input:focus { border-color: #2563eb !important; box-shadow: 0 0 0 2px rgba(37,99,235,0.1) !important; }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
      `}</style>
      <div style={{ height: "100vh", maxWidth: 1100, margin: "0 auto",
        display: "flex", flexDirection: "column", background: "#fff",
        boxShadow: "0 0 40px rgba(0,0,0,0.12)" }}>

        {/* Nav */}
        <div style={{ display: "flex", alignItems: "center", borderBottom: "1px solid #e2e8f0", padding: "0 16px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6, paddingRight: 20,
            borderRight: "1px solid #e2e8f0" }}>
            <span>🧠</span>
            <span style={{ fontWeight: 800, fontSize: 14, color: "#1e293b" }}>MindX</span>
          </div>
          {[["chat", "💬 Chat"], ["admin", "🛡️ Admin"]].map(([id, label]) => (
            <button key={id} onClick={() => setView(id)} style={{
              padding: "13px 18px", fontSize: 13, fontWeight: 600, background: "none",
              border: "none", cursor: "pointer", color: view === id ? "#2563eb" : "#64748b",
              borderBottom: view === id ? "2px solid #2563eb" : "2px solid transparent" }}>
              {label}
            </button>
          ))}
          <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ fontSize: 12, color: "#64748b" }}>👤 {user.name}</span>
            <button onClick={logout} style={{ fontSize: 11, color: "#ef4444", background: "#fef2f2",
              border: "1px solid #fecaca", borderRadius: 8, padding: "4px 10px", cursor: "pointer" }}>
              Logout
            </button>
          </div>
        </div>

        <div style={{ flex: 1, overflow: "hidden" }}>
          {view === "chat" ? <Chat user={user} /> : <Admin />}
        </div>
      </div>
    </>
  );
}