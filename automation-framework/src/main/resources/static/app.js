function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('bankingUser') || 'null');
  } catch {
    return null;
  }
}

const state = {
  token: localStorage.getItem('bankingToken') || '',
  user: readStoredUser(),
  accounts: [],
  history: [],
  toastTimer: null
};

const $ = selector => document.querySelector(selector);
const authPanel = $('#authPanel');
const dashboard = $('#dashboard');
const toast = $('#toast');

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle('error', isError);
  toast.classList.remove('hidden');
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => toast.classList.add('hidden'), 3400);
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[character]);
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value) || 0);
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Date unavailable'
    : new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(date);
}

async function readResponse(response) {
  const text = await response.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function clearSession() {
  state.token = '';
  state.user = null;
  state.accounts = [];
  state.history = [];
  localStorage.removeItem('bankingToken');
  localStorage.removeItem('bankingUser');
  setAuthState();
}

async function apiFetch(url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (state.token) {
    headers.set('Authorization', `Bearer ${state.token}`);
  }

  const response = await fetch(url, { ...options, headers });
  if (response.status === 401 && state.token) {
    clearSession();
    showToast('Your session has expired. Please sign in again.', true);
  }
  return response;
}

function setAuthState() {
  const loggedIn = Boolean(state.token);
  authPanel.classList.toggle('hidden', loggedIn);
  dashboard.classList.toggle('hidden', !loggedIn);
  if (!loggedIn) return;

  const fullName = [state.user?.firstName, state.user?.lastName].filter(Boolean).join(' ') || 'Northstar member';
  $('#userLabel').textContent = fullName;
  $('#firstNameLabel').textContent = state.user?.firstName || 'there';
  $('#userAvatar').textContent = (state.user?.firstName || 'N').charAt(0).toUpperCase();
  $('#todayLabel').textContent = new Intl.DateTimeFormat('en-US', {
    weekday: 'long', month: 'long', day: 'numeric'
  }).format(new Date());
  loadAccounts();
}

function renderAccounts() {
  const count = state.accounts.length;
  const total = state.accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0);
  $('#accountCount').textContent = count;
  $('#accountMetric').textContent = count;
  $('#accountWord').textContent = count === 1 ? 'account' : 'accounts';
  $('#accountCountPill').textContent = `${count} ${count === 1 ? 'account' : 'accounts'}`;
  $('#totalBalance').textContent = formatCurrency(total);

  $('#accountList').innerHTML = count
    ? state.accounts.map(account => {
      const type = escapeHtml(account.accountType || 'Account');
      const number = escapeHtml(account.accountNumber || '—');
      const accountId = escapeHtml(account.accountId);
      const isSavings = String(account.accountType).toUpperCase() !== 'CHECKING';
      return `
        <article class="account-card">
          <div class="account-icon ${isSavings ? 'savings' : ''}" aria-hidden="true">${isSavings ? '◈' : '▤'}</div>
          <div class="account-details">
            <strong>${type}</strong>
            <span>•••• ${number.slice(-4)} · ID ${accountId}</span>
            <span class="status-badge">${account.isActive ? 'Active' : 'Inactive'}</span>
          </div>
          <strong class="account-balance">${formatCurrency(account.balance)}</strong>
        </article>`;
    }).join('')
    : '<div class="empty-state">Your banking starts here.<br />Open an account to see it in your overview.</div>';

  const options = state.accounts.map(account => (
    `<option value="${escapeHtml(account.accountId)}">${escapeHtml(account.accountType)} ·•••• ${escapeHtml(account.accountNumber).slice(-4)}</option>`
  )).join('');
  ['#fromAccountId', '#toAccountId'].forEach(selector => {
    const select = $(selector);
    const previousValue = select.value;
    select.innerHTML = `<option value="">Choose an account</option>${options}`;
    if (state.accounts.some(account => String(account.accountId) === previousValue)) {
      select.value = previousValue;
    }
    select.disabled = count < 2;
  });
  $('#transferForm button[type="submit"]').disabled = count < 2;
  $('#transferForm button[type="submit"]').title = count < 2 ? 'Create at least two accounts to make a transfer' : '';
}

function renderHistory() {
  $('#activityCount').textContent = state.history.length;
  $('#historyList').innerHTML = state.history.length
    ? state.history.map(item => {
      const outgoing = state.accounts.some(account => String(account.accountId) === String(item.fromAccountId));
      const description = escapeHtml(item.description || item.type || 'Transfer');
      const status = escapeHtml(item.status || 'Completed');
      const source = escapeHtml(item.fromAccountId);
      const destination = escapeHtml(item.toAccountId);
      return `
        <article class="activity-item">
          <div class="activity-icon ${outgoing ? 'outgoing' : ''}" aria-hidden="true">${outgoing ? '↗' : '↙'}</div>
          <div class="activity-copy">
            <strong>${description}</strong>
            <span>${outgoing ? `To account ${destination}` : `From account ${source}`} · ${formatDate(item.transactionDate)}</span>
          </div>
          <div class="activity-amount">
            <strong>${outgoing ? '−' : '+'}${formatCurrency(item.amount)}</strong>
            <span>${status}</span>
          </div>
        </article>`;
    }).join('')
    : '<div class="empty-state">No activity yet. Your transfers will show up here.</div>';
}

