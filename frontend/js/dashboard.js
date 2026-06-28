// Dashboard JavaScript

let currentMedicineId = null;
let allMedicines = [];

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    const user = getCurrentUser();
    if (!user || user.role !== 'MANUFACTURER') {
        showToast('Access denied. Manufacturer account required.', 'error');
        setTimeout(() => window.location.href = 'auth.html', 2000);
        return;
    }

    // Update nav
    updateNavAuth();

    // Load manufacturer info
    loadManufacturerInfo();

    // Load initial data
    loadDashboardStats();
    loadRecentMedicines();

    // Setup tab navigation
    setupTabNavigation();

    // Setup mobile menu
    setupMobileMenu();
});

// Load manufacturer info
async function loadManufacturerInfo() {
    try {
        const user = getCurrentUser();
        document.getElementById('manufacturerName').textContent = user.name || 'Manufacturer';
    } catch (error) {
        console.error('Error loading manufacturer info:', error);
    }
}

// Load dashboard stats
async function loadDashboardStats() {
    try {
        const medicines = await apiCall('/manufacturer/medicines?size=1000');
        const recalls = await apiCall('/manufacturer/recalls');

        // Calculate stats
        const totalMedicines = medicines.totalElements || 0;
        let totalQRCodes = 0;
        let totalScans = 0;

        if (medicines.content) {
            for (const med of medicines.content) {
                const units = await apiCall(`/manufacturer/medicines/${med.id}/units?size=1`);
                totalQRCodes += units.totalElements || 0;
            }
        }

        const activeRecalls = Array.isArray(recalls) ? recalls.filter(r => r.isActive).length : 0;

        // Update UI
        animateCounter('totalMedicines', totalMedicines);
        animateCounter('totalQRCodes', totalQRCodes);
        animateCounter('totalScans', totalScans);
        animateCounter('activeRecalls', activeRecalls);
    } catch (error) {
        console.error('Error loading stats:', error);
        showToast('Failed to load statistics', 'error');
    }
}

