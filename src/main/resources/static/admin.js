/* Admin screens (Modules A-G). Each tab loads its data on click. */
const state = { interns: [], editInternId: null, editInternActive: null, editTaskId: null, editProjectId: null };

async function loadInterns() {
  state.interns = await API.get('/admin/interns');
  const rows = state.interns.map(i => `
    <tr>
      <td>${esc(i.name)}</td><td>${esc(i.email)}</td><td>${esc(i.college)}</td>
      <td>${esc(fmtDate(i.joinedDate))}</td><td>${esc(fmtDate(i.endDate))}</td>
      <td>${badge(i.active ? 'ACTIVE' : 'INACTIVE')}</td>
      <td>
        <button class="small ghost" onclick='fillIntern(${JSON.stringify(JSON.stringify(i))})'>Edit</button>
        <button class="small ghost" onclick="toggleIntern(${i.id}, ${i.active})">${i.active ? 'Deactivate' : 'Reactivate'}</button>
        <button class="small ghost" onclick="openInternReport(${i.id}, '${esc(i.name)}')">Report</button>
      </td>
    </tr>`).join('');
  // intern-list only exists once the Interns tab has rendered; other tabs call
  // this just to refresh state.interns for their assignee pickers
  const list = document.getElementById('intern-list');
  if (list) {
    list.innerHTML =
        `<table><tr><th>Name</th><th>Email</th><th>College</th><th>Joined</th><th>End</th><th>Status</th><th></th></tr>${rows}</table>`;
  }
  // Assignee pickers in Tasks/Projects/Reports filters
  const opts = state.interns.map(i => `<option value="${i.id}">${esc(i.name)}</option>`).join('');
  ['task-assignee', 'report-intern', 'project-interns'].forEach(id => {
    const sel = document.getElementById(id);
    if (sel) { const keep = sel.multiple ? [...sel.selectedOptions].map(o => o.value) : sel.value; sel.innerHTML = (id === 'report-intern' ? '<option value="">All interns</option>' : '') + opts; if (keep) [...sel.options].forEach(o => { if (keep.includes(o.value)) o.selected = true; }); }
  });
}

function fillIntern(json) {
  const i = JSON.parse(json);
  state.editInternId = i.id;
  state.editInternActive = i.active;
  setForm('intern-form', { name: i.name, email: i.email, college: i.college, joinedDate: i.joinedDate, endDate: i.endDate });
  document.getElementById('intern-editing').textContent = 'Editing #' + i.id + ' (leave password empty to keep it)';
}

async function saveIntern(e) {
  e.preventDefault();
  const f = getForm('intern-form');
  try {
    if (state.editInternId) {
      await API.put('/admin/interns/' + state.editInternId, {
        name: f.name, phone: f.phone, college: f.college, joinedDate: f.joinedDate || null,
        endDate: f.endDate || null, active: state.editInternActive,
        ...(f.password ? { password: f.password } : {}),
      });
    } else {
      await API.post('/admin/interns', { name: f.name, email: f.email, password: f.password, college: f.college, joinedDate: f.joinedDate });
    }
    state.editInternId = null; clearForm('intern-form'); document.getElementById('intern-editing').textContent = '';
    loadInterns();
  } catch (ex) { showErr(ex); }
}

async function toggleIntern(id, active) {
  const i = state.interns.find(x => x.id === id);
  try {
    await API.put('/admin/interns/' + id, {
      name: i.name, phone: i.phone || '', college: i.college || '',
      joinedDate: i.joinedDate, endDate: i.endDate, active: !active,
    });
    loadInterns();
  } catch (ex) { showErr(ex); }
}

