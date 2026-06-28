// MedChain - Medicine Verification

let videoStream = null;
let selectedImageFile = null;
let nameSearchTimeout = null;

// ─── Tab Switching ────────────────────────────────────────────────────────────

function switchMethod(method) {
    ['batch', 'name', 'qr', 'image'].forEach(m => {
        document.getElementById(m + 'Input').classList.add('hidden');
    });
    document.getElementById(method + 'Input').classList.remove('hidden');

    document.querySelectorAll('.method-btn').forEach((btn, i) => {
        btn.classList.remove('active');
    });
    const idx = ['batch', 'name', 'qr', 'image'].indexOf(method);
    document.querySelectorAll('.method-btn')[idx].classList.add('active');

    stopCamera();
    document.getElementById('verificationResult').classList.add('hidden');
}

function switchQrMethod(type) {
    document.getElementById('qrManual').classList.toggle('hidden', type !== 'manual');
    document.getElementById('qrCamera').classList.toggle('hidden', type !== 'camera');
    document.querySelectorAll('.method-btn-sm').forEach((btn, i) => {
        btn.classList.toggle('active', (i === 0 && type === 'manual') || (i === 1 && type === 'camera'));
    });
    if (type !== 'camera') stopCamera();
}

// ─── 1. Batch Number Verification ────────────────────────────────────────────

async function verifyByBatch() {
    const batch = document.getElementById('batchNumberInput').value.trim();
    if (!batch) { showToast('Please enter a batch number', 'error'); return; }

    const location = await tryGetLocation('shareLocationBatch');

    try {
        showLoading();
        const params = new URLSearchParams();
        if (location) { params.append('lat', location.lat); params.append('lng', location.lng); }

        const result = await fetch(`${API_BASE_URL}/verify/batch/${encodeURIComponent(batch)}?${params}`)
            .then(res => res.json());

        hideLoading();
        displayResult(result);
        saveHistory('BATCH:' + batch, result);
    } catch (e) {
        hideLoading();
        showToast('Verification failed: ' + e.message, 'error');
    }
}

// ─── 2. Name Search ───────────────────────────────────────────────────────────

function handleNameInput(value) {
    clearTimeout(nameSearchTimeout);
    const resultsDiv = document.getElementById('nameSearchResults');

    if (value.trim().length < 2) {
        resultsDiv.classList.add('hidden');
        return;
    }

    nameSearchTimeout = setTimeout(() => searchMedicineByName(value.trim()), 400);
}

async function searchMedicineByName(name) {
    const resultsDiv = document.getElementById('nameSearchResults');
    try {
        resultsDiv.innerHTML = '<div style="padding:1rem;text-align:center;color:#64748b">Searching...</div>';
        resultsDiv.classList.remove('hidden');

        const results = await fetch(`${API_BASE_URL}/verify/search?name=${encodeURIComponent(name)}`)
            .then(res => res.json());

        if (!results || results.length === 0) {
            resultsDiv.innerHTML = '<div style="padding:1rem;text-align:center;color:#64748b">No medicines found. If your medicine isn\'t listed, it may be unregistered or fake.</div>';
            return;
        }

        resultsDiv.innerHTML = results.map(m => `
            <div class="search-result-item" onclick="showMedicineDetail(${JSON.stringify(m).replace(/"/g, '&quot;')})">
                <div class="search-result-name">${m.name}
                    <span class="status-badge status-${m.status}" style="margin-left:0.5rem">${m.status}</span>
                </div>
                <div class="search-result-meta">
                    ${m.genericName ? `Generic: ${m.genericName} &nbsp;|&nbsp;` : ''}
                    Batch: ${m.batchNumber} &nbsp;|&nbsp;
                    Expires: ${formatDate(m.expiryDate)}
                    ${m.category ? ` &nbsp;|&nbsp; ${m.category}` : ''}
                </div>
            </div>
        `).join('');
    } catch (e) {
        resultsDiv.innerHTML = '<div style="padding:1rem;color:#dc2626">Search failed. Please try again.</div>';
    }
}

