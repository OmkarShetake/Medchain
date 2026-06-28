// WebSocket Client for Real-time Notifications

let stompClient = null;
let isConnected = false;
let retryCount = 0;
const MAX_RETRIES = 5;

// Initialize WebSocket connection
function connectWebSocket() {
    const token = getAuthToken();
    if (!token) {
        console.log('No auth token, skipping WebSocket connection');
        return;
    }

    const user = getCurrentUser();
    if (!user) {
        console.log('No user found, skipping WebSocket connection');
        return;
    }

    // Create WebSocket connection
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);

    // Disable debug logging
    stompClient.debug = null;

    // Connect
    stompClient.connect(
        { Authorization: `Bearer ${token}` },
        () => onConnected(user),
        onError
    );
}

// On connected callback
function onConnected(user) {
    isConnected = true;
    retryCount = 0;
    console.log('WebSocket connected');

    // Subscribe to user-specific notifications
    stompClient.subscribe(`/user/${user.id}/notifications`, onNotificationReceived);

    // Subscribe to public broadcasts (recalls, alerts)
    stompClient.subscribe('/topic/recalls', onRecallReceived);
    stompClient.subscribe('/topic/alerts', onAlertReceived);

    // Show connection status
    showToast('Connected to real-time notifications', 'success');
}

// On error callback
function onError(error) {
    isConnected = false;
    console.error('WebSocket error:', error);

    // Retry with backoff, up to MAX_RETRIES times
    if (retryCount < MAX_RETRIES) {
        retryCount++;
        const delay = Math.min(5000 * retryCount, 30000);
        console.log(`Retrying WebSocket connection (${retryCount}/${MAX_RETRIES}) in ${delay/1000}s...`);
        setTimeout(() => {
            connectWebSocket();
        }, delay);
    } else {
        console.log('WebSocket max retries reached. Real-time notifications unavailable.');
    }
}

// On notification received
function onNotificationReceived(payload) {
    const notification = JSON.parse(payload.body);
    console.log('Notification received:', notification);

    // Show toast notification
    showNotificationToast(notification);

    // Update notification badge
    updateNotificationBadge();

    // Play notification sound (optional)
    playNotificationSound();

    // Trigger custom event for other parts of the app
    window.dispatchEvent(new CustomEvent('notification', { detail: notification }));
}

// On recall received
function onRecallReceived(payload) {
    const recall = JSON.parse(payload.body);
    console.log('Recall received:', recall);

    // Show urgent toast
    showToast(`🚨 BANNED MEDICINE: ${recall.medicine.name} - ${recall.reason}`, 'error', 10000);

    // Trigger custom event
    window.dispatchEvent(new CustomEvent('recall', { detail: recall }));
}

// On alert received
function onAlertReceived(payload) {
    const alert = JSON.parse(payload.body);
    console.log('Alert received:', alert);

    // Show alert toast
    showToast(`🚨 ${alert.message}`, 'warning', 8000);

    // Trigger custom event
    window.dispatchEvent(new CustomEvent('alert', { detail: alert }));
}

// Show notification toast
function showNotificationToast(notification) {
    const message = notification.message || 'New notification';
    const type = notification.type === 'RECALL' ? 'error' : 
                 notification.type === 'ALERT' ? 'warning' : 'info';
    
    showToast(message, type, 5000);
}

// Update notification badge
function updateNotificationBadge() {
    // This would update a badge count in the UI
    // Implementation depends on your UI structure
    const badge = document.getElementById('notificationBadge');
    if (badge) {
        const currentCount = parseInt(badge.textContent) || 0;
        badge.textContent = currentCount + 1;
        badge.style.display = 'inline-block';
    }
}

// Play notification sound
function playNotificationSound() {
    // Optional: Play a notification sound
    try {
        const audio = new Audio('/sounds/notification.mp3');
        audio.volume = 0.3;
        audio.play().catch(e => console.log('Could not play sound:', e));
    } catch (error) {
        // Ignore sound errors
    }
}

// Disconnect WebSocket
function disconnectWebSocket() {
    if (stompClient && isConnected) {
        stompClient.disconnect(() => {
            isConnected = false;
            console.log('WebSocket disconnected');
        });
    }
}

// Send message (if needed)
function sendWebSocketMessage(destination, message) {
    if (stompClient && isConnected) {
        stompClient.send(destination, {}, JSON.stringify(message));
    } else {
        console.error('WebSocket not connected');
    }
}

// Check connection status
function isWebSocketConnected() {
    return isConnected;
}

// Auto-connect on page load
document.addEventListener('DOMContentLoaded', () => {
    // Only connect if user is logged in
    const token = getAuthToken();
    if (token) {
        connectWebSocket();
    }
});

// Disconnect on page unload
window.addEventListener('beforeunload', () => {
    disconnectWebSocket();
});

// Reconnect on visibility change (when tab becomes active)
document.addEventListener('visibilitychange', () => {
    if (!document.hidden && !isConnected) {
        const token = getAuthToken();
        if (token) {
            connectWebSocket();
        }
    }
});

// Export functions for use in other scripts
window.WebSocketClient = {
    connect: connectWebSocket,
    disconnect: disconnectWebSocket,
    send: sendWebSocketMessage,
    isConnected: isWebSocketConnected
};
