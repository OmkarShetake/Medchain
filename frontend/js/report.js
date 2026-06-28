// report.js - Fake Medicine Report logic

document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();

    const qrFromUrl = getQueryParam('qr');
    if (qrFromUrl) {
        document.getElementById('qrCode').value = qrFromUrl;
    }

    document.getElementById('photo').addEventListener('change', previewPhoto);
    document.getElementById('autoLocation').addEventListener('change', toggleManualLocation);
});

function previewPhoto(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            document.getElementById('photoPreview').innerHTML =
                `<img src="${e.target.result}" style="max-width:100%;border-radius:8px;">`;
        };
        reader.readAsDataURL(file);
    }
}

function toggleManualLocation() {
    const auto = document.getElementById('autoLocation').checked;
    document.getElementById('manualLocation').classList.toggle('hidden', auto);
}

async function submitReport(e) {
    e.preventDefault();

    if (!isLoggedIn()) {
        showToast('Please login to submit a report', 'error');
        window.location.href = 'auth.html?redirect=report.html';
        return;
    }

    const formData = new FormData();
    const photo = document.getElementById('photo').files[0];

    const reportData = {
        qrCode: document.getElementById('qrCode').value,
        description: document.getElementById('description').value
    };

    if (document.getElementById('autoLocation').checked) {
        try {
            const location = await getLocation();
            reportData.locationLat = location.lat;
            reportData.locationLng = location.lng;
        } catch (error) {
            console.log('Location not available');
        }
    } else {
        reportData.city = document.getElementById('city').value;
        reportData.state = document.getElementById('state').value;
    }

    formData.append('report', new Blob([JSON.stringify(reportData)], { type: 'application/json' }));
    formData.append('photo', photo);

    try {
        const result = await uploadFile('/reports', formData);

        document.getElementById('reportForm').classList.add('hidden');
        document.getElementById('successMessage').classList.remove('hidden');
        document.getElementById('trackingId').textContent = result.id;

        showToast('Report submitted successfully!', 'success');
    } catch (error) {
        showToast('Failed to submit report: ' + error.message, 'error');
    }
}