function showMedicineDetail(medicine) {
    const resultDiv = document.getElementById('verificationResult');
    const isRecalled = medicine.status === 'RECALLED';
    const isExpired = medicine.status === 'EXPIRED' || new Date(medicine.expiryDate) < new Date();

    const verdict = isRecalled ? 'recalled' : isExpired ? 'expired' : 'genuine';
    const icon = isRecalled ? '🚨' : isExpired ? '⚠️' : '✅';
    const title = isRecalled ? '🚨 Banned Medicine - Do Not Use' : isExpired ? 'Medicine Expired' : 'Registered Medicine Found';

    resultDiv.innerHTML = `
        <div class="result-card ${verdict}">
            <div class="result-header">
                <div class="result-icon">${icon}</div>
                <div class="result-verdict">
                    <h2 class="verdict-title ${verdict}">${title}</h2>
                    <p class="confidence-note">ℹ️ Compare these details with what's printed on your medicine box</p>
                </div>
            </div>
            <div class="medicine-details">
                <h3>Registered Details</h3>
                ${row('Name', medicine.name)}
                ${row('Generic Name', medicine.genericName)}
                ${row('Batch Number', medicine.batchNumber)}
                ${row('Category', medicine.category)}
                ${row('Composition', medicine.composition)}
                ${row('Expiry Date', formatDate(medicine.expiryDate))}
                ${row('Side Effects', medicine.sideEffects)}
                ${row('Storage', medicine.storageInstructions)}
            </div>
            ${isRecalled ? '<div class="warning-box danger">🚨 This medicine has been <strong>BANNED</strong>. Do not use it. Return it to the pharmacy immediately.</div>' : ''}
            ${isExpired ? '<div class="warning-box">⚠️ This medicine has expired. Do not use it.</div>' : ''}
            ${!isRecalled && !isExpired ? '<div class="warning-box" style="background:#f0fdf4;border-color:#16a34a;color:#166534">✅ Details match? Your medicine appears legitimate. If anything looks different on your box, report it.</div>' : ''}
            <div class="result-actions">
                <button class="btn btn-primary" onclick="verifyAnother()">Check Another</button>
                <button class="btn btn-danger" onclick="window.location.href=\'report.html\'">Report Suspicious</button>
            </div>
        </div>
    `;
    resultDiv.classList.remove('hidden');
    resultDiv.scrollIntoView({ behavior: 'smooth' });
}

function row(label, value) {
    if (!value) return '';
    return `<div class="detail-row"><span class="detail-label">${label}:</span><span class="detail-value">${value}</span></div>`;
}

// ─── 3. QR Code Verification ──────────────────────────────────────────────────

async function verifyByQR() {
    const qrCode = document.getElementById('qrCodeInput').value.trim();
    if (!qrCode) { showToast('Please enter a QR code', 'error'); return; }

    const location = await tryGetLocation('shareLocationQr');

    try {
        showLoading();
        const params = new URLSearchParams();
        if (location) { params.append('lat', location.lat); params.append('lng', location.lng); }

        const result = await fetch(`${API_BASE_URL}/verify/${encodeURIComponent(qrCode)}?${params}`, {
            headers: { 'User-Agent': navigator.userAgent }
        }).then(res => res.json());

        hideLoading();
        displayResult(result);
        saveHistory(qrCode, result);
    } catch (e) {
        hideLoading();
        showToast('Verification failed: ' + e.message, 'error');
    }
}

// ─── 4. Image / Photo Scan ────────────────────────────────────────────────────

function handleImageSelect(event) {
    const file = event.target.files[0];
    if (!file) return;
    loadImagePreview(file);
}

function handleDrop(event) {
    event.preventDefault();
    const file = event.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) loadImagePreview(file);
}

