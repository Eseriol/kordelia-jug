const roleSelect = document.getElementById('role-select');

function currentRole() {
    return roleSelect.value;
}

async function api(path, options = {}) {
    const headers = Object.assign({'X-Role': currentRole()}, options.headers || {});
    const res = await fetch(path, Object.assign({}, options, {headers}));
    const body = await res.json().catch(() => null);
    return {ok: res.ok, status: res.status, body};
}

const CATEGORY_LABELS = {
    A_OFERTA_SZKOLEN: 'A · Oferta szkoleń',
    B_FAQ: 'B · FAQ',
    C_STATUS_SPRAWY: 'C · Status sprawy',
    D_DOKUMENTACJA: 'D · Dokumentacja',
    E_INNE: 'E · Inne'
};

const SOURCE_LABELS = {
    MODEL: 'przez Gemini',
    REGULY_FALLBACK: 'reguły — fallback (nie model!)',
    BEZ_MODELU: 'bez modelu, z definicji'
};

const STATUS_CLASS = {OCZEKUJE: 'pending', ZATWIERDZONE: 'approved', ODRZUCONE: 'rejected'};
const STATUS_LABEL = {OCZEKUJE: 'oczekuje', ZATWIERDZONE: 'zatwierdzone', ODRZUCONE: 'odrzucone'};

const TASK_STATUS_CLASS = {OCZEKUJE: 'pending', OBSLUZONE: 'approved'};
const TASK_STATUS_LABEL = {OCZEKUJE: 'oczekuje', OBSLUZONE: 'obsłużone'};

function categoryRow(result) {
    const label = CATEGORY_LABELS[result.kategoria] || result.kategoria;
    const sourceLabel = SOURCE_LABELS[result.zrodlo] || result.zrodlo;
    const notRealModel = result.zrodlo !== 'MODEL';
    const pct = Math.round(result.pewnosc * 100);
    return `
        <div class="category-row">
            <span class="category-label ${notRealModel ? 'is-no-model' : ''}">${label}</span>
            <span class="confidence-track"><span class="confidence-fill ${notRealModel ? '' : 'is-model'}" style="width:${pct}%"></span></span>
            <span>${pct}%</span>
        </div>
        <div class="item-detail">źródło: ${sourceLabel}</div>
        ${result.szczegoly ? `<div class="item-detail">${result.szczegoly}</div>` : ''}
    `;
}

function renderMails(mails) {
    const container = document.getElementById('mails-list');
    container.innerHTML = '';

    if (!mails || mails.length === 0) {
        container.innerHTML = '<div class="empty">Brak maili.</div>';
        return;
    }

    for (const mail of mails) {
        const div = document.createElement('div');
        div.className = 'item';
        div.id = 'mail-item-' + mail.id;
        div.innerHTML = `
            <div class="item-top">
                <span class="item-title">${mail.id} — ${mail.subject}</span>
                <span class="item-meta">${mail.senderAddress}</span>
            </div>
            <div class="item-body">${mail.body}</div>
            <div data-result-for="${mail.id}"></div>
        `;
        container.appendChild(div);
    }
}

function renderMailResult(result) {
    const el = document.querySelector(`[data-result-for="${result.mailId}"]`);
    if (el) {
        el.innerHTML = `<div class="item-result">${categoryRow(result)}</div>`;
    }
}

function renderInbox(entries) {
    const container = document.getElementById('inbox-list');
    container.innerHTML = '';

    if (!entries || entries.length === 0) {
        container.innerHTML = '<div class="empty">Kolejka pusta — uruchom triage (mail-5, mail-7 to dokumentacja).</div>';
        return;
    }

    for (const entry of entries) {
        container.appendChild(buildInboxItem(entry));
    }

    container.querySelectorAll('[data-approve]').forEach(btn => {
        btn.addEventListener('click', () => decide(btn.dataset.approve, true));
    });
    container.querySelectorAll('[data-reject]').forEach(btn => {
        btn.addEventListener('click', () => decide(btn.dataset.reject, false));
    });
}

