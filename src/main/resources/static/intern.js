/* Intern screens. All data is scoped to the logged-in user by the backend. */
const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];

/* ---- Profile ---- */
async function loadProfile() {
  const p = await API.get('/auth/me');
  document.getElementById('tab-profile').innerHTML = `
    <div class="card"><h2>My profile</h2>
      <form id="profile-form" class="inline">
        <div><label>Name</label><input id="pf-name" value="${esc(p.name)}" required></div>
        <div><label>Phone</label><input id="pf-phone" value="${esc(p.phone)}"></div>
        <div><label>College</label><input id="pf-college" value="${esc(p.college)}"></div>
        <div style="align-self:end"><button>Save</button></div>
      </form>
      <p class="muted">Email ${esc(p.email)} · joined ${esc(p.joinedDate)} · end ${esc(p.endDate)}</p>
      <p class="ok" id="pf-msg"></p></div>`;
  document.getElementById('profile-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = getForm('profile-form');
    try { await API.put('/auth/me', { name: f['pf-name'], phone: f['pf-phone'], college: f['pf-college'] });
      document.getElementById('pf-msg').textContent = 'Saved.'; }
    catch (ex) { showErr(ex); }
  });
}

/* ---- Attendance ---- */
async function loadAttendance() {
  const month = document.getElementById('att-month').value || thisMonth();
  const [history, incomplete] = await Promise.all([
    API.get('/intern/attendance?month=' + month),
    API.get('/intern/attendance/incomplete'),
  ]);
  const open = history.find(a => a.checkOut == null && a.date === today());
  const total = history.reduce((s, a) => s + (a.minutes || 0), 0);
  const m = month.split('-');
  // render into att-body so the month picker (att-month) survives re-renders
  document.getElementById('att-body').innerHTML = `
    <div class="card">
      <h2>Work session — ${MONTHS[Number(m[1]) - 1]} ${m[0]}</h2>
      <div class="row">
        <button id="btn-start" ${open ? 'disabled' : ''} onclick="punch('start')">Start Work</button>
        <button id="btn-end" class="ghost" ${open ? '' : 'disabled'} onclick="punch('end')">End Work</button>
        <span class="muted">Check-in/check-out is separate from portal login. Times come from the server clock.</span>
      </div>
      ${open ? `<p class="ok">Working since ${esc(open.checkIn)} — session not yet ended.</p>` : ''}
      <p><b>Total this month: ${minutes(total)}</b></p>
      ${incomplete.length ? `<p class="error">Incomplete sessions from earlier days: ${incomplete.length} (never ended)</p>` : ''}
      <table><tr><th>Date</th><th>Check-in</th><th>Check-out</th><th>Hours</th><th>Corrected</th></tr>
      ${history.length === 0 ? '<tr><td colspan="5" class="muted">No records.</td></tr>' :
        history.map(a => `<tr><td>${esc(a.date)}</td><td>${esc(a.checkIn)}</td><td>${esc(a.checkOut)}</td><td>${minutes(a.minutes)}</td><td>${a.correction === 'CORRECTED' ? 'yes' : ''}</td></tr>`).join('')}
      </table></div>`;
}

async function punch(action) {
  try {
    await API.post('/intern/attendance/' + action);
    loadAttendance();
  } catch (ex) { showErr(ex); }
}

/* ---- Tasks ---- */
async function loadTasks() {
  const list = await API.get('/intern/tasks');
  document.getElementById('tab-tasks').innerHTML = list.length === 0 ? '<div class="card"><p class="muted">No tasks assigned yet.</p></div>' :
    list.map(t => `<div class="card">
      <h2>${esc(t.title)} ${badge(t.status)}</h2>
      <p>${esc(t.description)}<br>
      <span class="muted">Priority ${badge(t.priority)} · start ${esc(t.startDate)} · deadline ${esc(t.deadline)} · progress ${t.progress}%</span></p>
      ${t.adminRemarks ? `<p class="muted">Admin remarks: ${esc(t.adminRemarks)}</p>` : ''}
      <div class="row">
        <input id="tp-${t.id}" type="number" min="0" max="100" value="${t.progress}" style="max-width:90px" title="Progress %">
        <select id="ts-${t.id}">
          ${['NOT_STARTED','IN_PROGRESS','UNDER_REVIEW'].map(s => `<option ${t.status === s ? 'selected' : ''}>${s}</option>`).join('')}
        </select>
        <button onclick="saveProgress(${t.id})">Update</button>
      </div></div>`).join('');
}

async function saveProgress(id) {
  const progress = Number(document.getElementById('tp-' + id).value);
  const status = document.getElementById('ts-' + id).value;
  try { await API.put('/intern/tasks/' + id + '/progress', { progress, status }); loadTasks(); }
  catch (ex) { showErr(ex); }
}

