// chemist.js - Chemist Dashboard

let drugList = [];
let nameSearchTimeout = null;

// ── Init ──────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    const user = getCurrentUser();
    if (!user || user.role !== 'CHEMIST') {
        showToast('Access denied. Chemist account required.', 'error');
        setTimeout(() => window.location.href = 'auth.html', 2000);
        return;
    }

    updateNavAuth();
    document.getElementById('chemistName').textContent = user.name || 'Chemist';
    document.getElementById('welcomeName').textContent = user.name || 'Chemist';

    setupTabNavigation();
    loadOverviewStats();
    loadRecentBans();
    loadRecentScans();
});

// ── Tab Navigation ────────────────────────────────────────────────────────────

function setupTabNavigation() {
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            switchTab(link.dataset.tab);
        });
    });
}

function switchTab(tabName) {
    document.querySelectorAll('.sidebar-link').forEach(l => {
        l.classList.toggle('active', l.dataset.tab === tabName);
    });
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(tabName).classList.add('active');

    switch (tabName) {
        case 'recalls':     loadBannedMedicines(); break;
        case 'reports':     loadMyReports(); break;
        case 'interactions': break; // handled on demand
    }
}

// ── Overview ──────────────────────────────────────────────────────────────────

async function loadOverviewStats() {
    try {
        // Unread notifications
        const notifData = await apiCall('/notifications/unread-count');
        animateCounter('unreadNotifs', notifData.count || 0);

        // Active bans
        const recalls = await fetch(API_BASE_URL + '/recalls').then(r => r.json());
        animateCounter('activeBans', Array.isArray(recalls) ? recalls.filter(r => r.isActive).length : 0);

        // My reports
        const reports = await apiCall('/reports?size=1');
        animateCounter('myReports', reports.totalElements || 0);

        // Today's verifications from localStorage
        const history = JSON.parse(localStorage.getItem('scanHistory') || '[]');
        const today = new Date().toDateString();
        const todayScans = history.filter(s => new Date(s.timestamp).toDateString() === today).length;
        animateCounter('totalVerified', todayScans);
    } catch (e) {
        console.error('Error loading stats:', e);
    }
}

