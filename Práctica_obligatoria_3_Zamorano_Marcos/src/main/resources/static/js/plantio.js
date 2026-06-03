/*
 * El Plantío 360 - JavaScript principal.
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

/**
 * Inicializa el visor cartográfico Leaflet con datos inyectados por Thymeleaf.
 */
function initPlantioMap() {
    const mapElement = document.getElementById('plantioMap');
    if (!mapElement || typeof L === 'undefined') return;
    const map = L.map('plantioMap').setView([42.3500, -3.6890], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    const data = window.plantioMapData || {points: [], sensors: []};
    (data.points || []).forEach(point => {
        L.marker([Number(point.latitude), Number(point.longitude)])
            .addTo(map)
            .bindPopup(`<strong>${point.name}</strong><br>${point.type}<br>${point.description || ''}`);
    });
    (data.sensors || []).forEach(sensor => {
        const color = sensor.status === 'CRITICAL' ? 'red' : sensor.status === 'WARNING' ? 'orange' : 'green';
        L.circleMarker([Number(sensor.latitude), Number(sensor.longitude)], {radius: 10, color})
            .addTo(map)
            .bindPopup(`<strong>${sensor.name}</strong><br>${sensor.type}: ${sensor.value} ${sensor.unit}<br>Estado: ${sensor.status}`);
    });
}

/**
 * Inicializa el chat STOMP del Muro Blanquinegro.
 */
function initPlantioChat() {
    const button = document.getElementById('sendChat');
    const input = document.getElementById('chatInput');
    const box = document.getElementById('chatMessages');
    if (!button || !input || !box || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    const stompClient = Stomp.over(new SockJS('/ws'));
    stompClient.debug = null;
    stompClient.connect({}, () => {
        stompClient.subscribe('/topic/chat', payload => {
            const message = JSON.parse(payload.body);
            const row = document.createElement('div');
            row.className = 'chat-message';
            row.innerHTML = `<strong>${message.displayName}</strong><span>${message.content}</span>`;
            box.appendChild(row);
            box.scrollTop = box.scrollHeight;
        });
    });
    button.addEventListener('click', () => sendChatMessage(stompClient, input));
    input.addEventListener('keydown', event => {
        if (event.key === 'Enter') sendChatMessage(stompClient, input);
    });
}

/**
 * Envía un mensaje por WebSocket cuando hay contenido no vacío.
 *
 * @param {object} stompClient cliente STOMP.
 * @param {HTMLInputElement} input campo de texto.
 */
function sendChatMessage(stompClient, input) {
    const content = input.value.trim();
    if (!content || !stompClient.connected) return;
    stompClient.send('/app/chat.send', {}, JSON.stringify({content}));
    input.value = '';
}


/**
 * Inicializa los mensajes flash como toasts volátiles y no intrusivos.
 */
function initFlashToasts() {
    document.querySelectorAll('[data-toast]').forEach(toast => {
        const closeButton = toast.querySelector('.plantio-toast-close');
        let dismissed = false;
        const dismiss = () => {
            if (dismissed) return;
            dismissed = true;
            toast.classList.add('plantio-toast-leaving');
            window.setTimeout(() => toast.remove(), 240);
        };
        if (closeButton) closeButton.addEventListener('click', dismiss);
        const delay = Number(toast.dataset.autoDismiss || 5000);
        if (delay > 0) window.setTimeout(dismiss, delay);
    });
}


/**
 * Inicializa controles del checkout simulado: límite de stock y formato visual de la tarjeta ficticia.
 */
function initCheckoutControls() {
    document.querySelectorAll('[data-stock-limited]').forEach(input => {
        const stock = Number(input.dataset.stock || input.getAttribute('max') || 0);
        const normalizeQuantity = () => {
            let value = Number(input.value || 1);
            if (Number.isNaN(value) || value < 1) value = 1;
            if (stock > 0 && value > stock) value = stock;
            input.value = String(value);
        };
        input.addEventListener('input', normalizeQuantity);
        input.addEventListener('blur', normalizeQuantity);
        normalizeQuantity();
    });

    const cardInput = document.querySelector('[data-demo-number-input]');
    const cardHidden = document.querySelector('[data-demo-number-hidden]');
    const holderInput = document.querySelector('[data-demo-holder-input]');
    const holderHidden = document.querySelector('[data-demo-holder-hidden]');
    const expiryInput = document.querySelector('[data-demo-expiry-input]');
    const expiryHidden = document.querySelector('[data-demo-expiry-hidden]');
    const cvvInput = document.querySelector('[data-demo-cvv-input]');
    const cvvHidden = document.querySelector('[data-demo-cvv-hidden]');

    if (cardInput && cardHidden) {
        const formatCard = () => {
            const digits = cardInput.value.replace(/\D/g, '').slice(0, 16);
            cardInput.value = digits.replace(/(.{4})/g, '$1 ').trim();
            cardHidden.value = digits;
        };
        cardInput.addEventListener('input', formatCard);
        formatCard();
    }
    if (expiryInput && expiryHidden) {
        const formatExpiry = () => {
            const digits = expiryInput.value.replace(/\D/g, '').slice(0, 4);
            expiryInput.value = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
            expiryHidden.value = expiryInput.value;
        };
        expiryInput.addEventListener('input', formatExpiry);
        formatExpiry();
    }
    if (cvvInput && cvvHidden) {
        const formatCvv = () => {
            cvvInput.value = cvvInput.value.replace(/\D/g, '').slice(0, 4);
            cvvHidden.value = cvvInput.value;
        };
        cvvInput.addEventListener('input', formatCvv);
        formatCvv();
    }
    if (holderInput && holderHidden) {
        const syncHolder = () => { holderHidden.value = holderInput.value.trim(); };
        holderInput.addEventListener('input', syncHolder);
        syncHolder();
    }

    document.querySelectorAll('form[data-form-type="other"]').forEach(form => {
        form.addEventListener('submit', () => {
            if (cardInput && cardHidden) cardHidden.value = cardInput.value.replace(/\D/g, '').slice(0, 16);
            if (holderInput && holderHidden) holderHidden.value = holderInput.value.trim();
            if (expiryInput && expiryHidden) expiryHidden.value = expiryInput.value;
            if (cvvInput && cvvHidden) cvvHidden.value = cvvInput.value.replace(/\D/g, '').slice(0, 4);
        });
    });
}

/**
 * Punto de entrada de utilidades de interfaz.
 */
document.addEventListener('DOMContentLoaded', () => {
    initPlantioMap();
    initPlantioChat();
    initFlashToasts();
    initCheckoutControls();
});


/**
 * Rellena el textarea del asistente desde una FAQ seleccionada.
 * Autor: Marcos Zamorano Lasso
 */
document.addEventListener('click', (event) => {
    const button = event.target.closest('.faq-question-button');
    if (!button) return;
    const questionInput = document.getElementById('question');
    if (!questionInput) return;
    questionInput.value = button.dataset.question || '';
    questionInput.focus();
});