/* ---- Daily report ---- */
async function loadReports() {
  const list = await API.get('/intern/reports');
  const submitted = new Set(list.map(r => r.date));
  document.getElementById('tab-reports').innerHTML = `
    <div class="card">
      <h2>Submit daily report</h2>
      <form id="report-form" class="inline">
        <div><label>Date</label><input id="r-date" type="date" value="${today()}" required></div>
        <div><label>Tasks completed</label><input id="r-tasks" required></div>
        <div><label>Work performed</label><input id="r-work" required></div>
        <div><label>Time spent per task</label><input id="r-time" required></div>
        <div><label>Progress %</label><input id="r-progress" type="number" min="0" max="100" required></div>
        <div><label>Technical challenges</label><input id="r-challenges"></div>
        <div><label>Pending work</label><input id="r-pending" required></div>
        <div><label>Plan for next working day</label><input id="r-plan" required></div>
        <div style="grid-column:1/-1"><label>Links / attachments (URLs)</label><input id="r-links"></div>
        <div style="align-self:end"><button>Submit report</button></div>
      </form>
      <p class="muted" id="r-note"></p>
    </div>
    <div class="card"><h2>My reports &amp; admin feedback</h2>
      ${list.length === 0 ? '<p class="muted">No reports yet.</p>' : list.map(r => `<details>
        <summary>${esc(r.date)} — ${badge(r.status)} — ${r.progress}%</summary>
        <p><b>Tasks completed:</b> ${esc(r.tasksCompleted)}<br><b>Work performed:</b> ${esc(r.workPerformed)}<br>
        <b>Time spent:</b> ${esc(r.timeSpent)}<br><b>Challenges:</b> ${esc(r.challenges)}<br>
        <b>Pending:</b> ${esc(r.pendingWork)}<br><b>Next day plan:</b> ${esc(r.nextDayPlan)}<br>
        <b>Links:</b> ${esc(r.links)}<br><b>Admin remarks:</b> ${esc(r.adminRemarks) || '<span class="muted">none yet</span>'}</p>
      </details>`).join('')}
    </div>`;
  document.getElementById('report-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = getForm('report-form');
    try {
      await API.post('/intern/reports', {
        date: f['r-date'], tasksCompleted: f['r-tasks'], workPerformed: f['r-work'],
        timeSpent: f['r-time'], progress: Number(f['r-progress']), challenges: f['r-challenges'],
        pendingWork: f['r-pending'], nextDayPlan: f['r-plan'], links: f['r-links'],
      });
      loadReports();
    } catch (ex) { showErr(ex); }
  });
}

/* ---- Projects ---- */
async function loadProjects() {
  const [projects, submissions] = await Promise.all([
    API.get('/intern/projects'), API.get('/intern/submissions'),
  ]);
  document.getElementById('tab-projects').innerHTML = `
    <div class="card"><h2>Assigned projects</h2>
      ${projects.length === 0 ? '<p class="muted">No projects assigned yet.</p>' : projects.map(p => `
      <div style="border-top:1px solid #eee;padding:8px 0">
        <h3 style="margin:4px 0">${esc(p.title)} ${badge(p.status)}</h3>
        <p>${esc(p.description)}<br><span class="muted">Supervisor ${esc(p.supervisor)} · ${esc(p.startDate)} → ${esc(p.expectedEndDate)}</span></p>
        <div class="row">
          <input id="repo-${p.id}" placeholder="GitHub repository URL" style="max-width:320px">
          <input id="notes-${p.id}" placeholder="Progress notes" style="max-width:320px">
          <button onclick="submitWork(${p.id})">Submit deliverable</button>
        </div>
      </div>`).join('')}
    </div>
    <div class="card"><h2>My submissions &amp; feedback</h2>
      ${submissions.length === 0 ? '<p class="muted">No submissions yet.</p>' : `
      <table><tr><th>Project</th><th>Version</th><th>Repo</th><th>Submitted</th><th>Status</th><th>Admin comments</th></tr>
      ${submissions.map(s => `<tr><td>${esc(s.projectTitle)}</td><td>v${s.version}</td>
        <td><a href="${esc(s.repoUrl)}" target="_blank">repo</a></td><td>${esc(s.submittedAt)}</td>
        <td>${badge(s.status)}</td><td>${esc(s.adminComments) || '—'}</td></tr>`).join('')}
      </table>`}
    </div>`;
}

async function submitWork(projectId) {
  const repoUrl = document.getElementById('repo-' + projectId).value;
  const notes = document.getElementById('notes-' + projectId).value;
  try { await API.post('/intern/projects/' + projectId + '/submissions', { repoUrl, notes }); loadProjects(); }
  catch (ex) { showErr(ex); }
}

/* ---- boot ---- */
if (guard('INTERN')) {
  tabs({
    profile: loadProfile,
    attendance: async () => {
      document.getElementById('tab-attendance').innerHTML = `
        <div class="row"><input type="month" id="att-month" value="${thisMonth()}"><button class="ghost" onclick="loadAttendance()">Show</button></div>
        <div id="att-body"></div>`;
      loadAttendance();
    },
    tasks: loadTasks,
    reports: loadReports,
    projects: loadProjects,
  });
}