/* ---- Dashboard ---- */
async function loadDashboard() {
  const d = await API.get('/admin/dashboard');
  const cards = (items) => '<div class="cards">' + items.map(([lbl, num]) => `<div class="card"><div class="num">${num}</div><div class="lbl">${lbl}</div></div>`).join('') + '</div>';
  document.getElementById('tab-dashboard').innerHTML =
    `<div class="card"><h2>Team</h2>${cards([['Total interns', d.team.interns], ['Active interns', d.team.active], ['Completed internships', d.team.completedInternships], ['Working now', d.team.workingNow]])}</div>
     <div class="card"><h2>Attendance (today)</h2>${cards([['Present today', d.attendance.presentToday], ['Hours logged today', d.attendance.hoursToday], ['Incomplete sessions', d.attendance.incompleteSessions]])}</div>
     <div class="card"><h2>Tasks</h2>${cards([['Total tasks', d.tasks.total], ['Completed', d.tasks.completed], ['Pending', d.tasks.pending], ['Overdue', d.tasks.overdue]])}</div>
     <div class="card"><h2>Reports (today)</h2>${cards([['Submitted', d.reports.submittedToday], ['Missing', d.reports.missingToday], ['Awaiting review', d.reports.awaitingReview], ['Approved (all time)', d.reports.approved]])}</div>
     <div class="card"><h2>Projects</h2>${cards([['Active', d.projects.active], ['Completed', d.projects.completed], ['Submissions awaiting approval', d.projects.pendingSubmissions]])}</div>`;
}

/* ---- Attendance ---- */
async function loadAttendance() {
  document.getElementById('tab-attendance').innerHTML = `
    <div class="card">
      <h2>Sessions on a date</h2>
      <div class="row"><input type="date" id="att-date" value="${today()}">
      <button onclick="loadAttendanceDate()">Show</button></div>
      <div id="att-list"></div>
    </div>
    <div class="card">
      <h2>Correct a session (audited)</h2>
      <form id="correct-form" class="inline">
        <div><label>Attendance ID</label><input id="c-id" type="number" required></div>
        <div><label>Check-in</label><input id="c-in" type="datetime-local" required></div>
        <div><label>Check-out (empty = open session)</label><input id="c-out" type="datetime-local"></div>
        <div><label>Reason (required)</label><input id="c-reason" required></div>
        <div style="align-self:end"><button>Save correction</button></div>
      </form>
      <p class="muted">Corrections are recorded in the audit history.</p>
      <div id="audit-box"></div>
    </div>`;
  document.getElementById('correct-form').addEventListener('submit', saveCorrection);
  loadAttendanceDate();
}

async function loadAttendanceDate() {
  const date = document.getElementById('att-date').value;
  const list = await API.get('/admin/attendance?date=' + date);
  document.getElementById('att-list').innerHTML = list.length === 0 ? '<p class="muted">No sessions on this date.</p>' :
    `<table><tr><th>ID</th><th>Intern</th><th>Check-in</th><th>Check-out</th><th>Hours</th><th>Corrected</th><th></th></tr>` +
    list.map(a => `<tr><td>${a.id}</td><td>${esc(a.internName)}</td><td>${esc(fmtDateTime(a.checkIn))}</td><td>${esc(fmtDateTime(a.checkOut))}</td>
      <td>${minutes(a.minutes)}</td><td>${a.correction === 'CORRECTED' ? 'yes' : 'no'}</td>
      <td><button class="small ghost" onclick="prefillCorrection(${a.id})">Correct</button>
      <button class="small ghost" onclick="showAudit(${a.id})">Audit</button></td></tr>`).join('') + '</table>';
}

function prefillCorrection(id) {
  document.getElementById('c-id').value = id;
  document.getElementById('c-reason').focus();
}

async function saveCorrection(e) {
  e.preventDefault();
  const f = getForm('correct-form');
  try {
    await API.post('/admin/attendance/' + f['c-id'] + '/correct', {
      checkIn: f['c-in'], checkOut: f['c-out'] || null, reason: f['c-reason'],
    });
    loadAttendanceDate(); document.getElementById('audit-box').innerHTML = '<p class="ok">Correction saved.</p>';
  } catch (ex) { showErr(ex); }
}

async function showAudit(id) {
  const rows = await API.get('/admin/attendance/' + id + '/audit');
  document.getElementById('audit-box').innerHTML = rows.length === 0 ? '<p class="muted">No corrections recorded for #' + id + '.</p>' :
    `<table><tr><th>At</th><th>Old in</th><th>Old out</th><th>New in</th><th>New out</th><th>Reason</th><th>By</th></tr>` +
    rows.map(a => `<tr><td>${esc(fmtDateTime(a.at))}</td><td>${esc(fmtDateTime(a.oldCheckIn))}</td><td>${esc(fmtDateTime(a.oldCheckOut))}</td><td>${esc(fmtDateTime(a.newCheckIn))}</td><td>${esc(fmtDateTime(a.newCheckOut))}</td><td>${esc(a.reason)}</td><td>${esc(a.admin.name)}</td></tr>`).join('') + '</table>';
}