function loadImagePreview(file) {
    selectedImageFile = file;
    const preview = document.getElementById('imagePreview');
    const placeholder = document.getElementById('uploadPlaceholder');
    const reader = new FileReader();
    reader.onload = e => {
        preview.src = e.target.result;
        preview.classList.remove('hidden');
        placeholder.classList.add('hidden');
    };
    reader.readAsDataURL(file);
    document.getElementById('scanImageBtn').disabled = false;
}

async function scanImage() {
    if (!selectedImageFile) { showToast('Please select an image first', 'error'); return; }

    const formData = new FormData();
    formData.append('image', selectedImageFile);

    const token = getAuthToken();
    if (!token) { showToast('Please login to use image scan', 'error'); return; }

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/ai/image-scan`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        }).then(res => res.json());
        hideLoading();
        displayImageResult(response);
    } catch (e) {
        hideLoading();
        showToast('Image scan failed: ' + e.message, 'error');
    }
}

function displayImageResult(result) {
    const resultDiv = document.getElementById('verificationResult');
    const verdict = result.verdict?.toLowerCase() || 'suspicious';

    const icons    = { genuine: '✅', suspicious: '⚠️', fake: '🚨', invalid: '🚫' };
    const titles   = { genuine: 'Packaging Looks Authentic', suspicious: 'Suspicious Packaging', fake: 'Likely Fake Packaging', invalid: 'Not a Medicine Image' };
    const cardClass = { genuine: 'genuine', suspicious: 'expired', fake: 'recalled', invalid: 'not_found' };

    resultDiv.innerHTML = `
        <div class="result-card ${cardClass[verdict] || 'expired'}">
            <div class="result-header">
                <div class="result-icon">${icons[verdict] || '⚠️'}</div>
                <div class="result-verdict">
                    <h2 class="verdict-title">${titles[verdict] || 'Analysis Complete'}</h2>
                    ${verdict !== 'invalid' ? `<span class="confidence-badge">AI Confidence: ${result.confidence || 0}%</span>` : ''}
                </div>
            </div>
            ${verdict === 'invalid' ? `
                <p style="font-size:1.1rem;color:#64748b;margin:1rem 0">This is not a medicine image.</p>
                <p style="color:#94a3b8;font-size:0.9rem">Upload a photo of a medicine box, strip, bottle, or blister pack.</p>
            ` : `
                <div class="medicine-details">
                    <h3>AI Findings</h3>
                    ${(result.findings || []).map(f => `<div class="detail-row">✓ ${f}</div>`).join('')}
                    ${(result.redFlags || []).length > 0 ? `
                    <h3 style="color:#dc2626;margin-top:1rem">⚠️ Red Flags</h3>
                    ${result.redFlags.map(f => `<div class="detail-row" style="color:#dc2626">✗ ${f}</div>`).join('')}
                    ` : ''}
                </div>
                <div class="warning-box ${verdict === 'fake' ? 'danger' : ''}">
                    <strong>Recommendation:</strong> ${result.recommendation || 'Consult a pharmacist.'}
                </div>
            `}
            <div class="result-actions">
                <button class="btn btn-primary" onclick="verifyAnother()">Check Another</button>
                ${verdict !== 'invalid' && verdict !== 'genuine' ? `<button class="btn btn-danger" onclick="window.location.href='report.html'">Report Suspicious</button>` : ''}
            </div>
        </div>
    `;
    resultDiv.classList.remove('hidden');
    resultDiv.scrollIntoView({ behavior: 'smooth' });
}

// ─── Display QR/Batch Result ──────────────────────────────────────────────────

function displayResult(result) {
    const resultDiv = document.getElementById('verificationResult');
    const verdict = result.verdict?.toLowerCase() || 'not_found';

    const configs = {
        genuine:   { icon: '✅', title: 'Verified Genuine',             desc: 'This medicine is registered and authentic' },
        expired:   { icon: '⚠️', title: 'Medicine Expired',             desc: 'This medicine has expired and should not be used' },
        recalled:  { icon: '🚨', title: 'Banned Medicine — Do Not Use', desc: 'This medicine batch has been banned' },
        not_found: { icon: '❓', title: 'Not Found in Database',         desc: 'This code is not registered. This may indicate a fake medicine!' },
    };
    const cfg  = configs[verdict] || configs.not_found;
    const conf = result.confidence || 0;
    const confClass = conf >= 80 ? 'high' : conf >= 50 ? 'medium' : 'low';
    const medicineName = result.medicine?.name || 'Unknown';

    let html = `
        <div class="result-card ${verdict}">
            <div class="result-header">
                <div class="result-icon">${cfg.icon}</div>
                <div class="result-verdict">
                    <h2 class="verdict-title ${verdict}">${cfg.title}</h2>
                    <div class="confidence-meter">
                        <div class="confidence-meter-label">
                            <span>Confidence</span><span>${conf}%</span>
                        </div>
                        <div class="confidence-meter-bar">
                            <div class="confidence-meter-fill ${confClass}" style="width:${conf}%"></div>
                        </div>
                    </div>
                    ${verdict === 'genuine' && conf < 100 ? '<p class="confidence-note">ℹ️ Verified by batch number — slightly lower than QR scan</p>' : ''}
                </div>
            </div>
            <p>${cfg.desc}</p>
    `;

    if (result.medicine) {
        const days = result.expiryInfo?.daysUntilExpiry ?? 0;
        const pct  = Math.max(0, Math.min(100, (days / 365) * 100));
        const cls  = days < 30 ? 'danger' : days < 90 ? 'warning' : '';

        html += `
            <div class="medicine-details">
                <h3>Medicine Details</h3>
                ${row('Name', result.medicine.name)}
                ${row('Generic Name', result.medicine.genericName)}
                ${row('Batch Number', result.medicine.batchNumber)}
                ${row('Category', result.medicine.category)}
                ${row('Composition', result.medicine.composition)}
                ${row('Expiry Date', formatDate(result.medicine.expiryDate))}
                ${row('Description', result.medicine.description)}
                ${row('Side Effects', result.medicine.sideEffects)}
            </div>
            <div class="expiry-progress">
                <div style="display:flex;justify-content:space-between;margin-bottom:.5rem">
                    <span>Days until expiry:</span>
                    <strong>${days} days</strong>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar-fill ${cls}" style="width:${pct}%"></div>
                </div>
            </div>
        `;

        if (result.manufacturer) {
            html += `
                <div class="medicine-details" style="margin-top:1rem">
                    <h3>Manufacturer</h3>
                    ${row('Company', result.manufacturer.companyName)}
                    ${result.manufacturer.city ? row('Location', result.manufacturer.city + ', ' + result.manufacturer.state) : ''}
                    ${row('Verified', result.manufacturer.isVerified ? '✅ Yes' : '❌ No')}
                </div>
            `;
        }
    }

    if (result.warning) {
        html += `<div class="warning-box ${verdict === 'recalled' ? 'danger' : ''}"><strong>⚠️</strong> ${result.warning}</div>`;
    }

    html += `
        <div class="scan-count">
            <div class="scan-count-number">${result.scanCount || 0}</div>
            <div>Times Verified</div>
        </div>
        <div class="result-actions">
            <button class="btn btn-primary" onclick="verifyAnother()">Verify Another</button>
            <button class="btn-share" onclick="shareResult('${verdict}', '${medicineName.replace(/'/g,"\\'")}', ${conf})">📤 Share</button>
            ${result.reportButton ? `<button class="btn btn-danger" onclick="window.location.href='report.html'">Report as Fake</button>` : ''}
        </div>
        </div>
    `;

    resultDiv.innerHTML = html;
    resultDiv.classList.remove('hidden');
    resultDiv.scrollIntoView({ behavior: 'smooth' });
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function shareResult(verdict, medicineName, confidence) {
    const verdictText = { genuine: '✅ GENUINE', expired: '⚠️ EXPIRED', recalled: '🚨 BANNED', not_found: '❓ NOT FOUND' };
    const text = `MedChain Verification Result\n${verdictText[verdict] || verdict.toUpperCase()}\nMedicine: ${medicineName}\nConfidence: ${confidence}%\nVerified at: ${new Date().toLocaleString('en-IN')}\n\nVerify your medicines free at MedChain`;
    if (navigator.share) {
        navigator.share({ title: 'MedChain Verification', text }).catch(() => {});
    } else {
        navigator.clipboard.writeText(text)
            .then(() => showToast('Result copied to clipboard! 📋', 'success'))
            .catch(() => showToast('Could not share', 'error'));
    }
}

function verifyAnother() {
    document.getElementById('verificationResult').classList.add('hidden');
    document.getElementById('batchNumberInput').value = '';
    document.getElementById('nameSearchInput').value = '';
    document.getElementById('qrCodeInput').value = '';
    document.getElementById('nameSearchResults').classList.add('hidden');
    selectedImageFile = null;
    document.getElementById('imagePreview').classList.add('hidden');
    document.getElementById('uploadPlaceholder').classList.remove('hidden');
    document.getElementById('scanImageBtn').disabled = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function saveHistory(key, result) {
    let history = JSON.parse(localStorage.getItem('scanHistory') || '[]');
    history.unshift({ key, verdict: result.verdict, medicineName: result.medicine?.name || 'Unknown', timestamp: new Date().toISOString() });
    history = history.slice(0, 10);
    localStorage.setItem('scanHistory', JSON.stringify(history));
    loadScanHistory();
}

function loadScanHistory() {
    const history = JSON.parse(localStorage.getItem('scanHistory') || '[]');
    const div = document.getElementById('scanHistory');
    if (history.length === 0) { div.innerHTML = '<p class="no-data">No verification history yet</p>'; return; }
    div.innerHTML = history.map(s => `
        <div class="history-item">
            <div class="history-info">
                <div class="history-qr">${s.key}</div>
                <div class="history-result ${s.verdict?.toLowerCase()}">${s.verdict}</div>
                <div>${s.medicineName}</div>
            </div>
            <div class="history-date">${formatDateTime(s.timestamp)}</div>
        </div>
    `).join('');
}

async function tryGetLocation(checkboxId) {
    const checkbox = document.getElementById(checkboxId);
    if (!checkbox?.checked) return null;
    try { return await getLocation(); } catch { return null; }
}

// Camera (QR scanning)
async function startCamera() {
    try {
        videoStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
        const video = document.getElementById('video');
        video.srcObject = videoStream;
        video.setAttribute('playsinline', true); // iOS fix
        await video.play();
        showToast('Camera started. Point at QR code', 'success');
        requestAnimationFrame(scanFrame);
    } catch (e) {
        showToast('Camera access denied: ' + e.message, 'error');
    }
}

function stopCamera() {
    if (videoStream) {
        videoStream.getTracks().forEach(t => t.stop());
        videoStream = null;
    }
}

function scanFrame() {
    if (!videoStream) return; // stopped

    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');

    if (video.readyState !== video.HAVE_ENOUGH_DATA) {
        requestAnimationFrame(scanFrame);
        return;
    }

    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

    // Use jsQR if available
    if (typeof jsQR === 'function') {
        const code = jsQR(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: 'dontInvert'
        });

        if (code && code.data) {
            // Only process MedChain QR codes
            if (!code.data.startsWith('MEDCHAIN:')) {
                // Keep scanning, ignore non-MedChain codes
                requestAnimationFrame(scanFrame);
                return;
            }
            stopCamera();
            switchQrMethod('manual');
            document.getElementById('qrCodeInput').value = code.data;
            showToast('MedChain QR code detected!', 'success');
            verifyByQR();
            return;
        }
    }

    requestAnimationFrame(scanFrame);
}
