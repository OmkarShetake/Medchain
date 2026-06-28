// Admin Dashboard JavaScript

let currentReportId = null;
let hotspotsMap = null;
let charts = {};

// Initialize admin dashboard
document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    const user = getCurrentUser();
    if (!user || user.role !== 'ADMIN') {
        showToast('Access denied. Admin account required.', 'error');
        setTimeout(() => window.location.href = 'auth.html', 2000);
        return;
    }

    // Update nav
    updateNavAuth();

    // Load initial data
    loadDashboard();

    // Setup tab navigation
    setupTabNavigation();

    // Setup mobile menu
    setupMobileMenu();
});

// Load dashboard
async function loadDashboard() {
    try {
        const stats = await apiCall('/admin/dashboard');

        // Update stats
        animateCounter('totalMedicines', stats.totalMedicinesRegistered || 0);
        animateCounter('totalScans', stats.totalScansAllTime || 0);
        animateCounter('pendingReports', stats.pendingReports || 0);
        animateCounter('activeRecalls', stats.activeRecalls || 0);
        animateCounter('verifiedManufacturers', stats.verifiedManufacturers || 0);
        animateCounter('totalUsers', stats.totalUsers || stats.verifiedManufacturers || 0);

        // Load recent activity
        loadRecentReports();
        loadPendingVerifications();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showToast('Failed to load dashboard', 'error');
    }
}

// Refresh dashboard
function refreshDashboard() {
    loadDashboard();
    showToast('Dashboard refreshed', 'success');
}

// Load recent reports
async function loadRecentReports() {
    try {
        const response = await apiCall('/admin/reports?size=5&sort=createdAt,desc');
        const container = document.getElementById('recentReports');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No recent reports</p>';
            return;
        }

        container.innerHTML = response.content.map(report => `
            <div class="activity-item" onclick="switchTab('reports')" style="cursor:pointer">
                <h4>${report.medicineName || 'Unknown Medicine'}</h4>
                <p>${(report.description || '').substring(0, 80)}${(report.description || '').length > 80 ? '...' : ''}</p>
                <div class="activity-time">${report.createdAt ? formatDate(report.createdAt) : ''}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading recent reports:', error);
    }
}

// Load pending verifications
async function loadPendingVerifications() {
    try {
        const response = await apiCall('/admin/manufacturers?verified=false&size=5');
        const container = document.getElementById('pendingVerifications');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No pending verifications</p>';
            return;
        }

        container.innerHTML = response.content.map(mfr => `
            <div class="activity-item" onclick="switchTab('manufacturers')" style="cursor:pointer">
                <h4>${mfr.companyName}</h4>
                <p>${mfr.email || 'N/A'}</p>
                <div class="activity-time">${mfr.createdAt ? formatDate(mfr.createdAt) : 'N/A'}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading pending verifications:', error);
    }
}