function buildInboxItem(entry) {
    const pct = Math.round(entry.pewnosc * 100);
    const disabled = entry.status !== 'OCZEKUJE' ? 'disabled' : '';
    const div = document.createElement('div');
    div.className = 'item';
    div.innerHTML = `
        <div class="item-top">
            <span class="item-title">${entry.id} · ${entry.typ}${entry.trybSeryjnyMozliwy ? ' · tryb seryjny' : ''}</span>
            <span class="pill ${STATUS_CLASS[entry.status]}">${STATUS_LABEL[entry.status]}</span>
        </div>
        <div class="item-body">${entry.opis}</div>
        <div class="category-row" style="margin-top:8px;">
            <span class="confidence-track"><span class="confidence-fill is-model" style="width:${pct}%"></span></span>
            <span>${pct}%</span>
        </div>
        ${entry.decydent ? `<div class="item-meta" style="margin-top:6px;">decyzja: ${entry.decydent}</div>` : ''}
        <div class="item-actions">
            <button class="btn btn-ghost" data-approve="${entry.id}" ${disabled}>Zatwierdź</button>
            <button class="btn btn-ghost" data-reject="${entry.id}" ${disabled}>Odrzuć</button>
        </div>
    `;
    return div;
}

async function loadMails() {
    const {body} = await api('/api/mails');
    renderMails(body);
}

async function loadInbox() {
    const {body} = await api('/api/ai-inbox');
    renderInbox(body);
}

function buildTaskItem(task) {
    const disabled = task.status !== 'OCZEKUJE' ? 'disabled' : '';
    const sourceLabel = task.sourceMailId
        ? `<button class="link-btn" data-jump="${task.sourceMailId}">→ ${task.sourceMailId}</button>`
        : '<span class="item-meta">ręczny test (bez powiązanego maila)</span>';
    const div = document.createElement('div');
    div.className = 'item';
    div.innerHTML = `
        <div class="item-top">
            <span class="item-title">${task.id}</span>
            <span class="pill ${TASK_STATUS_CLASS[task.status]}">${TASK_STATUS_LABEL[task.status]}</span>
        </div>
        <div class="item-body">${task.reason}</div>
        <div class="item-detail">${task.context}</div>
        <div class="item-detail">${sourceLabel}</div>
        <div class="item-actions">
            <button class="btn btn-ghost" data-resolve="${task.id}" ${disabled}>Oznacz jako obsłużone</button>
        </div>
    `;
    return div;
}

function renderTasks(tasks) {
    const container = document.getElementById('tasks-list');
    container.innerHTML = '';

    if (!tasks || tasks.length === 0) {
        container.innerHTML = '<div class="empty">Brak zadań — eskalacje kategorii C i maile kategorii E pojawią się tutaj.</div>';
        return;
    }

    for (const task of tasks) {
        container.appendChild(buildTaskItem(task));
    }

    container.querySelectorAll('[data-resolve]').forEach(btn => {
        btn.addEventListener('click', () => resolveTask(btn.dataset.resolve));
    });
    container.querySelectorAll('[data-jump]').forEach(btn => {
        btn.addEventListener('click', () => highlightMail(btn.dataset.jump));
    });
}

function highlightMail(mailId) {
    const el = document.getElementById('mail-item-' + mailId);
    if (!el) {
        return;
    }
    el.scrollIntoView({behavior: 'smooth', block: 'center'});
    el.classList.add('flash');
    setTimeout(() => el.classList.remove('flash'), 1500);
}

async function loadTasks() {
    const {body} = await api('/api/tasks');
    renderTasks(body);
}

async function resolveTask(id) {
    await api(`/api/tasks/resolve?id=${id}`, {method: 'POST'});
    await loadTasks();
    await loadStats();
}

function renderStats(s) {
    document.getElementById('stats-panel').innerHTML = `
        <div class="stat-group-title">Ostatni triage (${s.przetworzoneOstatnioRazem} maili)</div>
        <div class="stat-row"><span>A · Oferta szkoleń</span><span>${s.kategoriaA}</span></div>
        <div class="stat-row"><span>B · FAQ</span><span>${s.kategoriaB}</span></div>
        <div class="stat-row"><span>C · Status sprawy</span><span>${s.kategoriaC}</span></div>
        <div class="stat-row"><span>D · Dokumentacja</span><span>${s.kategoriaD}</span></div>
        <div class="stat-row"><span>E · Inne</span><span>${s.kategoriaE}</span></div>
        <div class="stat-group-title">Źródło klasyfikacji</div>
        <div class="stat-row"><span>przez Gemini</span><span>${s.przezModel}</span></div>
        <div class="stat-row"><span>reguły — fallback</span><span>${s.przezReguly}</span></div>
        <div class="stat-row"><span>bez modelu (kategoria C)</span><span>${s.bezModelu}</span></div>
        <div class="stat-group-title">ai_inbox</div>
        <div class="stat-row"><span>oczekuje</span><span>${s.inboxOczekuje}</span></div>
        <div class="stat-row"><span>zatwierdzone</span><span>${s.inboxZatwierdzone}</span></div>
        <div class="stat-row"><span>odrzucone</span><span>${s.inboxOdrzucone}</span></div>
        <div class="stat-group-title">Zadania dla człowieka</div>
        <div class="stat-row"><span>otwarte</span><span>${s.zadaniaOtwarte}</span></div>
        <div class="stat-row"><span>obsłużone</span><span>${s.zadaniaObsluzone}</span></div>
    `;
}

