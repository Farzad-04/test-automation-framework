import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

function storedUser() {
  try {
    return JSON.parse(localStorage.getItem('bankingUser') || 'null');
  } catch {
    return null;
  }
}

function money(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(amount) || 0);
}

function safeDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Date unavailable'
    : new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(date);
}

function Brand({ light = false, mobile = false }) {
  return (
    <a className={`brand${light ? ' brand-light' : ''}${mobile ? ' mobile-brand' : ''}`} href="/" aria-label="Northstar Bank home">
      <span className="brand-mark">N</span>
      <span>NORTHSTAR <small>BANK</small></span>
    </a>
  );
}

function AuthPanel({ onAuthenticated, notify }) {
  const [tab, setTab] = useState('login');
  const [busy, setBusy] = useState(false);
  const [values, setValues] = useState({
    firstName: '', lastName: '', email: '', password: ''
  });

  function update(event) {
    setValues(current => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    const path = tab === 'login' ? '/api/auth/login' : '/api/auth/register';
    const body = tab === 'login'
      ? { email: values.email.trim(), password: values.password }
      : {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        password: values.password
      };
    try {
      const response = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.error || payload.message || 'Unable to sign in.');
      if (!payload.token) throw new Error('The server did not return a sign-in token.');
      setValues({ firstName: '', lastName: '', email: '', password: '' });
      onAuthenticated(payload);
      notify(tab === 'login' ? 'Welcome back.' : 'Your account is ready.');
    } catch (error) {
      notify(error.message || 'Unable to sign in.', true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section id="authPanel" className="auth-screen">
      <div className="auth-story">
        <Brand light />
        <div className="story-copy">
          <p className="eyebrow">Banking, with a little more clarity</p>
          <h1>Your money.<br />Moving forward.</h1>
          <p>One thoughtful place to keep track of your accounts and move money with confidence.</p>
        </div>
        <div className="story-foot"><span className="status-dot" /> Secure access to your finances</div>
      </div>
      <div className="auth-side">
        <div className="auth-card">
          <Brand mobile />
          <div className="tabs" role="tablist" aria-label="Account access">
            <button className={`tab${tab === 'login' ? ' active' : ''}`} type="button" role="tab" aria-selected={tab === 'login'} onClick={() => setTab('login')}>Sign in</button>
            <button className={`tab${tab === 'register' ? ' active' : ''}`} type="button" role="tab" aria-selected={tab === 'register'} data-tab="register" onClick={() => setTab('register')}>Create account</button>
          </div>
          <form id={tab === 'login' ? 'loginForm' : 'registerForm'} className="auth-form active" onSubmit={submit}>
            <div className="form-heading">
              <p className="eyebrow">{tab === 'login' ? 'Welcome back' : 'A fresh start'}</p>
              <h2>{tab === 'login' ? 'Sign in to your account' : 'Open your account'}</h2>
              <p>{tab === 'login' ? 'Your finances are right where you left them.' : 'It only takes a moment to get started.'}</p>
            </div>
            {tab === 'register' && (
              <div className="two-col">
                <div><label htmlFor="registerFirstName">First name</label><input id="registerFirstName" name="firstName" autoComplete="given-name" placeholder="First name" value={values.firstName} onChange={update} required /></div>
                <div><label htmlFor="registerLastName">Last name</label><input id="registerLastName" name="lastName" autoComplete="family-name" placeholder="Last name" value={values.lastName} onChange={update} required /></div>
              </div>
            )}
            <label htmlFor={tab === 'login' ? 'loginEmail' : 'registerEmail'}>Email address</label>
            <input id={tab === 'login' ? 'loginEmail' : 'registerEmail'} name="email" type="email" autoComplete="email" placeholder="you@example.com" value={values.email} onChange={update} required />
            <label htmlFor={tab === 'login' ? 'loginPassword' : 'registerPassword'}>Password</label>
            <input id={tab === 'login' ? 'loginPassword' : 'registerPassword'} name="password" type="password" autoComplete={tab === 'login' ? 'current-password' : 'new-password'} minLength={tab === 'register' ? 8 : undefined} placeholder={tab === 'login' ? 'Enter your password' : 'At least 8 characters'} value={values.password} onChange={update} required />
            <button className="button button-primary button-full" type="submit" disabled={busy}>
              {busy ? 'Please wait…' : tab === 'login' ? 'Sign in' : 'Create account'} <span aria-hidden="true">→</span>
            </button>
          </form>
          <p className="auth-note">Your information is protected with secure sign-in.</p>
        </div>
      </div>
    </section>
  );
}

function Sidebar({ user, onLogout }) {
  return (
    <aside className="sidebar">
      <Brand light />
      <p className="side-label">YOUR MONEY</p>
      <a className="side-link active" href="#overview"><span className="side-icon">⌂</span> Overview</a>
      <a className="side-link" href="#accounts"><span className="side-icon">▤</span> Accounts</a>
      <a className="side-link" href="#activity"><span className="side-icon">↗</span> Activity</a>
      <div className="sidebar-bottom">
        <div className="help-card"><span className="help-icon">?</span><strong>Here for you</strong><p>This demo keeps your banking tools in one simple place.</p></div>
        <div className="profile">
          <div id="userAvatar" className="avatar">{(user.firstName || 'N').charAt(0).toUpperCase()}</div>
          <div className="profile-copy"><strong id="userLabel">{[user.firstName, user.lastName].filter(Boolean).join(' ') || 'Northstar member'}</strong><span>Personal banking</span></div>
          <button id="logoutBtn" className="icon-button" type="button" aria-label="Sign out" title="Sign out" onClick={onLogout}>↗</button>
        </div>
      </div>
    </aside>
  );
}

function AccountCard({ account }) {
  const type = account.accountType || 'Account';
  const number = account.accountNumber || '—';
  return (
    <article className="account-card">
      <div className={`account-icon${type.toUpperCase() === 'CHECKING' ? '' : ' savings'}`} aria-hidden="true">{type.toUpperCase() === 'CHECKING' ? '▤' : '◈'}</div>
      <div className="account-details">
        <strong>{type}</strong>
        <span>•••• {number.slice(-4)} · ID {account.accountId}</span>
        <span className="status-badge">{account.isActive ? 'Active' : 'Inactive'}</span>
      </div>
      <strong className="account-balance">{money(account.balance)}</strong>
    </article>
  );
}

function ActionCard({ accounts, onAccountCreated, onTransfer, notify }) {
  const [accountType, setAccountType] = useState('CHECKING');
  const [initialBalance, setInitialBalance] = useState('0');
  const [fromAccountId, setFromAccountId] = useState('');
  const [toAccountId, setToAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (!accounts.some(account => String(account.accountId) === fromAccountId)) setFromAccountId('');
    if (!accounts.some(account => String(account.accountId) === toAccountId)) setToAccountId('');
  }, [accounts, fromAccountId, toAccountId]);

  async function createAccount(event) {
    event.preventDefault();
    try {
      const response = await fetch('/api/accounts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('bankingToken')}` },
        body: JSON.stringify({ accountType, initialBalance: Number(initialBalance || 0) })
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.error || payload.message || 'Unable to create account.');
      setAccountType('CHECKING');
      setInitialBalance('0');
      await onAccountCreated();
      notify('Your new account is ready.');
    } catch (error) {
      notify(error.message || 'Unable to create account.', true);
    }
  }

  async function transfer(event) {
    event.preventDefault();
    if (!fromAccountId || !toAccountId || fromAccountId === toAccountId) {
      notify('Choose two different accounts for this transfer.', true);
      return;
    }
    try {
      const response = await fetch('/api/transactions/transfer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('bankingToken')}` },
        body: JSON.stringify({
          fromAccountId: Number(fromAccountId),
          toAccountId: Number(toAccountId),
          amount: Number(amount),
          description: description.trim() || 'Transfer'
        })
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.error || payload.message || 'Transfer failed.');
      setAmount('');
      setDescription('');
      await onTransfer();
      notify('Transfer complete.');
    } catch (error) {
      notify(error.message || 'Transfer failed.', true);
    }
  }

  function accountOptions() {
    return accounts.map(account => (
      <option key={account.accountId} value={account.accountId}>
        {account.accountType} ·•••• {(account.accountNumber || '').slice(-4)}
      </option>
    ));
  }

  return (
    <div className="action-column">
      <form id="transferForm" className="surface action-card" onSubmit={transfer}>
        <div className="section-heading compact"><div><p className="eyebrow">Move money</p><h2>Make a transfer</h2></div><span className="action-icon">↗</span></div>
        <label htmlFor="fromAccountId">From account</label>
        <select id="fromAccountId" value={fromAccountId} onChange={event => setFromAccountId(event.target.value)} required disabled={accounts.length < 2}><option value="">Choose an account</option>{accountOptions()}</select>
        <label htmlFor="toAccountId">To account</label>
        <select id="toAccountId" value={toAccountId} onChange={event => setToAccountId(event.target.value)} required disabled={accounts.length < 2}><option value="">Choose an account</option>{accountOptions()}</select>
        <label htmlFor="transferAmount">Amount</label>
        <div className="amount-input"><span>$</span><input id="transferAmount" type="number" min="0.01" step="0.01" placeholder="0.00" value={amount} onChange={event => setAmount(event.target.value)} required /></div>
        <label htmlFor="transferDescription">Note <span className="optional">(optional)</span></label>
        <input id="transferDescription" type="text" maxLength="120" placeholder="What’s this for?" value={description} onChange={event => setDescription(event.target.value)} />
        <button className="button button-primary button-full" type="submit" disabled={accounts.length < 2}>Review transfer <span aria-hidden="true">→</span></button>
      </form>
      <form id="accountForm" className="surface action-card open-account-card" onSubmit={createAccount}>
        <div className="section-heading compact"><div><p className="eyebrow">Make room to grow</p><h2>Open an account</h2></div><span className="action-icon soft">＋</span></div>
        <label htmlFor="accountType">Account type</label>
        <select id="accountType" value={accountType} onChange={event => setAccountType(event.target.value)}>
          <option value="CHECKING">Checking</option><option value="SAVINGS">Savings</option><option value="MONEY_MARKET">Money market</option>
        </select>
        <label htmlFor="initialBalance">Starting balance</label>
        <div className="amount-input"><span>$</span><input id="initialBalance" type="number" min="0" step="0.01" value={initialBalance} onChange={event => setInitialBalance(event.target.value)} required /></div>
        <button className="button button-dark button-full" type="submit">Create account <span aria-hidden="true">→</span></button>
      </form>
    </div>
  );
}

function Dashboard({ user, onLogout, notify }) {
  const [accounts, setAccounts] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const total = useMemo(() => accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0), [accounts]);

  async function loadAccounts() {
    const response = await fetch('/api/accounts', {
      headers: { Authorization: `Bearer ${localStorage.getItem('bankingToken')}` }
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || payload.message || 'Unable to load accounts.');
    const nextAccounts = Array.isArray(payload) ? payload : [];
    setAccounts(nextAccounts);
    const histories = await Promise.all(nextAccounts.map(async account => {
      const historyResponse = await fetch(`/api/accounts/${encodeURIComponent(account.accountId)}/transactions`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('bankingToken')}` }
      });
      const historyPayload = await historyResponse.json();
      if (!historyResponse.ok) throw new Error(historyPayload.error || historyPayload.message || 'Unable to load transactions.');
      return Array.isArray(historyPayload) ? historyPayload : [];
    }));
    const unique = new Map();
    histories.flat().forEach(item => unique.set(String(item.transactionId), item));
    setHistory([...unique.values()].sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate)));
    return nextAccounts;
  }

  useEffect(() => {
    let active = true;
    loadAccounts()
      .catch(error => { if (active) notify(error.message, true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  async function refresh() {
    try {
      await loadAccounts();
    } catch (error) {
      notify(error.message || 'Unable to refresh your accounts.', true);
    }
  }

  const today = new Intl.DateTimeFormat('en-US', { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date());

  return (
    <div id="dashboard" className="dashboard">
      <Sidebar user={user} onLogout={onLogout} />
      <main id="overview" className="main-content">
        <header className="page-header">
          <div><p id="todayLabel" className="eyebrow">{today}</p><h1>Good to see you, <span id="firstNameLabel">{user.firstName || 'there'}</span></h1><p className="page-subtitle">Here’s what’s happening with your money.</p></div>
          <button id="refreshBtn" className="button button-outline" type="button" onClick={refresh}><span aria-hidden="true">↻</span> Refresh</button>
        </header>
        <section className="overview-grid" aria-label="Account summary">
          <article className="balance-card"><div className="balance-top"><span>Total balance</span><span className="balance-symbol" aria-hidden="true">$</span></div><strong id="totalBalance">{money(total)}</strong><p>Across <span id="accountCount">{accounts.length}</span> <span id="accountWord">{accounts.length === 1 ? 'account' : 'accounts'}</span></p><div className="balance-decoration" aria-hidden="true" /></article>
          <article className="metric-card"><div className="metric-icon green">▤</div><span className="metric-label">Your accounts</span><strong id="accountMetric">{accounts.length}</strong><p>Active banking accounts</p></article>
          <article className="metric-card"><div className="metric-icon gold">↗</div><span className="metric-label">Recent activity</span><strong id="activityCount">{history.length}</strong><p>Transactions across your accounts</p></article>
        </section>
        <section className="content-grid">
          <div id="accounts" className="surface accounts-surface">
            <div className="section-heading"><div><p className="eyebrow">Your money, organized</p><h2>Your accounts</h2></div><span id="accountCountPill" className="count-pill">{accounts.length} {accounts.length === 1 ? 'account' : 'accounts'}</span></div>
            <div id="accountList" className="account-list">
              {loading ? <div className="empty-state">Loading your accounts…</div>
                : accounts.length ? accounts.map(account => <AccountCard key={account.accountId} account={account} />)
                  : <div className="empty-state">Your banking starts here.<br />Open an account to see it in your overview.</div>}
            </div>
          </div>
          <ActionCard accounts={accounts} onAccountCreated={loadAccounts} onTransfer={loadAccounts} notify={notify} />
        </section>
        <section id="activity" className="surface activity-surface">
          <div className="section-heading"><div><p className="eyebrow">The latest</p><h2>Recent activity</h2></div><span className="activity-caption">Across all your accounts</span></div>
          <div id="historyList" className="activity-list">
            {!history.length
              ? <div className="empty-state">No activity yet. Your transfers will show up here.</div>
              : history.map(item => {
                const outgoing = accounts.some(account => String(account.accountId) === String(item.fromAccountId));
                return (
                  <article className="activity-item" key={item.transactionId}>
                    <div className={`activity-icon${outgoing ? ' outgoing' : ''}`} aria-hidden="true">{outgoing ? '↗' : '↙'}</div>
                    <div className="activity-copy"><strong>{item.description || item.type || 'Transfer'}</strong><span>{outgoing ? `To account ${item.toAccountId}` : `From account ${item.fromAccountId}`} · {safeDate(item.transactionDate)}</span></div>
                    <div className="activity-amount"><strong>{outgoing ? '−' : '+'}{money(item.amount)}</strong><span>{item.status || 'Completed'}</span></div>
                  </article>
                );
              })}
          </div>
        </section>
        <footer className="page-footer">Northstar Bank <span>·</span> A banking demo project</footer>
      </main>
    </div>
  );
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('bankingToken') || '');
  const [user, setUser] = useState(storedUser);
  const [toast, setToast] = useState(null);

  function notify(message, error = false) {
    setToast({ message, error, key: Date.now() });
  }

  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(null), 3400);
    return () => window.clearTimeout(timer);
  }, [toast]);

  function authenticate(payload) {
    const nextUser = {
      id: payload.id,
      firstName: payload.firstName,
      lastName: payload.lastName,
      email: payload.email
    };
    localStorage.setItem('bankingToken', payload.token);
    localStorage.setItem('bankingUser', JSON.stringify(nextUser));
    setUser(nextUser);
    setToken(payload.token);
  }

  function logout() {
    localStorage.removeItem('bankingToken');
    localStorage.removeItem('bankingUser');
    setToken('');
    setUser(null);
    notify('You have signed out.');
  }

  return (
    <>
      {token && user
        ? <Dashboard key={token} user={user} onLogout={logout} notify={notify} />
        : <AuthPanel onAuthenticated={authenticate} notify={notify} />}
      {toast && <div key={toast.key} id="toast" className={`toast${toast.error ? ' error' : ''}`} role="status" aria-live="polite">{toast.message}</div>}
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);