// Load reports
async function loadReports(status = '') {
    try {
        const url = status ? `/admin/reports?status=${status}&size=100` : '/admin/reports?size=100';
        const response = await apiCall(url);
        const container = document.getElementById('reportsList');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No reports found</p>';
            return;
        }

        container.innerHTML = response.content.map(report => `
            <div class="report-card" onclick="viewReportDetails('${report.id}')">
                <div class="report-header">
                    <span class="report-status ${report.status.toLowerCase()}">${report.status}</span>
                    <span class="report-date">${formatDate(report.createdAt)}</span>
                </div>
                <h3>${report.medicineName}</h3>
                <p>${report.description}</p>
                ${report.photoUrl ? `<img src="${report.photoUrl}" alt="Report" class="report-image">` : ''}
                <div class="report-footer">
                    <span>📍 ${report.location || 'Unknown'}</span>
                    <span>👤 ${report.reporterName}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading reports:', error);
        showToast('Failed to load reports', 'error');
    }
}

// Filter reports
function filterReports() {
    const status = document.getElementById('reportStatusFilter').value;
    loadReports(status);
}

// View report details
async function viewReportDetails(reportId) {
    currentReportId = reportId;
    try {
        const report = await apiCall(`/admin/reports/${reportId}`);
        const modal = document.getElementById('reportModal');
        const details = document.getElementById('reportDetails');

        details.innerHTML = `
            ${report.photoUrl ? `<img src="${report.photoUrl}" alt="Report" class="report-detail-image">` : ''}
            <div class="detail-section">
                <h3>Report Information</h3>
                <div class="detail-row">
                    <span class="detail-label">Medicine Name:</span>
                    <span class="detail-value">${report.medicineName}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Status:</span>
                    <span class="detail-value">${report.status}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Reported By:</span>
                    <span class="detail-value">${report.reporterName}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Location:</span>
                    <span class="detail-value">${report.location || 'Unknown'}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Date:</span>
                    <span class="detail-value">${formatDate(report.createdAt)}</span>
                </div>
            </div>
            <div class="detail-section">
                <h3>Description</h3>
                <p>${report.description}</p>
            </div>
            ${report.adminNotes ? `
                <div class="detail-section">
                    <h3>Admin Notes</h3>
                    <p>${report.adminNotes}</p>
                </div>
            ` : ''}
        `;

        modal.classList.add('active');
    } catch (error) {
        console.error('Error loading report details:', error);
        showToast('Failed to load report details', 'error');
    }
}

// Close report modal
function closeReportModal() {
    document.getElementById('reportModal').classList.remove('active');
    currentReportId = null;
}

// Verify report
async function verifyReport() {
    if (!currentReportId) return;

    try {
        showLoading();
        await apiCall(`/admin/reports/${currentReportId}/verify`, { 
            method: 'PATCH',
            body: JSON.stringify({ adminNotes: '' })
        });
        hideLoading();
        showToast('Report verified successfully', 'success');
        closeReportModal();
        loadReports();
        loadDashboard();
    } catch (error) {
        hideLoading();
        console.error('Error verifying report:', error);
        showToast(error.message || 'Failed to verify report', 'error');
    }
}

// Dismiss report
async function dismissReport() {
    if (!currentReportId) return;

    try {
        showLoading();
        await apiCall(`/admin/reports/${currentReportId}/dismiss`, { 
            method: 'PATCH',
            body: JSON.stringify({ adminNotes: '' })
        });
        hideLoading();
        showToast('Report dismissed', 'success');
        closeReportModal();
        loadReports();
        loadDashboard();
    } catch (error) {
        hideLoading();
        console.error('Error dismissing report:', error);
        showToast(error.message || 'Failed to dismiss report', 'error');
    }
}

// Load manufacturers
async function loadManufacturers(verified = '') {
    try {
        const url = verified ? `/admin/manufacturers?verified=${verified}&size=100` : '/admin/manufacturers?size=100';
        const response = await apiCall(url);
        const container = document.getElementById('manufacturersList');

        if (!response.content || response.content.length === 0) {
            container.innerHTML = '<p class="no-data">No manufacturers found</p>';
            return;
        }

        container.innerHTML = response.content.map(mfr => `
            <div class="manufacturer-card">
                <div class="manufacturer-header">
                    <h3>${mfr.companyName}</h3>
                    <span class="manufacturer-badge ${mfr.isVerified ? 'verified' : 'unverified'}">
                        ${mfr.isVerified ? '✅ Verified' : '⏳ Pending'}
                    </span>
                </div>
                <div class="manufacturer-info">
                    <p><strong>Email:</strong> ${mfr.email || 'N/A'}</p>
                    <p><strong>License:</strong> ${mfr.licenseNumber || 'N/A'}</p>
                    <p><strong>Address:</strong> ${[mfr.address, mfr.city, mfr.state].filter(Boolean).join(', ') || 'N/A'}</p>
                    <p><strong>Joined:</strong> ${mfr.createdAt ? formatDate(mfr.createdAt) : 'N/A'}</p>
                </div>
                <div class="manufacturer-actions">
                    ${!mfr.isVerified ? `
                        <button class="btn btn-success" onclick="verifyManufacturer('${mfr.id}')">Verify</button>
                    ` : `
                        <button class="btn btn-danger" onclick="suspendManufacturer('${mfr.id}')">Suspend</button>
                    `}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading manufacturers:', error);
        showToast('Failed to load manufacturers', 'error');
    }
}

