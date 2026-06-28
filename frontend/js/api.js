// API Configuration
const API_BASE_URL = 'http://localhost:8080/api/v1';

// Get auth token
function getAuthToken() {
    return localStorage.getItem('accessToken');
}

// Set auth token
function setAuthToken(token) {
    localStorage.setItem('accessToken', token);
}

// Remove auth token
function removeAuthToken() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

// API call wrapper
async function apiCall(endpoint, options = {}) {
    showLoading();

    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(API_BASE_URL + endpoint, {
            ...options,
            headers
        });

        hideLoading();

        // Handle 401 - try to refresh token
        if (response.status === 401 && token) {
            const refreshed = await refreshAccessToken();
            if (refreshed) {
                // Retry original request
                return apiCall(endpoint, options);
            } else {
                // Refresh failed, logout
                logout();
                window.location.href = 'auth.html';
                throw new Error('Session expired');
            }
        }

        if (!response.ok) {
            let message = 'Request failed';
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const error = await response.json();
                    message = error.message || message;
                } else if (response.status === 403) {
                    message = 'Access denied. Your account may not be verified yet.';
                } else if (response.status === 401) {
                    message = 'Unauthorized. Please log in again.';
                }
            } catch (e) { /* empty body, use default message */ }
            throw new Error(message);
        }

        // Handle empty responses (e.g. 204 No Content)
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            return null;
        }
        return await response.json();
    } catch (error) {
        hideLoading();
        console.error('API Error:', error);
        throw error;
    }
}

// Refresh access token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return false;

    try {
        const response = await fetch(API_BASE_URL + '/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            setAuthToken(data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            return true;
        }
        return false;
    } catch (error) {
        return false;
    }
}

// Upload file
async function uploadFile(endpoint, formData) {
    showLoading();

    const token = getAuthToken();
    const headers = {};

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(API_BASE_URL + endpoint, {
            method: 'POST',
            headers,
            body: formData
        });

        hideLoading();

        if (!response.ok) {
            let message = 'Upload failed';
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const error = await response.json();
                    message = error.message || message;
                } else if (response.status === 403) {
                    message = 'Access denied. Your account may not be verified yet.';
                }
            } catch (e) { /* empty body */ }
            throw new Error(message);
        }

        return await response.json();
    } catch (error) {
        hideLoading();
        console.error('Upload Error:', error);
        throw error;
    }
}

// Logout
function logout() {
    removeAuthToken();
    showToast('Logged out successfully', 'success');
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 1000);
}
