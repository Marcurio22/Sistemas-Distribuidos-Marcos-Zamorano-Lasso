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
 * Punto de entrada de utilidades de interfaz.
 */
document.addEventListener('DOMContentLoaded', () => {
    initPlantioMap();
    initPlantioChat();
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