// Filter manufacturers
function filterManufacturers() {
    const verified = document.getElementById('manufacturerFilter').value;
    loadManufacturers(verified);
}

// Verify manufacturer
async function verifyManufacturer(id) {
    try {
        showLoading();
        await apiCall(`/admin/manufacturers/${id}/verify`, { method: 'PATCH' });
        hideLoading();
        showToast('Manufacturer verified successfully', 'success');
        loadManufacturers();
        loadDashboard();
    } catch (error) {
        hideLoading();
        console.error('Error verifying manufacturer:', error);
        showToast(error.message || 'Failed to verify manufacturer', 'error');
    }
}

// Suspend manufacturer
async function suspendManufacturer(id) {
    if (!confirm('Are you sure you want to suspend this manufacturer?')) return;

    try {
        showLoading();
        await apiCall(`/admin/manufacturers/${id}/suspend`, { method: 'PATCH' });
        hideLoading();
        showToast('Manufacturer suspended', 'success');
        loadManufacturers();
        loadDashboard();
    } catch (error) {
        hideLoading();
        console.error('Error suspending manufacturer:', error);
        showToast(error.message || 'Failed to suspend manufacturer', 'error');
    }
}

// Load analytics
async function loadAnalytics() {
    const days = document.getElementById('analyticsRange').value;

    try {
        const data = await apiCall(`/admin/analytics?days=${days}`);

        // Scan trends
        if (data.scanTrends && data.scanTrends.labels && data.scanTrends.labels.length > 0) {
            createLineChart('scanTrendsChart', 'Daily Scans', data.scanTrends);
        } else {
            showChartEmpty('scanTrendsChart', 'No scan data for this period');
        }

        // Report distribution (scan results: GENUINE, FAKE, EXPIRED etc.)
        if (data.reportDistribution && data.reportDistribution.labels && data.reportDistribution.labels.length > 0) {
            createPieChart('reportDistributionChart', 'Scan Results', data.reportDistribution);
        } else {
            showChartEmpty('reportDistributionChart', 'No scan result data yet');
        }

        // Top categories (reports by state)
        if (data.topCategories && data.topCategories.labels && data.topCategories.labels.length > 0) {
            createBarChart('categoriesChart', 'Reports by State', data.topCategories);
        } else {
            showChartEmpty('categoriesChart', 'No verified reports by state yet');
        }

        // Verification rate
        if (data.verificationRate && data.verificationRate.values &&
            data.verificationRate.values.some(v => v > 0)) {
            createDoughnutChart('verificationRateChart', 'Report Status', data.verificationRate);
        } else {
            showChartEmpty('verificationRateChart', 'No reports yet');
        }

    } catch (error) {
        console.error('Error loading analytics:', error);
        showToast('Failed to load analytics', 'error');
    }
}

function showChartEmpty(canvasId, message) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const parent = canvas.parentElement;
    canvas.style.display = 'none';
    // Avoid duplicate messages
    if (!parent.querySelector('.no-data')) {
        const msg = document.createElement('p');
        msg.className = 'no-data';
        msg.textContent = message;
        parent.appendChild(msg);
    }
}

// Create line chart
function createLineChart(canvasId, label, data) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    ctx.style.display = '';
    ctx.parentElement.querySelectorAll('.no-data').forEach(e => e.remove());
    if (charts[canvasId]) charts[canvasId].destroy();

    charts[canvasId] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.labels || [],
            datasets: [{
                label: label,
                data: data.values || [],
                borderColor: '#2563eb',
                backgroundColor: 'rgba(37, 99, 235, 0.1)',
                tension: 0.4,
                fill: true,
                pointRadius: 4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: true } },
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        }
    });
}