// Load recent medicines
async function loadRecentMedicines() {
    try {
        const response = await apiCall('/manufacturer/medicines?size=5&sort=createdAt,desc');
        const container = document.getElementById('recentMedicines');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No medicines registered yet</p>';
            return;
        }

        container.innerHTML = response.content.map(med => `
            <div class="medicine-list-item">
                <div>
                    <h4>${med.name}</h4>
                    <p>${med.genericName || 'N/A'} • ${med.category}</p>
                </div>
                <button class="btn btn-outline btn-sm" onclick="viewMedicine('${med.id}')">View</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading recent medicines:', error);
        document.getElementById('recentMedicines').innerHTML = '<p class="error">Failed to load medicines</p>';
    }
}

// Load all medicines
async function loadMedicines() {
    try {
        const response = await apiCall('/manufacturer/medicines?size=100&sort=name,asc');
        allMedicines = response.content || [];

        const container = document.getElementById('medicinesList');
        if (allMedicines.length === 0) {
            container.innerHTML = '<p class="no-data">No medicines registered yet. Click "Add Medicine" to get started.</p>';
            return;
        }

        renderMedicines(allMedicines);

        // Populate medicine select for QR generation
        const select = document.getElementById('medicineSelect');
        select.innerHTML = '<option value="">-- Select Medicine --</option>' +
            allMedicines.map(med => `<option value="${med.id}">${med.name}</option>`).join('');
    } catch (error) {
        console.error('Error loading medicines:', error);
        showToast('Failed to load medicines', 'error');
    }
}

// Render medicines
function renderMedicines(medicines) {
    const container = document.getElementById('medicinesList');
    container.innerHTML = medicines.map(med => `
        <div class="medicine-card">
            <div class="medicine-header">
                <h3>${med.name}</h3>
                <span class="medicine-badge active">Active</span>
            </div>
            <div class="medicine-info">
                <p><strong>Generic:</strong> ${med.genericName || 'N/A'}</p>
                <p><strong>Category:</strong> ${med.category}</p>
                <p><strong>Dosage:</strong> ${med.dosageForm} - ${med.strength}</p>
                <p><strong>Description:</strong> ${med.description || 'N/A'}</p>
            </div>
            <div class="medicine-actions">
                <button class="btn btn-outline" onclick="viewMedicine('${med.id}')">View Details</button>
                <button class="btn btn-primary" onclick="selectMedicineForQR('${med.id}')">Generate QR</button>
            </div>
        </div>
    `).join('');
}

// Search medicines
document.getElementById('medicineSearchInput')?.addEventListener('input', (e) => {
    const query = e.target.value.toLowerCase();
    const filtered = allMedicines.filter(med =>
        med.name.toLowerCase().includes(query) ||
        (med.genericName && med.genericName.toLowerCase().includes(query)) ||
        med.category.toLowerCase().includes(query)
    );
    renderMedicines(filtered);
});

// View medicine details
function viewMedicine(id) {
    window.location.href = `#medicines`;
    switchTab('medicines');
}

// Select medicine for QR generation
function selectMedicineForQR(id) {
    document.getElementById('medicineSelect').value = id;
    switchTab('qr-codes');
    loadMedicineUnits();
}

// Load medicine units (QR codes)
async function loadMedicineUnits() {
    const medicineId = document.getElementById('medicineSelect').value;
    if (!medicineId) {
        document.getElementById('qrCodesGrid').innerHTML = '<p class="text-center text-secondary">Select a medicine to view QR codes</p>';
        document.getElementById('qrGenerationSection').classList.add('hidden');
        return;
    }

    currentMedicineId = medicineId;
    document.getElementById('qrGenerationSection').classList.remove('hidden');

    try {
        const response = await apiCall(`/manufacturer/medicines/${medicineId}/units?size=100`);
        const container = document.getElementById('qrCodesGrid');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No QR codes generated yet for this medicine</p>';
            return;
        }

        container.innerHTML = response.content.map(unit => `
            <div class="qr-card">
                ${unit.qrImageBase64 
                    ? `<img src="data:image/png;base64,${unit.qrImageBase64}" alt="QR Code" class="qr-image">`
                    : `<div class="qr-image" style="background:#f1f5f9;display:flex;align-items:center;justify-content:center;font-size:2rem">📱</div>`
                }
                <div class="qr-code">${unit.qrCode}</div>
                <div class="qr-info">Strip: ${unit.stripNumber || 'N/A'}</div>
                <div class="qr-actions">
                    <button class="btn btn-outline" onclick="copyToClipboard('${unit.qrCode}')">Copy Code</button>
                    ${unit.qrImageBase64 ? `<button class="btn btn-primary" onclick="downloadQR('${unit.qrImageBase64}', '${unit.qrCode}')">Download</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading QR codes:', error);
        showToast('Failed to load QR codes', 'error');
    }
}

// Generate QR codes
async function generateQRCodes(event) {
    event.preventDefault();

    if (!currentMedicineId) {
        showToast('Please select a medicine first', 'error');
        return;
    }

    const quantity       = parseInt(document.getElementById('quantity').value);
    const distributedCity  = document.getElementById('distributedCity').value.trim();
    const distributedState = document.getElementById('distributedState').value.trim();

    try {
        showLoading();
        const qrCodes = await apiCall(`/manufacturer/medicines/${currentMedicineId}/generate-qr`, {
            method: 'POST',
            body: JSON.stringify({ quantity, distributedCity, distributedState })
        });

        hideLoading();
        showToast(`Successfully generated ${qrCodes.length} QR codes!`, 'success');
        document.getElementById('qrGenerationForm').reset();
        loadMedicineUnits();
        loadDashboardStats();
    } catch (error) {
        hideLoading();
        console.error('Error generating QR codes:', error);
        showToast(error.message || 'Failed to generate QR codes', 'error');
    }
}

// Download QR code
function downloadQR(base64, code) {
    const link = document.createElement('a');
    link.href = `data:image/png;base64,${base64}`;
    link.download = `${code}.png`;
    link.click();
    showToast('QR code downloaded', 'success');
}

// Show add medicine modal
function showAddMedicineModal() {
    document.getElementById('addMedicineModal').classList.add('active');
}

// Close add medicine modal
function closeAddMedicineModal() {
    document.getElementById('addMedicineModal').classList.remove('active');
    document.getElementById('addMedicineForm').reset();
}

// Add medicine
async function addMedicine(event) {
    event.preventDefault();

    const mfgDate = document.getElementById('medicineManufDate').value;
    const expDate = document.getElementById('medicineExpiryDate').value;

    if (new Date(expDate) <= new Date(mfgDate)) {
        showToast('Expiry date must be after manufacturing date', 'error');
        return;
    }

    const data = {
        name:               document.getElementById('medicineName').value.trim(),
        genericName:        document.getElementById('genericName').value.trim() || null,
        batchNumber:        document.getElementById('medicineBatchNumber').value.trim(),
        category:           document.getElementById('category').value,
        manufacturingDate:  mfgDate,
        expiryDate:         expDate,
        composition:        document.getElementById('composition').value.trim() || null,
        quantityProduced:   document.getElementById('quantityProduced').value
                                ? parseInt(document.getElementById('quantityProduced').value) : null,
        description:        document.getElementById('description').value.trim() || null
    };

    try {
        showLoading();
        await apiCall('/manufacturer/medicines', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        hideLoading();
        showToast('Medicine added successfully!', 'success');
        closeAddMedicineModal();
        loadDashboardStats();
        loadRecentMedicines();
        if (document.getElementById('medicines').classList.contains('active')) {
            loadMedicines();
        }
    } catch (error) {
        hideLoading();
        console.error('Error adding medicine:', error);
        showToast(error.message || 'Failed to add medicine', 'error');
    }
}

// Load banned medicines (recalls)
async function loadRecalls() {
    try {
        const recalls = await apiCall('/manufacturer/recalls');
        const container = document.getElementById('recallsList');

        if (!Array.isArray(recalls) || recalls.length === 0) {
            container.innerHTML = '<p class="no-data">No banned medicines ✅</p>';
            return;
        }

        container.innerHTML = recalls.map(recall => `
            <div class="recall-card severity-${(recall.severity || 'low').toLowerCase()}">
                <div class="recall-header">
                    <span class="recall-severity">${recall.severity || 'N/A'}</span>
                    <span class="recall-date">${recall.createdAt ? formatDate(recall.createdAt) : 'N/A'}</span>
                </div>
                <h3>${recall.medicine?.name || 'Unknown Medicine'}</h3>
                <p><strong>Reason:</strong> ${recall.reason || 'N/A'}</p>
                <p><strong>Batch:</strong> ${recall.medicine?.batchNumber || 'All batches'}</p>
                <div class="recall-footer">
                    <span>Status: ${recall.isActive ? '🔴 Active Ban' : '✅ Lifted'}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading banned medicines:', error);
        document.getElementById('recallsList').innerHTML = '<p class="error">Failed to load</p>';
    }
}

// Setup tab navigation
function setupTabNavigation() {
    const links = document.querySelectorAll('.sidebar-link');
    links.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const tab = link.dataset.tab;
            switchTab(tab);
        });
    });
}

// Switch tab
function switchTab(tabName) {
    // Update active link
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.classList.remove('active');
        if (link.dataset.tab === tabName) {
            link.classList.add('active');
        }
    });

    // Update active content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(tabName).classList.add('active');

    // Load tab-specific data
    switch (tabName) {
        case 'medicines':
            loadMedicines();
            break;
        case 'qr-codes':
            loadMedicines(); // For select dropdown
            break;
        case 'recalls':
            loadRecalls();
            break;
    }
}

// Setup mobile menu
function setupMobileMenu() {
    const btn = document.getElementById('mobileMenuBtn');
    const nav = document.querySelector('.nav-links');

    btn?.addEventListener('click', () => {
        nav.classList.toggle('active');
    });
}

// Close modal on outside click
window.addEventListener('click', (e) => {
    const modal = document.getElementById('addMedicineModal');
    if (e.target === modal) {
        closeAddMedicineModal();
    }
});