/* ---- Tasks ---- */
async function loadTasks() {
  document.getElementById('tab-tasks').innerHTML = `
    <div class="card">
      <h2>${state.editTaskId ? 'Edit' : 'Assign'} task</h2>
      <form id="task-form" class="inline">
        <div><label>Title</label><input id="t-title" required></div>
        <div><label>Description</label><input id="t-desc"></div>
        <div><label>Assignee</label><select id="task-assignee" required></select></div>
        <div><label>Priority</label><select id="t-priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option></select></div>
        <div><label>Start date</label><input id="t-start" type="date" required></div>
        <div><label>Deadline</label><input id="t-deadline" type="date" required></div>
        <div><label>Status</label><select id="t-status"><option>NOT_STARTED</option><option>IN_PROGRESS</option><option>UNDER_REVIEW</option><option>COMPLETED</option><option>CHANGES_REQUESTED</option></select></div>
        <div><label>Admin remarks</label><input id="t-remarks"></div>
        <div style="align-self:end"><button>Save task</button> <button type="button" class="ghost" onclick="resetTaskForm()">New</button></div>
      </form>
    </div>
    <div class="card">
      <h2>All tasks</h2>
      <div class="row"><select id="task-filter-intern"><option value="">All interns</option></select>
      <select id="task-filter-status"><option value="">All statuses</option>${['NOT_STARTED','IN_PROGRESS','UNDER_REVIEW','COMPLETED','CHANGES_REQUESTED'].map(s => `<option>${s}</option>`).join('')}</select>
      <button class="ghost" onclick="loadTaskList()">Filter</button></div>
      <div id="task-list"></div>
    </div>`;
  document.getElementById('task-form').addEventListener('submit', saveTask);
  await loadInterns();
  document.getElementById('task-filter-intern').innerHTML += state.interns.map(i => `<option value="${i.id}">${esc(i.name)}</option>`).join('');
  loadTaskList();
}

async function loadTaskList() {
  const internId = document.getElementById('task-filter-intern').value;
  const status = document.getElementById('task-filter-status').value;
  const qs = new URLSearchParams(); if (internId) qs.set('internId', internId); if (status) qs.set('status', status);
  const list = await API.get('/admin/tasks?' + qs);
  document.getElementById('task-list').innerHTML = list.length === 0 ? '<p class="muted">No tasks.</p>' :
    `<table><tr><th>ID</th><th>Title</th><th>Assignee</th><th>Priority</th><th>Deadline</th><th>Status</th><th>Progress</th><th>Remarks</th><th></th></tr>` +
    list.map(t => `<tr><td>${t.id}</td><td>${esc(t.title)}</td><td>${esc(t.assigneeName)}</td><td>${badge(t.priority)}</td><td>${esc(fmtDate(t.deadline))}</td><td>${badge(t.status)}</td><td>${t.progress}%</td><td>${esc(t.adminRemarks)}</td>
      <td><button class="small ghost" onclick='fillTask(${JSON.stringify(JSON.stringify(t))})'>Edit</button></td></tr>`).join('') + '</table>';
}

function fillTask(json) {
  const t = JSON.parse(json);
  state.editTaskId = t.id;
  setForm('task-form', { 't-title': t.title, 't-desc': t.description, 'task-assignee': String(t.assigneeId), 't-priority': t.priority, 't-start': t.startDate, 't-deadline': t.deadline, 't-status': t.status, 't-remarks': t.adminRemarks });
}

function resetTaskForm() { state.editTaskId = null; clearForm('task-form'); }

async function saveTask(e) {
  e.preventDefault();
  const f = getForm('task-form');
  const body = { title: f['t-title'], description: f['t-desc'], assigneeId: Number(f['task-assignee']), priority: f['t-priority'], startDate: f['t-start'], deadline: f['t-deadline'], status: f['t-status'], adminRemarks: f['t-remarks'] };
  try {
    if (state.editTaskId) await API.put('/admin/tasks/' + state.editTaskId, body);
    else await API.post('/admin/tasks', body);
    resetTaskForm(); loadTaskList();
  } catch (ex) { showErr(ex); }
}