async function loadAccounts() {
  try {
    const response = await apiFetch('/api/accounts');
    const payload = await readResponse(response);
    if (!response.ok) {
      if (response.status !== 401) throw new Error(payload.error || payload.message || 'Unable to load accounts.');
      return;
    }
    state.accounts = Array.isArray(payload) ? payload : [];
    renderAccounts();

    const histories = await Promise.all(state.accounts.map(async account => {
      const historyResponse = await apiFetch(`/api/accounts/${encodeURIComponent(account.accountId)}/transactions`);
      const historyPayload = await readResponse(historyResponse);
      if (!historyResponse.ok) {
        if (historyResponse.status === 401) return [];
        throw new Error(historyPayload.error || historyPayload.message || 'Unable to load transaction history.');
      }
      return Array.isArray(historyPayload) ? historyPayload : [];
    }));
    const unique = new Map();
    histories.flat().forEach(item => unique.set(String(item.transactionId), item));
    state.history = [...unique.values()].sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
    renderHistory();
  } catch (error) {
    showToast(error.message || 'Unable to load your banking information.', true);
  }
}

function saveSession(payload) {
  state.token = payload.token;
  state.user = {
    id: payload.id,
    firstName: payload.firstName,
    lastName: payload.lastName,
    email: payload.email
  };
  localStorage.setItem('bankingToken', state.token);
  localStorage.setItem('bankingUser', JSON.stringify(state.user));
  setAuthState();
}

async function handleAuth(event, endpoint) {
  event.preventDefault();
  const form = event.currentTarget;
  const isLogin = endpoint.endsWith('/login');
  const body = isLogin
    ? { email: $('#loginEmail').value.trim(), password: $('#loginPassword').value }
    : {
      firstName: $('#registerFirstName').value.trim(),
      lastName: $('#registerLastName').value.trim(),
      email: $('#registerEmail').value.trim(),
      password: $('#registerPassword').value
    };
  try {
    const response = await apiFetch(endpoint, { method: 'POST', body: JSON.stringify(body) });
    const payload = await readResponse(response);
    if (!response.ok) throw new Error(payload.error || payload.message || (isLogin ? 'Sign in failed.' : 'Registration failed.'));
    if (!payload.token) throw new Error('The server did not return a sign-in token.');
    form.reset();
    saveSession(payload);
    showToast(isLogin ? 'Welcome back.' : 'Your account is ready.');
  } catch (error) {
    showToast(error.message || 'Unable to sign in.', true);
  }
}

async function handleCreateAccount(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = {
    accountType: $('#accountType').value,
    initialBalance: Number($('#initialBalance').value || 0)
  };
  try {
    const response = await apiFetch('/api/accounts', { method: 'POST', body: JSON.stringify(payload) });
    const result = await readResponse(response);
    if (!response.ok) throw new Error(result.error || result.message || 'Unable to create account.');
    form.reset();
    $('#initialBalance').value = '0';
    await loadAccounts();
    showToast('Your new account is ready.');
  } catch (error) {
    showToast(error.message || 'Unable to create account.', true);
  }
}

async function handleTransfer(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const fromAccountId = $('#fromAccountId').value;
  const toAccountId = $('#toAccountId').value;
  if (!fromAccountId || !toAccountId || fromAccountId === toAccountId) {
    showToast('Choose two different accounts for this transfer.', true);
    return;
  }
  const payload = {
    fromAccountId: Number(fromAccountId),
    toAccountId: Number(toAccountId),
    amount: Number($('#transferAmount').value),
    description: $('#transferDescription').value.trim() || 'Transfer'
  };
  try {
    const response = await apiFetch('/api/transactions/transfer', { method: 'POST', body: JSON.stringify(payload) });
    const result = await readResponse(response);
    if (!response.ok) throw new Error(result.error || result.message || 'Transfer failed.');
    form.reset();
    await loadAccounts();
    showToast('Transfer complete.');
  } catch (error) {
    showToast(error.message || 'Transfer failed.', true);
  }
}

document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    const selected = tab.dataset.tab;
    document.querySelectorAll('.tab').forEach(item => {
      const active = item === tab;
      item.classList.toggle('active', active);
      item.setAttribute('aria-selected', String(active));
    });
    $('#loginForm').classList.toggle('active', selected === 'login');
    $('#registerForm').classList.toggle('active', selected === 'register');
  });
});

$('#loginForm').addEventListener('submit', event => handleAuth(event, '/api/auth/login'));
$('#registerForm').addEventListener('submit', event => handleAuth(event, '/api/auth/register'));
$('#accountForm').addEventListener('submit', handleCreateAccount);
$('#transferForm').addEventListener('submit', handleTransfer);
$('#logoutBtn').addEventListener('click', () => {
  clearSession();
  showToast('You have signed out.');
});
$('#refreshBtn').addEventListener('click', loadAccounts);

setAuthState();
