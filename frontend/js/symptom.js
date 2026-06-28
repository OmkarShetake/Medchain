// symptom.js - AI Symptom Checker logic

let selectedLanguage = 'English';

document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();
});

function setLanguage(lang) {
    selectedLanguage = lang;
    showToast(`Language set to ${lang}`, 'success');
}

function setSymptom(symptom) {
    document.getElementById('symptoms').value = symptom;
}

async function checkSymptoms() {
    const symptoms = document.getElementById('symptoms').value.trim();

    if (!symptoms) {
        showToast('Please describe your symptoms', 'error');
        return;
    }

    try {
        const result = await apiCall('/ai/symptom-check', {
            method: 'POST',
            body: JSON.stringify({ symptoms, language: selectedLanguage })
        });
        displayResults(result);
    } catch (error) {
        showToast('Failed to check symptoms: ' + error.message, 'error');
    }
}

function displayResults(result) {
    const severityColors = {
        'MILD':     'var(--success)',
        'MODERATE': 'var(--warning)',
        'SEVERE':   'var(--danger)'
    };

    let html = `
        <div class="card" style="max-width:800px;margin:0 auto;">
            <div style="display:flex;align-items:center;gap:1rem;margin-bottom:1.5rem;">
                <h2>Analysis Results</h2>
                <span style="background:${severityColors[result.severity]};color:white;
                    padding:0.25rem 1rem;border-radius:20px;font-weight:600;">
                    ${result.severity}
                </span>
            </div>
    `;

    if (result.emergencyWarning) {
        html += `
            <div style="background:rgba(220,38,38,0.1);border:2px solid var(--danger);
                border-radius:8px;padding:1rem;margin-bottom:1.5rem;">
                <h3 style="color:var(--danger);">🚨 EMERGENCY WARNING</h3>
                <p><strong>${result.emergencyReason}</strong></p>
                <p>Please seek immediate medical attention or call 108</p>
            </div>
        `;
    }

    if (result.medicines && result.medicines.length > 0) {
        html += `<h3>Recommended Medicines</h3>`;
        result.medicines.forEach(med => {
            html += `
                <div class="card" style="margin-bottom:1rem;background:var(--bg-secondary);">
                    <div style="display:flex;justify-content:space-between;align-items:start;">
                        <div>
                            <h4 style="margin-bottom:0.25rem;">${med.name}</h4>
                            <p style="color:var(--text-secondary);margin-bottom:0.5rem;">${med.genericName}</p>
                        </div>
                        <span style="background:${med.availableAs === 'OTC' ? 'var(--success)' : 'var(--warning)'};
                            color:white;padding:0.25rem 0.75rem;border-radius:12px;font-size:0.875rem;">
                            ${med.availableAs}
                        </span>
                    </div>
                    <p><strong>Dosage:</strong> ${med.dosage}</p>
                    <p><strong>Usage:</strong> ${med.usage}</p>
                    ${med.warning ? `<p style="color:var(--warning);"><strong>⚠️ Warning:</strong> ${med.warning}</p>` : ''}
                </div>
            `;
        });
    }

    if (result.homeRemedies && result.homeRemedies.length > 0) {
        html += `
            <h3>Home Remedies</h3>
            <ul style="margin-bottom:1.5rem;">
                ${result.homeRemedies.map(r => `<li>${r}</li>`).join('')}
            </ul>
        `;
    }

    if (result.doctorAdvice) {
        html += `
            <div style="background:rgba(37,99,235,0.1);border-left:4px solid var(--primary);
                padding:1rem;border-radius:8px;">
                <p><strong>👨‍⚕️ Doctor's Advice:</strong> ${result.doctorAdvice}</p>
            </div>
        `;
    }

    html += `</div>`;

    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = html;
    resultsDiv.classList.remove('hidden');
    resultsDiv.scrollIntoView({ behavior: 'smooth' });
}