async function loadRecentBans() {
    const container = document.getElementById('recentBans');
    try {
        const recalls = await fetch(API_BASE_URL + '/recalls').then(r => r.json());
        const active = Array.isArray(recalls) ? recalls.filter(r => r.isActive).slice(0, 3) : [];

        if (active.length === 0) {
            container.innerHTML = '<p class="no-data">✅ No active bans at this time</p>';
            return;
        }

        container.innerHTML = active.map(r => `
            <div class="activity-item" style="border-left-color:var(--danger)">
                <h4>🚫 ${r.medicine?.name || 'Unknown'}</h4>
                <p>${r.reason || ''}</p>
                <div class="activity-time">Batch: ${r.medicine?.batchNumber || 'N/A'} · ${r.severity}</div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<p class="error">Failed to load</p>';
    }
}

function loadRecentScans() {
    const container = document.getElementById('recentScans');
    const history = JSON.parse(localStorage.getItem('scanHistory') || '[]');

    if (history.length === 0) {
        container.innerHTML = '<p class="no-data">No recent verifications</p>';
        return;
    }

    container.innerHTML = history.slice(0, 5).map(s => {
        const icon = s.verdict === 'GENUINE' ? '✅' : s.verdict === 'EXPIRED' ? '⚠️' : s.verdict === 'RECALLED' ? '🚫' : '❓';
        return `
            <div class="activity-item">
                <h4>${icon} ${s.medicineName || 'Unknown'}</h4>
                <p style="font-size:0.8rem;color:var(--text-secondary)">${s.key}</p>
                <div class="activity-time">${formatDateTime(s.timestamp)}</div>
            </div>
        `;
    }).join('');
}

// ── Verify ────────────────────────────────────────────────────────────────────

function switchVerifyMode(mode) {
    ['batch', 'qr', 'name'].forEach(m => {
        document.getElementById('verify' + m.charAt(0).toUpperCase() + m.slice(1)).classList.toggle('hidden', m !== mode);
    });
    document.querySelectorAll('.chemist-tab-btn').forEach((btn, i) => {
        btn.classList.toggle('active', ['batch','qr','name'][i] === mode);
    });
    document.getElementById('chemistVerifyResult').classList.add('hidden');
}

async function chemistVerifyBatch() {
    const batch = document.getElementById('cvBatchInput').value.trim();
    if (!batch) { showToast('Enter a batch number', 'error'); return; }
    try {
        showLoading();
        const result = await fetch(`${API_BASE_URL}/verify/batch/${encodeURIComponent(batch)}`).then(r => r.json());
        hideLoading();
        showVerifyResult(result);
        saveToHistory('BATCH:' + batch, result);
    } catch (e) { hideLoading(); showToast('Verification failed', 'error'); }
}

async function chemistVerifyQR() {
    const qr = document.getElementById('cvQRInput').value.trim();
    if (!qr) { showToast('Enter a QR code value', 'error'); return; }
    try {
        showLoading();
        const result = await fetch(`${API_BASE_URL}/verify/${encodeURIComponent(qr)}`).then(r => r.json());
        hideLoading();
        showVerifyResult(result);
        saveToHistory(qr, result);
    } catch (e) { hideLoading(); showToast('Verification failed', 'error'); }
}

function chemistNameSearch(value) {
    clearTimeout(nameSearchTimeout);
    const container = document.getElementById('cvNameResults');
    if (value.trim().length < 2) { container.innerHTML = ''; return; }

    nameSearchTimeout = setTimeout(async () => {
        try {
            const results = await fetch(`${API_BASE_URL}/verify/search?name=${encodeURIComponent(value)}`).then(r => r.json());
            if (!results || results.length === 0) {
                container.innerHTML = '<p class="no-data">No medicines found</p>';
                return;
            }
            container.innerHTML = `<div style="border:1px solid var(--border);border-radius:8px;overflow:hidden">` +
                results.map(m => `
                    <div class="chemist-search-result" onclick="showMedicineInfo(${JSON.stringify(m).replace(/"/g,'&quot;')})">
                        <strong>${m.name}</strong>
                        <span style="margin-left:0.5rem;font-size:0.75rem;padding:2px 8px;border-radius:99px;
                            background:${m.status==='ACTIVE'?'#dcfce7':m.status==='RECALLED'?'#fee2e2':'#fef3c7'};
                            color:${m.status==='ACTIVE'?'#16a34a':m.status==='RECALLED'?'#dc2626':'#d97706'}">
                            ${m.status}
                        </span>
                        <div style="font-size:0.8rem;color:var(--text-secondary);margin-top:0.2rem">
                            Batch: ${m.batchNumber} · Expires: ${formatDate(m.expiryDate)}
                        </div>
                    </div>
                `).join('') + `</div>`;
        } catch (e) { container.innerHTML = ''; }
    }, 400);
}

function showMedicineInfo(m) {
    const isRecalled = m.status === 'RECALLED';
    const isExpired  = m.status === 'EXPIRED' || new Date(m.expiryDate) < new Date();
    const verdict    = isRecalled ? 'recalled' : isExpired ? 'expired' : 'genuine';
    const icon       = isRecalled ? '🚫' : isExpired ? '⚠️' : '✅';
    const title      = isRecalled ? 'BANNED — Do Not Sell' : isExpired ? 'EXPIRED — Do Not Sell' : 'Registered Medicine';

    const div = document.getElementById('chemistVerifyResult');
    div.innerHTML = `
        <div class="chemist-result ${verdict}">
            <div class="result-verdict-row">
                <span class="result-verdict-icon">${icon}</span>
                <h2 class="result-verdict-title ${verdict}">${title}</h2>
            </div>
            ${isRecalled ? `<div class="alert-banner danger">🚫 This medicine is BANNED. Remove from shelf and return to distributor.</div>` : ''}
            ${isExpired  ? `<div class="alert-banner warning">⚠️ This medicine is EXPIRED. Do not sell.</div>` : ''}
            <div class="medicine-detail-grid">
                <div class="med-detail-item"><div class="med-detail-label">Name</div><div class="med-detail-value">${m.name}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Generic</div><div class="med-detail-value">${m.genericName||'N/A'}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Batch No.</div><div class="med-detail-value">${m.batchNumber}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Expiry</div><div class="med-detail-value">${formatDate(m.expiryDate)}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Category</div><div class="med-detail-value">${m.category||'N/A'}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Composition</div><div class="med-detail-value">${m.composition||'N/A'}</div></div>
            </div>
            <div style="display:flex;gap:0.75rem;margin-top:1rem">
                <button class="btn btn-outline" onclick="document.getElementById('chemistVerifyResult').classList.add('hidden')">Close</button>
                <button class="btn btn-danger" onclick="window.location.href='report.html'">Report Suspicious</button>
            </div>
        </div>`;
    div.classList.remove('hidden');
    div.scrollIntoView({ behavior: 'smooth' });
}

function showVerifyResult(result) {
    const verdict = (result.verdict || 'NOT_FOUND').toLowerCase().replace('_', '_');
    const icons = { genuine:'✅', expired:'⚠️', recalled:'🚫', not_found:'❓' };
    const titles = { genuine:'Genuine Medicine', expired:'EXPIRED — Do Not Sell', recalled:'BANNED — Do Not Sell', not_found:'Not Found in Database' };

    const div = document.getElementById('chemistVerifyResult');
    div.innerHTML = `
        <div class="chemist-result ${verdict}">
            <div class="result-verdict-row">
                <span class="result-verdict-icon">${icons[verdict]||'❓'}</span>
                <div>
                    <h2 class="result-verdict-title ${verdict}">${titles[verdict]||'Unknown'}</h2>
                    <span style="font-size:0.85rem;color:var(--text-secondary)">Confidence: ${result.confidence||0}%</span>
                </div>
            </div>
            ${result.verdict==='RECALLED' ? `<div class="alert-banner danger">🚫 BANNED medicine. Remove from shelf immediately and contact distributor.</div>` : ''}
            ${result.verdict==='EXPIRED'  ? `<div class="alert-banner warning">⚠️ Expired medicine. Do not sell. Dispose properly.</div>` : ''}
            ${result.verdict==='NOT_FOUND'? `<div class="alert-banner warning">❓ Not registered. Could be unverified or fake. Do not sell without verification.</div>` : ''}
            ${result.medicine ? `
            <div class="medicine-detail-grid">
                <div class="med-detail-item"><div class="med-detail-label">Name</div><div class="med-detail-value">${result.medicine.name}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Generic</div><div class="med-detail-value">${result.medicine.genericName||'N/A'}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Batch No.</div><div class="med-detail-value">${result.medicine.batchNumber}</div></div>
                <div class="med-detail-item"><div class="med-detail-label">Expiry</div><div class="med-detail-value">${formatDate(result.medicine.expiryDate)}</div></div>
                ${result.manufacturer ? `<div class="med-detail-item"><div class="med-detail-label">Manufacturer</div><div class="med-detail-value">${result.manufacturer.companyName}</div></div>` : ''}
                <div class="med-detail-item"><div class="med-detail-label">Times Scanned</div><div class="med-detail-value">${result.scanCount||0}</div></div>
            </div>` : ''}
            ${result.warning ? `<div class="alert-banner warning">${result.warning}</div>` : ''}
            <div style="display:flex;gap:0.75rem;margin-top:1rem">
                <button class="btn btn-outline" onclick="document.getElementById('chemistVerifyResult').classList.add('hidden')">Close</button>
                ${result.reportButton ? `<button class="btn btn-danger" onclick="window.location.href='report.html'">Report as Fake</button>` : ''}
            </div>
        </div>`;
    div.classList.remove('hidden');
    div.scrollIntoView({ behavior: 'smooth' });
}

function saveToHistory(key, result) {
    let history = JSON.parse(localStorage.getItem('scanHistory') || '[]');
    history.unshift({ key, verdict: result.verdict, medicineName: result.medicine?.name || 'Unknown', timestamp: new Date().toISOString() });
    history = history.slice(0, 20);
    localStorage.setItem('scanHistory', JSON.stringify(history));
}

// ── Banned Medicines ──────────────────────────────────────────────────────────

async function loadBannedMedicines() {
    const container = document.getElementById('bannedList');
    try {
        const recalls = await fetch(API_BASE_URL + '/recalls').then(r => r.json());
        if (!Array.isArray(recalls) || recalls.length === 0) {
            container.innerHTML = '<p class="no-data">✅ No banned medicines at this time</p>';
            return;
        }
        container.innerHTML = recalls.map(r => `
            <div class="ban-card">
                <div class="ban-card-header">
                    <h3>🚫 ${r.medicine?.name || 'Unknown'}</h3>
                    <span class="ban-severity">${r.severity}</span>
                </div>
                <p><strong>Reason:</strong> ${r.reason}</p>
                <p><strong>Batch:</strong> ${r.medicine?.batchNumber || 'All batches'}</p>
                <p><strong>Status:</strong> ${r.isActive ? '🔴 Active Ban' : '✅ Lifted'}</p>
                <p style="font-size:0.8rem;color:var(--text-secondary)">Issued: ${formatDate(r.createdAt)}</p>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<p class="error">Failed to load</p>';
    }
}

