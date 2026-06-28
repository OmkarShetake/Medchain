// Authentication Functions

// Check if user is logged in
function isLoggedIn() {
    return !!getAuthToken();
}

// Get current user
function getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
}

// Update navigation based on auth status
function updateNavAuth() {
    const navAuth = document.getElementById('navAuth');
    if (!navAuth) return;

    if (isLoggedIn()) {
        const user = getCurrentUser();
        navAuth.innerHTML = `
            <div class="notification-bell" onclick="window.location.href='notifications.html'">
                🔔
                <span class="notification-badge" id="notificationBadge" style="display: none;">0</span>
            </div>
            <div class="user-menu">
                <button class="btn btn-outline" onclick="goToDashboard()">${user?.name || 'Dashboard'}</button>
                <button class="btn btn-primary" onclick="logout()">Logout</button>
            </div>
        `;
        loadUnreadCount();
    } else {
        navAuth.innerHTML = `
            <a href="auth.html" class="btn btn-outline">Login</a>
            <a href="auth.html?register=true" class="btn btn-primary">Sign Up</a>
        `;
    }
}

// Go to appropriate dashboard
function goToDashboard() {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = 'auth.html';
        return;
    }

    switch (user.role) {
        case 'ADMIN':
            window.location.href = 'admin.html';
            break;
        case 'MANUFACTURER':
            window.location.href = 'dashboard.html';
            break;
        case 'CHEMIST':
            window.location.href = 'chemist.html';
            break;
        default:
            window.location.href = 'verify.html';
    }
}

// Load unread notification count
async function loadUnreadCount() {
    if (!isLoggedIn()) return;

    try {
        const data = await apiCall('/notifications/unread-count');
        const badge = document.getElementById('notificationBadge');
        if (badge && data.count > 0) {
            badge.textContent = data.count;
            badge.style.display = 'block';
        }
    } catch (error) {
        console.error('Failed to load notification count:', error);
    }
}

// Protect route (require authentication)
function protectRoute() {
    if (!isLoggedIn()) {
        showToast('Please login to continue', 'info');
        window.location.href = 'auth.html?redirect=' + encodeURIComponent(window.location.pathname);
    }
}

// Require role
function requireRole(role) {
    const user = getCurrentUser();
    if (!user || user.role !== role) {
        showToast('Access denied', 'error');
        window.location.href = 'index.html';
    }
}

// Login function
async function login(email, password) {
    try {
        const data = await apiCall('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        setAuthToken(data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('user', JSON.stringify(data.user));

        showToast('Login successful!', 'success');

        // Redirect
        const redirect = getQueryParam('redirect');
        setTimeout(() => {
            window.location.href = redirect || 'index.html';
        }, 1000);

        return true;
    } catch (error) {
        showToast(error.message || 'Login failed', 'error');
        return false;
    }
}

// Register function
async function register(formData) {
    try {
        const data = await apiCall('/auth/register', {
            method: 'POST',
            body: JSON.stringify(formData)
        });

        setAuthToken(data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('user', JSON.stringify(data.user));

        showToast('Registration successful!', 'success');

        setTimeout(() => {
            window.location.href = 'index.html';
        }, 1000);

        return true;
    } catch (error) {
        showToast(error.message || 'Registration failed', 'error');
        return false;
    }
}

// Mobile menu toggle
document.addEventListener('DOMContentLoaded', () => {
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', () => {
            const navLinks = document.querySelector('.nav-links');
            const navAuth = document.querySelector('.nav-auth');
            navLinks?.classList.toggle('active');
            navAuth?.classList.toggle('active');
        });
    }
});
