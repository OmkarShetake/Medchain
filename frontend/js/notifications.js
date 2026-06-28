// notifications.js - Notifications page logic

let currentPage = 0;
const PAGE_SIZE = 15;
let totalPages = 1;

const typeIcons = {
    RECALL:           '🚨',
    ALERT:            '⚠️',
    REPORT_VERIFIED:  '✅',
    REPORT_DISMISSED: '❌',
    EXPIRY_ALERT:     '⏰',
    DEFAULT:          '📋'
};

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth()) return;
    updateNavAuth();
    loadNotifications();
});

async function loadNotifications(append = false) {
    try {
        const data = await apiCall(`/notifications?page=${currentPage}&size=${PAGE_SIZE}&sort=createdAt,desc`);
        totalPages = data.totalPages;

        if (!append) {
            if (data.content.length === 0) {
                document.getElementById('notifList').innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🔔</div>
                        <p>You have no notifications yet</p>
                    </div>`;
                return;
            }
            document.getElementById('notifList').innerHTML = '';
        }

        const hasUnread = data.content.some(n => !n.isRead);
        document.getElementById('markAllBtn').style.display = hasUnread ? 'inline-flex' : 'none';

        data.content.forEach(n => {
            document.getElementById('notifList').appendChild(buildCard(n));
        });

        document.getElementById('loadMoreDiv').style.display =
            currentPage + 1 < totalPages ? 'block' : 'none';

    } catch (e) {
        document.getElementById('notifList').innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">⚠️</div>
                <p>Failed to load notifications</p>
            </div>`;
    }
}

function buildCard(n) {
    const div = document.createElement('div');
    const type = n.type || 'DEFAULT';
    div.className = `notif-card ${!n.isRead ? 'unread' : ''} type-${type}`;
    div.innerHTML = `
        <div class="notif-icon">${typeIcons[type] || typeIcons.DEFAULT}</div>
        <div class="notif-body">
            <div class="notif-title">${escapeHtml(n.title || 'Notification')}</div>
            <div class="notif-message">${escapeHtml(n.message || '')}</div>
            <div class="notif-time">${formatDateTime(n.createdAt)}</div>
        </div>
        ${!n.isRead ? '<div class="unread-dot"></div>' : ''}
        <button class="notif-delete" onclick="deleteNotif(event, '${n.id}')" title="Dismiss">✕</button>
    `;
    return div;
}

async function markAllRead() {
    try {
        await apiCall('/notifications/read-all', { method: 'PATCH' });
        document.getElementById('markAllBtn').style.display = 'none';
        document.querySelectorAll('.notif-card.unread').forEach(c => {
            c.classList.remove('unread');
            c.querySelector('.unread-dot')?.remove();
        });
        const badge = document.getElementById('notificationBadge');
        if (badge) { badge.textContent = '0'; badge.style.display = 'none'; }
        showToast('All notifications marked as read', 'success');
    } catch (e) {
        showToast('Failed to mark as read', 'error');
    }
}

async function deleteNotif(event, id) {
    event.stopPropagation();
    try {
        await apiCall(`/notifications/${id}`, { method: 'DELETE' });
        event.target.closest('.notif-card').remove();
        if (document.querySelectorAll('.notif-card').length === 0) {
            document.getElementById('notifList').innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🔔</div>
                    <p>You have no notifications yet</p>
                </div>`;
        }
    } catch (e) {
        showToast('Failed to delete notification', 'error');
    }
}

function loadMore() {
    currentPage++;
    loadNotifications(true);
}

function escapeHtml(str) {
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

function requireAuth() {
    if (!getAuthToken()) {
        window.location.href = 'auth.html';
        return false;
    }
    return true;
}