// ── My Reports ────────────────────────────────────────────────────────────────

async function loadMyReports() {
    const container = document.getElementById('myReportsList');
    try {
        const data = await apiCall('/reports?size=20&sort=createdAt,desc');
        if (!data.content || data.content.length === 0) {
            container.innerHTML = `
                <div style="grid-column:1/-1;text-align:center;padding:3rem">
                    <div style="font-size:3rem;margin-bottom:1rem">📋</div>
                    <p>No reports submitted yet</p>
                    <button class="btn btn-danger" onclick="window.location.href='report.html'" style="margin-top:1rem">
                        + Report Fake Medicine
                    </button>
                </div>`;
            return;
        }
        container.innerHTML = data.content.map(r => `
            <div class="report-card">
                <div class="report-header">
                    <span class="report-status ${r.status.toLowerCase()}">${r.status}</span>
                    <span class="report-date">${formatDate(r.createdAt)}</span>
                </div>
                <h3>${r.medicineName || 'Unknown Medicine'}</h3>
                <p>${(r.description||'').substring(0, 100)}${(r.description||'').length > 100 ? '...' : ''}</p>
                <div class="report-footer">
                    <span>📍 ${r.location || 'Unknown'}</span>
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<p class="error">Failed to load reports</p>';
    }
}

// ── Drug Interactions ─────────────────────────────────────────────────────────

function addDrug() {
    const input = document.getElementById('drugInput');
    const val = input.value.trim();
    if (!val) return;
    if (drugList.includes(val)) { showToast('Already added', 'info'); return; }
    drugList.push(val);
    input.value = '';
    renderDrugChips();
}

function removeDrug(name) {
    drugList = drugList.filter(d => d !== name);
    renderDrugChips();
}

function clearDrugs() {
    drugList = [];
    renderDrugChips();
    document.getElementById('drugResults').classList.add('hidden');
}

function renderDrugChips() {
    const container = document.getElementById('drugChips');
    container.innerHTML = drugList.length === 0
        ? '<p style="color:var(--text-secondary);font-size:0.875rem">No medicines added yet</p>'
        : drugList.map(d => `
            <div class="drug-chip">
                <span>${d}</span>
                <button onclick="removeDrug('${d}')">×</button>
            </div>`).join('');
    document.getElementById('checkDrugsBtn').disabled = drugList.length < 2;
}

async function checkDrugs() {
    if (drugList.length < 2) { showToast('Add at least 2 medicines', 'error'); return; }
    try {
        showLoading();
        const result = await apiCall('/ai/drug-interactions', {
            method: 'POST',
            body: JSON.stringify({ medicines: drugList })
        });
        hideLoading();

        const riskColor = { LOW:'var(--success)', MEDIUM:'var(--warning)', HIGH:'var(--danger)', CRITICAL:'var(--danger)', UNKNOWN:'var(--text-secondary)' };
        const sevColor  = { NONE:'var(--success)', MILD:'var(--info)', MODERATE:'var(--warning)', SEVERE:'var(--danger)', DANGEROUS:'var(--danger)' };

        let html = `
            <div class="card">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
                    <h2>Interaction Results</h2>
                    <span style="background:${riskColor[result.overallRisk]};color:white;padding:0.4rem 1rem;border-radius:20px;font-weight:700">
                        ${result.overallRisk} RISK
                    </span>
                </div>
                <div style="background:${result.safeCombination?'rgba(22,163,74,0.1)':'rgba(220,38,38,0.1)'};
                    border-left:4px solid ${result.safeCombination?'var(--success)':'var(--danger)'};
                    padding:1rem;border-radius:8px;margin-bottom:1.5rem">
                    <p style="margin:0;font-weight:600">${result.summary}</p>
                </div>`;

        if (result.interactions && result.interactions.length > 0) {
            result.interactions.forEach(i => {
                html += `
                    <div style="border:1px solid var(--border);border-left:4px solid ${sevColor[i.severity]||'var(--warning)'};border-radius:8px;padding:1rem;margin-bottom:0.75rem">
                        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:0.5rem">
                            <strong>${i.drug1} + ${i.drug2}</strong>
                            <span style="background:${sevColor[i.severity]};color:white;padding:2px 10px;border-radius:99px;font-size:0.75rem">${i.severity}</span>
                        </div>
                        <p style="margin-bottom:0.3rem"><strong>Effect:</strong> ${i.effect}</p>
                        <p style="margin:0"><strong>Recommendation:</strong> ${i.recommendation}</p>
                        ${i.avoidCombination ? '<p style="color:var(--danger);font-weight:600;margin-top:0.5rem">⚠️ Avoid this combination</p>' : ''}
                    </div>`;
            });
        } else {
            html += `<div style="text-align:center;padding:2rem;background:rgba(22,163,74,0.1);border-radius:8px">
                <div style="font-size:2.5rem">✅</div>
                <p style="font-weight:600;margin-top:0.5rem">No interactions found — safe combination</p>
            </div>`;
        }

        html += `</div>`;
        const resultsDiv = document.getElementById('drugResults');
        resultsDiv.innerHTML = html;
        resultsDiv.classList.remove('hidden');
        resultsDiv.scrollIntoView({ behavior: 'smooth' });
    } catch (e) {
        hideLoading();
        showToast('Failed to check interactions', 'error');
    }
}