async function loadStats() {
    const {body} = await api('/api/stats');
    if (body) {
        renderStats(body);
    }
}

function initTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const tab = btn.dataset.tab;
            document.getElementById('stats-panel').hidden = tab !== 'stats';
            document.getElementById('activity-log').hidden = tab !== 'log';
        });
    });
}

async function runTriage() {
    const btn = document.getElementById('run-triage-btn');
    btn.disabled = true;
    btn.textContent = 'Przetwarzam…';
    const {body} = await api('/api/triage/run', {method: 'POST'});
    if (body && body.wyniki) {
        body.wyniki.forEach(renderMailResult);
    }
    btn.disabled = false;
    btn.textContent = 'Uruchom triage';
    await loadInbox();
    await loadTasks();
    await loadStats();
}

async function runCaseStatus() {
    const sender = document.getElementById('cs-sender').value.trim();
    const tekst = document.getElementById('cs-text').value.trim();
    const {body} = await api(`/api/case-status?sender=${encodeURIComponent(sender)}&tekst=${encodeURIComponent(tekst)}`, {method: 'POST'});
    const el = document.getElementById('case-status-result');
    if (!body) {
        el.textContent = 'Błąd zapytania.';
        return;
    }
    el.textContent = (body.typ === 'TEMPLATE' ? '✓ ' : '⚠ eskalacja — ') + body.wynik;
    el.title = body.wynik;
    await loadTasks();
    await loadStats();
}

async function decide(id, approving) {
    const path = approving ? `/api/ai-inbox/approve?id=${id}` : `/api/ai-inbox/reject?id=${id}`;
    const {ok, status, body} = await api(path, {method: 'POST'});
    if (!ok) {
        alert(`Odmowa (HTTP ${status}): ${body ? body.error : 'nieznany błąd'}`);
    }
    await loadInbox();
    await loadStats();
}

async function loadAiStatus() {
    const {body} = await api('/api/demo/ai-status');
    renderAiStatus(body ? body.aiEnabled : true);
}

async function toggleAi() {
    const dot = document.getElementById('ai-status-dot');
    const currentlyOn = dot.classList.contains('dot-on');
    const path = currentlyOn ? '/api/demo/ai-disable' : '/api/demo/ai-enable';
    const {body} = await api(path, {method: 'POST'});
    renderAiStatus(body ? body.aiEnabled : !currentlyOn);
}

function renderAiStatus(enabled) {
    const dot = document.getElementById('ai-status-dot');
    const label = document.getElementById('ai-status-label');
    dot.className = enabled ? 'dot dot-on' : 'dot dot-off';
    label.textContent = enabled ? 'AI połączone' : 'AI wyłączone (symulacja)';
}

async function resetDemo() {
    const btn = document.getElementById('reset-btn');
    btn.disabled = true;
    document.getElementById('activity-log').textContent = '';
    document.getElementById('case-status-result').innerHTML = '';
    await api('/api/demo/reset', {method: 'POST'});
    await loadMails();
    await loadInbox();
    await loadTasks();
    await loadStats();
    btn.disabled = false;
}

function appendLogLine(line) {
    const el = document.getElementById('activity-log');
    el.textContent += (el.textContent ? '\n' : '') + line;
    el.scrollTop = el.scrollHeight;
}

async function loadLogHistory() {
    const {body} = await api('/api/log');
    const el = document.getElementById('activity-log');
    el.textContent = (body || []).join('\n');
    el.scrollTop = el.scrollHeight;
}

function connectLogStream() {
    new EventSource('/api/log/stream').onmessage = event => appendLogLine(event.data);
}

document.getElementById('run-triage-btn').addEventListener('click', runTriage);
document.getElementById('cs-run-btn').addEventListener('click', runCaseStatus);
document.querySelectorAll('.preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.getElementById('cs-sender').value = btn.dataset.sender;
        document.getElementById('cs-text').value = btn.dataset.text;
    });
});
document.getElementById('ai-toggle-btn').addEventListener('click', toggleAi);
document.getElementById('reset-btn').addEventListener('click', resetDemo);
initTabs();

loadMails();
loadInbox();
loadTasks();
loadStats();
loadAiStatus();
loadLogHistory();
connectLogStream();
