const state = {
  token: localStorage.getItem('bankingToken') || '',
  user: JSON.parse(localStorage.getItem('bankingUser') || 'null'),
  accounts: [],
  history: []
};

const authPanel = document.getElementById('authPanel');
const dashboard = document.getElementById('dashboard');
const logoutBtn = document.getElementById('logoutBtn');
const userLabel = document.getElementById('userLabel');
const accountCount = document.getElementById('accountCount');
const accountList = document.getElementById('accountList');
const historyList = document.getElementById('historyList');
const toast = document.getElementById('toast');

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle('hidden', false);
  toast.style.background = isError ? '#d54949' : '#11263d';
  setTimeout(() => toast.classList.add('hidden'), 2800);
}

function isLoggedIn() {
  return !!state.token;
}

function setAuthState() {
  authPanel.classList.toggle('hidden', isLoggedIn());
  dashboard.classList.toggle('hidden', !isLoggedIn());
  logoutBtn.classList.toggle('hidden', !isLoggedIn());

  if (isLoggedIn()) {
    userLabel.textContent = state.user ? `${state.user.firstName} ${state.user.lastName}` : 'Banking user';
    loadAccounts();
  } else {
    userLabel.textContent = '-';
    accountCount.textContent = '0';
    accountList.innerHTML = '<p>Login to view accounts.</p>';
    historyList.innerHTML = '<p>Transfer history will appear here.</p>';
  }
}

function apiFetch(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  return fetch(url, { ...options, headers });
}

async function loadAccounts() {
  try {
    const response = await apiFetch('/api/accounts');
    if (!response.ok) {
      throw new Error('Unable to load accounts');
    }
    const accounts = await response.json();
    state.accounts = accounts || [];
    accountCount.textContent = String(state.accounts.length);
    accountList.innerHTML = state.accounts.length
      ? state.accounts.map(account => `
        <div class="account-item">
          <strong>${account.accountType} • ${account.accountNumber}</strong>
          <div>Balance: $${Number(account.balance).toFixed(2)}</div>
          <div>Account ID: ${account.accountId}</div>
          <span class="badge ${account.isActive ? 'success' : 'warning'}">${account.isActive ? 'Active' : 'Inactive'}</span>
        </div>
      `).join('')
      : '<p>No accounts yet.</p>';

    if (state.accounts.length > 0) {
      const firstAccountId = state.accounts[0].accountId;
      await loadHistory(firstAccountId);
    } else {
      historyList.innerHTML = '<p>No recent activity.</p>';
    }
  } catch (error) {
    showToast(error.message, true);
  }
}

async function loadHistory(accountId) {
  try {
    const response = await apiFetch(`/api/accounts/${accountId}/transactions`);
    if (!response.ok) {
      throw new Error('Unable to load transaction history');
    }
    const history = await response.json();
    state.history = history || [];
    historyList.innerHTML = state.history.length
      ? state.history.map(item => `
        <div class="history-item">
          <strong>${item.type || 'TRANSFER'}</strong>
          <div>Amount: $${Number(item.amount).toFixed(2)}</div>
          <div>Status: ${item.status}</div>
          <div>From ${item.fromAccountId} → To ${item.toAccountId}</div>
        </div>
      `).join('')
      : '<p>No recent activity.</p>';
  } catch (error) {
    historyList.innerHTML = '<p>No recent activity.</p>';
  }
}

async function handleLogin(event) {
  event.preventDefault();
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  try {
    const response = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.error || 'Login failed');
    }

    state.token = payload.token;
    state.user = {
      id: payload.id,
      firstName: payload.firstName,
      lastName: payload.lastName,
      email: payload.email
    };

    localStorage.setItem('bankingToken', state.token);
    localStorage.setItem('bankingUser', JSON.stringify(state.user));
    document.getElementById('loginForm').reset();
    setAuthState();
    showToast('Logged in successfully');
  } catch (error) {
    showToast(error.message, true);
  }
}

async function handleRegister(event) {
  event.preventDefault();
  const body = {
    firstName: document.getElementById('registerFirstName').value.trim(),
    lastName: document.getElementById('registerLastName').value.trim(),
    email: document.getElementById('registerEmail').value.trim(),
    password: document.getElementById('registerPassword').value
  };

  try {
    const response = await apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(body)
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.error || payload.message || 'Registration failed');
    }

    state.token = payload.token;
    state.user = {
      id: payload.id,
      firstName: payload.firstName,
      lastName: payload.lastName,
      email: payload.email
    };

    localStorage.setItem('bankingToken', state.token);
    localStorage.setItem('bankingUser', JSON.stringify(state.user));
    document.getElementById('registerForm').reset();
    setAuthState();
    showToast('Registration successful');
  } catch (error) {
    showToast(error.message, true);
  }
}

async function handleCreateAccount(event) {
  event.preventDefault();
  const payload = {
    accountType: document.getElementById('accountType').value,
    initialBalance: Number(document.getElementById('initialBalance').value || 0)
  };

  try {
    const response = await apiFetch('/api/accounts', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.error || 'Unable to create account');
    }

    document.getElementById('accountForm').reset();
    document.getElementById('initialBalance').value = '0';
    await loadAccounts();
    showToast('Account created');
  } catch (error) {
    showToast(error.message, true);
  }
}

async function handleTransfer(event) {
  event.preventDefault();
  const payload = {
    fromAccountId: Number(document.getElementById('fromAccountId').value),
    toAccountId: Number(document.getElementById('toAccountId').value),
    amount: Number(document.getElementById('transferAmount').value),
    description: document.getElementById('transferDescription').value || 'Transfer'
  };

  try {
    const response = await apiFetch('/api/transactions/transfer', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.error || result.message || 'Transfer failed');
    }

    document.getElementById('transferForm').reset();
    await loadAccounts();
    showToast('Transfer complete');
  } catch (error) {
    showToast(error.message, true);
  }
}

function attachUiEvents() {
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.tab').forEach(item => item.classList.toggle('active', item === tab));
      const selected = tab.dataset.tab;
      document.getElementById('loginForm').classList.toggle('active', selected === 'login');
      document.getElementById('registerForm').classList.toggle('active', selected === 'register');
    });
  });

  document.getElementById('loginForm').addEventListener('submit', handleLogin);
  document.getElementById('registerForm').addEventListener('submit', handleRegister);
  document.getElementById('accountForm').addEventListener('submit', handleCreateAccount);
  document.getElementById('transferForm').addEventListener('submit', handleTransfer);
  logoutBtn.addEventListener('click', () => {
    state.token = '';
    state.user = null;
    localStorage.removeItem('bankingToken');
    localStorage.removeItem('bankingUser');
    setAuthState();
    showToast('Logged out');
  });
}

attachUiEvents();
setAuthState();
