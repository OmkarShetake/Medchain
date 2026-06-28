// interactions.js - Drug Interaction Checker logic

let medicines = [];

document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();
});

function addMedicine() {
    const input = document.getElementById('medicineInput');
    const medicine = input.value.trim();
    if (!medicine) return;
    if (medicines.includes(medicine)) {
        showToast('Medicine already added', 'info');
        return;
    }
    medicines.push(medicine);
    input.value = '';
    updateMedicineList();
}

function removeMedicine(medicine) {
    medicines = medicines.filter(m => m !== medicine);
    updateMedicineList();
}

function updateMedicineList() {
    const list = document.getElementById('medicineList');
    if (medicines.length === 0) {
        list.innerHTML = '<p style="color:var(--text-secondary);">No medicines added yet</p>';
        document.getElementById('checkBtn').disabled = true;
        return;
    }
    list.innerHTML = medicines.map(med => `
        <div style="background:var(--primary);color:white;padding:0.5rem 1rem;
            border-radius:20px;display:flex;align-items:center;gap:0.5rem;">
            <span>${med}</span>
            <button onclick="removeMedicine('${med}')"
                style="background:none;border:none;color:white;cursor:pointer;font-size:1.2rem;line-height:1;">×</button>
        </div>
    `).join('');
    document.getElementById('checkBtn').disabled = medicines.length < 2;
}

function clearAll() {
    medicines = [];
    updateMedicineList();
    document.getElementById('results').classList.add('hidden');
}

async function checkInteractions() {
    if (medicines.length < 2) {
        showToast('Add at least 2 medicines', 'error');
        return;
    }
    try {
        const result = await apiCall('/ai/drug-interactions', {
            method: 'POST',
            body: JSON.stringify({ medicines })
        });
        displayResults(result);
    } catch (error) {
        showToast('Failed to check interactions: ' + error.message, 'error');
    }
}

function displayResults(result) {
    const riskColors = {
        'LOW':      'var(--success)',
        'MEDIUM':   'var(--warning)',
        'HIGH':     'var(--danger)',
        'CRITICAL': 'var(--danger)',
        'UNKNOWN':  'var(--text-secondary)'
    };
    const severityColors = {
        'NONE':      'var(--success)',
        'MILD':      'var(--info)',
        'MODERATE':  'var(--warning)',
        'SEVERE':    'var(--danger)',
        'DANGEROUS': 'var(--danger)'
    };

    let html = `
        <div class="card" style="max-width:800px;margin:0 auto;">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:1.5rem;">
                <h2>Interaction Analysis</h2>
                <div>
                    <span style="background:${riskColors[result.overallRisk]};color:white;
                        padding:0.5rem 1rem;border-radius:20px;font-weight:600;">
                        ${result.overallRisk} RISK
                    </span>
                    <span style="margin-left:0.5rem;font-size:2rem;">
                        ${result.safeCombination ? '✅' : '⚠️'}
                    </span>
                </div>
            </div>
            <div style="background:${result.safeCombination ? 'rgba(22,163,74,0.1)' : 'rgba(220,38,38,0.1)'};
                border-left:4px solid ${result.safeCombination ? 'var(--success)' : 'var(--danger)'};
                padding:1rem;border-radius:8px;margin-bottom:1.5rem;">
                <p><strong>${result.summary}</strong></p>
            </div>
    `;

    if (result.interactions && result.interactions.length > 0) {
        html += `<h3>Detected Interactions</h3>`;
        result.interactions.forEach(i => {
            html += `
                <div class="card" style="margin-bottom:1rem;background:var(--bg-secondary);
                    border-left:4px solid ${severityColors[i.severity]};">
                    <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:0.5rem;">
                        <h4 style="margin:0;">${i.drug1} + ${i.drug2}</h4>
                        <span style="background:${severityColors[i.severity]};color:white;
                            padding:0.25rem 0.75rem;border-radius:12px;font-size:0.875rem;">
                            ${i.severity}
                        </span>
                    </div>
                    <p><strong>Mechanism:</strong> ${i.mechanism}</p>
                    <p><strong>Effect:</strong> ${i.effect}</p>
                    <p><strong>Recommendation:</strong> ${i.recommendation}</p>
                    ${i.avoidCombination ? '<p style="color:var(--danger);font-weight:600;">⚠️ Avoid this combination</p>' : ''}
                </div>
            `;
        });
    } else {
        html += `
            <div style="text-align:center;padding:2rem;background:rgba(22,163,74,0.1);border-radius:8px;">
                <div style="font-size:3rem;margin-bottom:0.5rem;">✅</div>
                <h3>No Interactions Found</h3>
                <p>These medicines appear safe to take together</p>
            </div>
        `;
    }

    if (result.alternatives && result.alternatives.length > 0) {
        html += `<h3>Alternative Suggestions</h3><ul>${result.alternatives.map(a => `<li>${a}</li>`).join('')}</ul>`;
    }

    html += `</div>`;

    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = html;
    resultsDiv.classList.remove('hidden');
    resultsDiv.scrollIntoView({ behavior: 'smooth' });
}