// Create pie chart
function createPieChart(canvasId, label, data) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    ctx.style.display = '';
    ctx.parentElement.querySelectorAll('.no-data').forEach(e => e.remove());
    if (charts[canvasId]) charts[canvasId].destroy();

    charts[canvasId] = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: data.labels || [],
            datasets: [{
                data: data.values || [],
                backgroundColor: ['#16a34a', '#d97706', '#dc2626', '#6b7280', '#2563eb']
            }]
        },
        options: { responsive: true }
    });
}

// Create bar chart
function createBarChart(canvasId, label, data) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    ctx.style.display = '';
    ctx.parentElement.querySelectorAll('.no-data').forEach(e => e.remove());
    if (charts[canvasId]) charts[canvasId].destroy();

    charts[canvasId] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels || [],
            datasets: [{
                label: label,
                data: data.values || [],
                backgroundColor: '#2563eb',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        }
    });
}

// Create doughnut chart
function createDoughnutChart(canvasId, label, data) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    ctx.style.display = '';
    ctx.parentElement.querySelectorAll('.no-data').forEach(e => e.remove());
    if (charts[canvasId]) charts[canvasId].destroy();

    charts[canvasId] = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: data.labels || [],
            datasets: [{
                data: data.values || [],
                backgroundColor: ['#16a34a', '#d97706', '#6b7280']
            }]
        },
        options: { responsive: true }
    });
}

// Load hotspots
async function loadHotspots() {
    try {
        const response = await apiCall('/admin/reports/hotspots');

        // Initialize map
        if (!hotspotsMap) {
            hotspotsMap = L.map('hotspotsMap').setView([20.5937, 78.9629], 5); // India center

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(hotspotsMap);
        }

        // Add GeoJSON layer
        if (response.features && response.features.length > 0) {
            L.geoJSON(response, {
                pointToLayer: (feature, latlng) => {
                    const count = feature.properties.count || 0;
                    return L.circleMarker(latlng, {
                        radius: Math.min(count * 2, 30),
                        fillColor: '#dc2626',
                        color: '#fff',
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 0.6
                    });
                },
                onEachFeature: (feature, layer) => {
                    if (feature.properties) {
                        layer.bindPopup(`
                            <strong>${feature.properties.location}</strong><br>
                            Reports: ${feature.properties.count}
                        `);
                    }
                }
            }).addTo(hotspotsMap);

            // Update stats
            const stats = document.getElementById('hotspotStats');
            const totalReports = response.features.reduce((sum, f) => sum + (f.properties.count || 0), 0);
            const locations = response.features.length;

            stats.innerHTML = `
                <div class="hotspot-stat">
                    <h3>${totalReports}</h3>
                    <p>Total Reports</p>
                </div>
                <div class="hotspot-stat">
                    <h3>${locations}</h3>
                    <p>Hotspot Locations</p>
                </div>
            `;
        } else {
            document.getElementById('hotspotStats').innerHTML = '<p class="no-data">No hotspots detected</p>';
        }
    } catch (error) {
        console.error('Error loading hotspots:', error);
        showToast('Failed to load hotspots', 'error');
    }
}

// Refresh hotspots
function refreshHotspots() {
    loadHotspots();
    showToast('Hotspots refreshed', 'success');
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
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.classList.remove('active');
        if (link.dataset.tab === tabName) link.classList.add('active');
    });
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.getElementById(tabName).classList.add('active');

    switch (tabName) {
        case 'reports':      loadReports(); break;
        case 'manufacturers': loadManufacturers(); break;
        case 'analytics':    loadAnalytics(); break;
        case 'hotspots':     loadHotspots(); break;
        case 'bans':         loadBans(); break;
    }
}

// ── Ban Medicine ──────────────────────────────────────────────────────────────