/* ---- Reports ---- */
async function loadReports() {
  document.getElementById('tab-reports').innerHTML = `
    <div class="card">
      <h2>Daily reports</h2>
      <div class="row"><select id="report-intern"></select>
      <input type="date" id="report-date">
      <select id="report-status"><option value="">All statuses</option><option>PENDING</option><option>APPROVED</option><option>CHANGES_REQUESTED</option></select>
      <button class="ghost" onclick="loadReportList()">Filter</button></div>
      <div id="report-list"></div>
    </div>
    <div class="card">
      <h2>Missing reports on a date</h2>
      <div class="row"><input type="date" id="missing-date" value="${today()}"><button onclick="loadMissing()">Check</button></div>
      <div id="missing-list"></div>
    </div>`;
  await loadInterns();
  loadReportList();
}

async function loadReportList() {
  const qs = new URLSearchParams();
  const internId = document.getElementById('report-intern').value;
  const date = document.getElementById('report-date').value;
  const status = document.getElementById('report-status').value;
  if (internId) qs.set('internId', internId); if (date) qs.set('date', date); if (status) qs.set('status', status);
  const list = await API.get('/admin/reports?' + qs);
  document.getElementById('report-list').innerHTML = list.length === 0 ? '<p class="muted">No reports.</p>' :
    list.map(r => `<details><summary>${esc(fmtDate(r.date))} — ${esc(r.internName)} — ${badge(r.status)} — ${r.progress}%</summary>
      <p><b>Tasks completed:</b> ${esc(r.tasksCompleted)}<br><b>Work performed:</b> ${esc(r.workPerformed)}<br>
      <b>Time spent:</b> ${esc(r.timeSpent)}<br><b>Challenges:</b> ${esc(r.challenges)}<br>
      <b>Pending:</b> ${esc(r.pendingWork)}<br><b>Next day plan:</b> ${esc(r.nextDayPlan)}<br>
      <b>Links:</b> ${esc(r.links)}<br><b>Submitted:</b> ${esc(fmtDateTime(r.submittedAt))}<br><b>Remarks:</b> ${esc(r.adminRemarks)}</p>
      <div class="row"><button class="small" onclick="reviewReport(${r.id}, 'APPROVED')">Approve</button>
      <button class="small ghost" onclick="reviewReport(${r.id}, 'CHANGES_REQUESTED')">Request changes</button></div></details>`).join('');
}

async function reviewReport(id, status) {
  const remarks = status === 'APPROVED' ? prompt('Optional remarks:') : prompt('What needs to change?');
  if (remarks === null) return;
  try { await API.post('/admin/reports/' + id + '/review', { status, remarks }); loadReportList(); }
  catch (ex) { showErr(ex); }
}

async function loadMissing() {
  const date = document.getElementById('missing-date').value;
  const list = await API.get('/admin/reports/missing?date=' + date);
  document.getElementById('missing-list').innerHTML = list.length === 0 ? '<p class="ok">All active interns submitted.</p>' :
    `<table><tr><th>ID</th><th>Name</th><th>Email</th></tr>` + list.map(m => `<tr><td>${m.id}</td><td>${esc(m.name)}</td><td>${esc(m.email)}</td></tr>`).join('') + '</table>';
}

