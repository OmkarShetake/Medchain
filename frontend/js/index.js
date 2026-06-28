// index.js - Home page logic

// Load live stats + ticker
async function loadStats() {
    try {
        const response = await fetch(API_BASE_URL + '/stats/public');
        if (response.ok) {
            const data = await response.json();
            animateCounter('statMedicines',    data.totalScans            || 0);
            animateCounter('statFakes',         data.fakeDetected          || 0);
            animateCounter('statStates',        data.statesCovered         || 28);
            animateCounter('statManufacturers', data.verifiedManufacturers || 0);
            // Update hero ticker
            const ticker = document.getElementById('tickerCount');
            if (ticker) ticker.textContent = (data.totalScans || 0).toLocaleString('en-IN');
            return;
        }
    } catch (e) { console.error('Stats load failed:', e); }
    animateCounter('statMedicines', 0);
    animateCounter('statFakes', 0);
    animateCounter('statStates', 28);
    animateCounter('statManufacturers', 0);
}

// Load banned medicines
async function loadRecalls() {
    try {
        const recalls = await apiCall('/recalls');
        const recallsList = document.getElementById('recallsList');

        if (!recalls || recalls.length === 0) {
            recallsList.innerHTML = '<p class="no-data">No active bans at this time ✅</p>';
            return;
        }

        recallsList.innerHTML = recalls.slice(0, 3).map(recall => `
            <div class="recall-card severity-${recall.severity.toLowerCase()}">
                <div class="recall-header">
                    <span class="recall-severity">${recall.severity}</span>
                    <span class="recall-date">${formatDate(recall.createdAt)}</span>
                </div>
                <h3>${recall.medicine.name}</h3>
                <p>${recall.reason}</p>
                <div class="recall-footer">
                    <span>🚫 Banned — Batch: ${recall.medicine.batchNumber}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        document.getElementById('recallsList').innerHTML = '<p class="error">Failed to load</p>';
    }
}

// Verify from home hero input
function verifyFromHome() {
    const qrCode = document.getElementById('qrInput').value.trim();
    if (qrCode) {
        window.location.href = `verify.html?qr=${encodeURIComponent(qrCode)}`;
    } else {
        window.location.href = 'verify.html';
    }
}

// Search medicines
async function searchMedicines() {
    const query = document.getElementById('medicineSearch').value.trim();
    if (!query) return;

    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/medicines/search?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        const resultsDiv = document.getElementById('searchResults');

        if (data.content && data.content.length > 0) {
            resultsDiv.innerHTML = data.content.map(med => `
                <div class="medicine-card">
                    <h3>${med.name}</h3>
                    <p><strong>Generic:</strong> ${med.genericName || 'N/A'}</p>
                    <p><strong>Category:</strong> ${med.category || 'N/A'}</p>
                    <p><strong>Manufacturer:</strong> ${med.manufacturer?.companyName || 'N/A'}</p>
                </div>
            `).join('');
        } else {
            resultsDiv.innerHTML = '<p class="no-data">No medicines found</p>';
        }
    } catch (error) {
        showToast('Failed to search medicines', 'error');
    } finally {
        hideLoading();
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();
    loadStats();
    loadRecalls();
});
