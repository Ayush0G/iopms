/* Shared API client + tiny render helpers for all pages. */
const API = {
  get token() { return localStorage.getItem('token'); },

  async req(path, opts = {}) {
    const res = await fetch('/api' + path, {
      method: opts.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(this.token ? { Authorization: 'Bearer ' + this.token } : {}),
      },
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    });
    if (res.status === 401) { logout(); throw new Error('Not logged in'); }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || ('Request failed (' + res.status + ')'));
    }
    return res.status === 204 ? null : res.json();
  },
  get: (p) => API.req(p),
  post: (p, body) => API.req(p, { method: 'POST', body }),
  put: (p, body) => API.req(p, { method: 'PUT', body }),
  /** For CSV downloads that need the auth header. */
  async text(path) {
    const res = await fetch('/api' + path, { headers: { Authorization: 'Bearer ' + this.token } });
    if (!res.ok) throw new Error('Download failed (' + res.status + ')');
    return res.text();
  },
};

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('role');
  localStorage.removeItem('name');
  location.href = '/index.html';
}

/** Escape user data before it goes into innerHTML. */
function esc(v) {
  const d = document.createElement('div');
  d.textContent = v == null ? '' : v;
  return d.innerHTML;
}

function minutes(m) {
  if (m == null) return '—';
  return Math.floor(m / 60) + 'h ' + (m % 60) + 'm';
}

function badge(status) {
  return '<span class="badge b-' + esc(String(status || '').toLowerCase()) + '">' + esc(status) + '</span>';
}

function showErr(e) { alert(e.message || String(e)); }

function guard(role) {
  if (!API.token) { location.href = '/index.html'; return false; }
  if (localStorage.getItem('role') !== role) {
    location.href = localStorage.getItem('role') === 'ADMIN' ? '/admin.html' : '/intern.html';
    return false;
  }
  document.getElementById('who').textContent = localStorage.getItem('name') || '';
  return true;
}

/** Wires a nav: each <button data-tab> shows the <section id="tab-..."> and calls its loader. */
function tabs(loaders) {
  document.querySelectorAll('nav button[data-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('nav button').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('main > section').forEach(s => s.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
      loaders[btn.dataset.tab]();
    });
  });
  const first = document.querySelector('nav button[data-tab]');
  if (first) first.click();
}

function today() { return new Date().toISOString().slice(0, 10); }
function thisMonth() { return new Date().toISOString().slice(0, 7); }

function downloadCsv(filename, text) {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}