/* ---- Projects ---- */
async function loadProjects() {
  document.getElementById('tab-projects').innerHTML = `
    <div class="card">
      <h2>${state.editProjectId ? 'Edit' : 'Create'} project</h2>
      <form id="project-form" class="inline">
        <div><label>Title</label><input id="p-title" required></div>
        <div><label>Description</label><input id="p-desc"></div>
        <div><label>Supervisor</label><input id="p-supervisor" required></div>
        <div><label>Start date</label><input id="p-start" type="date" required></div>
        <div><label>Expected completion</label><input id="p-end" type="date"></div>
        <div><label>Status</label><select id="p-status"><option>ACTIVE</option><option>COMPLETED</option><option>ON_HOLD</option></select></div>
        <div style="grid-column:1/-1"><label>Assigned interns (Ctrl+click for multiple)</label><select id="project-interns" multiple size="4"></select></div>
        <div style="align-self:end"><button>Save project</button> <button type="button" class="ghost" onclick="resetProjectForm()">New</button></div>
      </form>
    </div>
    <div class="card">
      <h2>All projects</h2><div id="project-list"></div>
    </div>
    <div class="card">
      <h2>Review submissions</h2>
      <div class="row"><select id="sub-project"></select><button class="ghost" onclick="loadSubmissions()">Load</button></div>
      <div id="sub-list"></div>
    </div>`;
  document.getElementById('project-form').addEventListener('submit', saveProject);
  await loadInterns();
  document.getElementById('project-interns').innerHTML = state.interns.map(i => `<option value="${i.id}">${esc(i.name)}</option>`).join('');
  const list = await API.get('/admin/projects');
  document.getElementById('project-list').innerHTML = list.length === 0 ? '<p class="muted">No projects.</p>' :
    `<table><tr><th>ID</th><th>Title</th><th>Supervisor</th><th>Dates</th><th>Status</th><th>Interns</th><th></th></tr>` +
    list.map(p => `<tr><td>${p.id}</td><td>${esc(p.title)}</td><td>${esc(p.supervisor)}</td><td>${esc(fmtDate(p.startDate))} → ${esc(fmtDate(p.expectedEndDate))}</td><td>${badge(p.status)}</td><td>${esc(p.internNames.join(', '))}</td>
      <td><button class="small ghost" onclick='fillProject(${JSON.stringify(JSON.stringify(p))})'>Edit</button></td></tr>`).join('') + '</table>';
  document.getElementById('sub-project').innerHTML = list.map(p => `<option value="${p.id}">${esc(p.title)}</option>`).join('');
}

function fillProject(json) {
  const p = JSON.parse(json);
  state.editProjectId = p.id;
  setForm('project-form', { 'p-title': p.title, 'p-desc': p.description, 'p-supervisor': p.supervisor, 'p-start': p.startDate, 'p-end': p.expectedEndDate, 'p-status': p.status });
  [...document.getElementById('project-interns').options].forEach(o => { o.selected = p.internIds.includes(Number(o.value)); });
}

function resetProjectForm() { state.editProjectId = null; clearForm('project-form'); }

async function saveProject(e) {
  e.preventDefault();
  const f = getForm('project-form');
  const internIds = [...document.getElementById('project-interns').selectedOptions].map(o => Number(o.value));
  const body = { title: f['p-title'], description: f['p-desc'], supervisor: f['p-supervisor'], startDate: f['p-start'], expectedEndDate: f['p-end'] || null, status: f['p-status'], internIds };
  try {
    if (state.editProjectId) await API.put('/admin/projects/' + state.editProjectId, body);
    else await API.post('/admin/projects', body);
    resetProjectForm(); loadProjects();
  } catch (ex) { showErr(ex); }
}

async function loadSubmissions() {
  const id = document.getElementById('sub-project').value;
  const list = await API.get('/admin/projects/' + id + '/submissions');
  document.getElementById('sub-list').innerHTML = list.length === 0 ? '<p class="muted">No submissions yet.</p>' :
    list.map(s => `<details><summary>v${s.version} — ${esc(s.internName)} — ${badge(s.status)} — ${esc(fmtDateTime(s.submittedAt))}</summary>
      <p><b>Repo:</b> <a href="${esc(s.repoUrl)}" target="_blank">${esc(s.repoUrl)}</a><br><b>Notes:</b> ${esc(s.notes)}<br><b>Comments:</b> ${esc(s.adminComments)}</p>
      <div class="row"><button class="small" onclick="reviewSubmission(${s.id}, 'APPROVED')">Approve</button>
      <button class="small ghost" onclick="reviewSubmission(${s.id}, 'CHANGES_REQUESTED')">Request changes</button></div></details>`).join('');
}

async function reviewSubmission(id, status) {
  const remarks = status === 'APPROVED' ? prompt('Optional comments:') : prompt('What needs to change?');
  if (remarks === null) return;
  try { await API.post('/admin/submissions/' + id + '/review', { status, remarks }); loadSubmissions(); }
  catch (ex) { showErr(ex); }
}

/* ---- Module G: consolidated intern report ---- */
async function loadInternReport(id, name) {
  document.querySelector('nav button[data-tab="intern-report"]').click();
  if (id) document.getElementById('ir-intern').value = String(id);
  await showInternReport();
}