async function loadBans() {
    try {
        const recalls = await apiCall('/recalls');
        const container = document.getElementById('bansList');

        if (!recalls || recalls.length === 0) {
            container.innerHTML = '<p class="no-data">No banned medicines ✅</p>';
            return;
        }

        container.innerHTML = recalls.map(r => `
            <div class="report-card">
                <div class="report-header">
                    <span class="report-status ${r.isActive ? 'pending' : 'verified'}">${r.isActive ? '🔴 Active' : '✅ Lifted'}</span>
                    <span class="report-date">${r.createdAt ? formatDate(r.createdAt) : ''}</span>
                </div>
                <h3>${r.medicine?.name || 'Unknown'}</h3>
                <p><strong>Reason:</strong> ${r.reason}</p>
                <p><strong>Severity:</strong> <span style="font-weight:700;color:${r.severity==='CRITICAL'||r.severity==='HIGH'?'#dc2626':'#d97706'}">${r.severity}</span></p>
                <p><strong>Batch:</strong> ${r.medicine?.batchNumber || 'All'}</p>
                ${r.isActive ? `<button class="btn btn-outline" style="margin-top:0.5rem" onclick="liftBan('${r.id}')">Lift Ban</button>` : ''}
            </div>
        `).join('');
    } catch (e) {
        console.error('Error loading bans:', e);
        document.getElementById('bansList').innerHTML = '<p class="error">Failed to load</p>';
    }
}

async function searchMedicinesForBan(query) {
    const resultsDiv = document.getElementById('banMedicineResults');
    if (!query || query.length < 2) { resultsDiv.style.display = 'none'; return; }

    try {
        const data = await apiCall(`/medicines/search?query=${encodeURIComponent(query)}&size=10`);
        const items = data.content || [];
        if (items.length === 0) { resultsDiv.style.display = 'none'; return; }

        resultsDiv.style.display = 'block';
        resultsDiv.innerHTML = items.map(m => `
            <div onclick="selectBanMedicine('${m.id}','${m.name} (${m.batchNumber})')"
                 style="padding:0.75rem 1rem;cursor:pointer;border-bottom:1px solid #f1f5f9"
                 onmouseover="this.style.background='#f8fafc'" onmouseout="this.style.background=''">
                <strong>${m.name}</strong><br>
                <small style="color:#64748b">Batch: ${m.batchNumber} | ${m.manufacturer?.companyName || ''}</small>
            </div>
        `).join('');
    } catch (e) { resultsDiv.style.display = 'none'; }
}

function selectBanMedicine(id, label) {
    document.getElementById('banMedicineId').value = id;
    document.getElementById('selectedMedicineName').textContent = '✓ Selected: ' + label;
    document.getElementById('banMedicineResults').style.display = 'none';
    document.getElementById('banMedicineSearch').value = '';
}

async function issueBan() {
    const medicineId = document.getElementById('banMedicineId').value;
    const reason = document.getElementById('banReason').value.trim();
    const severity = document.getElementById('banSeverity').value;

    if (!medicineId) { showToast('Please select a medicine', 'error'); return; }
    if (!reason) { showToast('Please enter a reason', 'error'); return; }

    try {
        showLoading();
        await apiCall('/admin/recalls', {
            method: 'POST',
            body: JSON.stringify({ medicineId, reason, severity })
        });
        hideLoading();
        showToast('Medicine banned successfully', 'success');
        document.getElementById('issueBanModal').classList.remove('active');
        document.getElementById('banMedicineId').value = '';
        document.getElementById('banReason').value = '';
        document.getElementById('selectedMedicineName').textContent = '';
        loadBans();
        loadDashboard();
    } catch (e) {
        hideLoading();
        showToast(e.message || 'Failed to issue ban', 'error');
    }
}

async function liftBan(id) {
    if (!confirm('Lift this ban? The medicine will become active again.')) return;
    try {
        showLoading();
        await apiCall(`/admin/recalls/${id}/deactivate`, { method: 'PATCH' });
        hideLoading();
        showToast('Ban lifted successfully', 'success');
        loadBans();
        loadDashboard();
    } catch (e) {
        hideLoading();
        showToast(e.message || 'Failed to lift ban', 'error');
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
    const modal = document.getElementById('reportModal');
    if (e.target === modal) {
        closeReportModal();
    }
});