async function showInternReport() {
  const id = document.getElementById('ir-intern').value;
  const month = document.getElementById('ir-month').value;
  if (!id) { document.getElementById('ir-body').innerHTML = '<p class="muted">Pick an intern.</p>'; return; }
  const r = await API.get(`/admin/interns/${id}/report?month=${month}`);
  const t = (title, rows) => rows.length === 0 ? '' : `<div class="card"><h2>${title}</h2><table>${rows}</table></div>`;
  document.getElementById('ir-body').innerHTML = `
    <div class="card"><h2>${esc(r.profile.name)}</h2>
      <p>${esc(r.profile.email)} · ${esc(r.profile.college || '')} · joined ${esc(fmtDate(r.profile.joinedDate))} · end ${esc(fmtDate(r.profile.endDate))}<br>
      <b>Total working hours this month: ${minutes(r.totalMinutes)}</b></p></div>
    ${t('Attendance', r.attendance.map(a => `<tr><td>${esc(fmtDate(a.date))}</td><td>${esc(fmtDateTime(a.checkIn))}</td><td>${esc(fmtDateTime(a.checkOut))}</td><td>${minutes(a.minutes)}</td><td>${a.correction === 'CORRECTED' ? 'corrected' : ''}</td></tr>`).join(''))}
    ${t('Tasks', r.tasks.map(x => `<tr><td>${esc(x.title)}</td><td>${badge(x.status)}</td><td>${x.progress}%</td><td>${esc(fmtDate(x.deadline))}</td><td>${esc(x.adminRemarks)}</td></tr>`).join(''))}
    ${t('Daily reports', r.reports.map(x => `<tr><td>${esc(fmtDate(x.date))}</td><td>${badge(x.status)}</td><td>${x.progress}%</td><td>${esc(x.adminRemarks)}</td></tr>`).join(''))}
    ${t('Project submissions', r.submissions.map(s => `<tr><td>${esc(s.projectTitle)}</td><td>v${s.version}</td><td>${badge(s.status)}</td><td>${esc(s.adminComments)}</td></tr>`).join(''))}`;
}

async function loadInternReportTab() {
  document.getElementById('tab-intern-report').innerHTML = `
    <div class="row">
      <select id="ir-intern"></select>
      <input type="month" id="ir-month" value="${thisMonth()}">
      <button onclick="showInternReport()">Load report</button>
      <button class="ghost" onclick="downloadInternCsv()">Export CSV</button>
    </div><div id="ir-body"></div>`;
  await loadInterns();
  document.getElementById('ir-intern').innerHTML = state.interns.map(i => `<option value="${i.id}">${esc(i.name)}</option>`).join('');
}

async function downloadInternCsv() {
  const id = document.getElementById('ir-intern').value;
  const month = document.getElementById('ir-month').value;
  const csv = await API.text(`/admin/interns/${id}/report.csv?month=${month}`);
  downloadCsv(`intern-${id}-report-${month}.csv`, csv);
}

/* ---- form helpers ---- */
function setForm(id, values) {
  Object.entries(values).forEach(([k, v]) => { const el = document.getElementById(k); if (el && v != null) el.value = v; });
}
function clearForm(id) { document.getElementById(id).reset(); }

/* ---- boot ---- */
if (guard('ADMIN')) {
  tabs({
    dashboard: loadDashboard,
    interns: async () => { document.getElementById('tab-interns').innerHTML = `
        <div class="card"><h2 id="intern-editing"></h2>
          <form id="intern-form" class="inline">
            <div><label>Name</label><input id="name" required></div>
            <div><label>Email (new only)</label><input id="email" type="email"></div>
            <div><label>Password (new only, min 8)</label><input id="password"></div>
            <div><label>College</label><input id="college"></div>
            <div><label>Phone</label><input id="phone"></div>
            <div><label>Joined date</label><input id="joinedDate" type="date" required></div>
            <div><label>End date</label><input id="endDate" type="date"></div>
            <div style="align-self:end"><button>Save intern</button></div>
          </form></div>
        <div class="card"><h2>All interns</h2><div id="intern-list"></div></div>`;
      document.getElementById('intern-form').addEventListener('submit', saveIntern);
      loadInterns(); },
    attendance: loadAttendance,
    tasks: loadTasks,
    reports: loadReports,
    projects: loadProjects,
    'intern-report': loadInternReportTab,
  });
}
