const API_URL = 'fcgi-bin/server.jar';
const form = document.getElementById('point-form');
const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');
const tableBody = document.getElementById('results-body');
const error = document.getElementById('error');
const resultText = document.getElementById('result');
const serverInfo = document.getElementById('server-info');
const button = form.querySelector('button');
const canvasSize = 440;
// X строка
const xValues = ['-2', '-1.5', '-1', '-0.5', '0', '0.5', '1', '1.5', '2'];
const maxInputLength = 40;
let results = [];

// Bigint
function parseDecimal(text) {
    if (typeof text !== 'string') return null;
    const value = text.trim().replace(',', '.');
    const match = /^([+-]?)(?:(\d+)(?:\.(\d*))?|\.(\d+))$/.exec(value);
    if (!match) return null;
    const sign = match[1] === '-' ? -1n : 1n;
    const intPart = match[2] || '0';
    const fracPart = (match[3] !== undefined ? match[3] : match[4]) || '';
    return { int: sign * BigInt(intPart + fracPart), scale: fracPart.length };
}

function compare(a, b) {
    const scale = Math.max(a.scale, b.scale);
    const x = a.int * 10n ** BigInt(scale - a.scale);
    const y = b.int * 10n ** BigInt(scale - b.scale);
    return x < y ? -1 : x > y ? 1 : 0;
}

const Y_MIN = parseDecimal('-5');
const Y_MAX = parseDecimal('3');
const R_MIN = parseDecimal('2');
const R_MAX = parseDecimal('5');

function formatDate(timestamp) {
    return new Date(timestamp).toLocaleString('ru-RU', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
}

function formatMs(nanos) {
    return (nanos / 1e6).toFixed(3);
}

function renderTable() {
    tableBody.textContent = '';
    if (results.length === 0) {
        const cell = tableBody.insertRow().insertCell();
        cell.colSpan = 6;
        cell.textContent = 'Проверок пока нет';
        return;
    }
    for (let i = results.length - 1; i >= 0; i--) {
        const item = results[i];
        const row = tableBody.insertRow();
        [item.x, item.y, item.r, item.hit ? 'Попадание' : 'Промах',
            formatDate(item.timestamp), formatMs(item.executionNanos)].forEach(function (value) {
            row.insertCell().textContent = value;
        });
    }
}

function drawCanvas(r, points) {
    canvas.width = canvasSize;
    canvas.height = canvasSize;
    const center = 220;
    const scale = 36;
    const size = r * scale;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = 'rgba(48, 145, 245, 0.75)';
    // Треугольник
    ctx.beginPath();
    ctx.moveTo(center - size / 2, center);
    ctx.lineTo(center, center - size / 2);
    ctx.lineTo(center, center);
    ctx.closePath();
    ctx.fill();
    // Круг
    ctx.beginPath();
    ctx.moveTo(center, center);
    ctx.arc(center, center, size / 2, -Math.PI / 2, 0);
    ctx.closePath();
    ctx.fill();
    // Прямоугольник
    ctx.fillRect(center, center, size, size / 2);

    ctx.strokeStyle = '#263345';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(18, center); ctx.lineTo(422, center);
    ctx.moveTo(center, 422); ctx.lineTo(center, 18);
    ctx.moveTo(415, center - 4); ctx.lineTo(422, center); ctx.lineTo(415, center + 4);
    ctx.moveTo(center - 4, 25); ctx.lineTo(center, 18); ctx.lineTo(center + 4, 25);
    ctx.stroke();
    ctx.fillStyle = '#263345';
    ctx.font = '14px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('x', 425, center - 8);
    ctx.fillText('y', center + 8, 18);
    ctx.fillText('0', center - 14, center + 16);
    const marks = [-r, -r / 2, r / 2, r];
    const labels = ['−R', '−R/2', 'R/2', 'R'];
    marks.forEach(function (value, index) {
        const px = center + value * scale;
        const py = center - value * scale;
        ctx.beginPath();
        ctx.moveTo(px, center - 4); ctx.lineTo(px, center + 4);
        ctx.moveTo(center - 4, py); ctx.lineTo(center + 4, py);
        ctx.stroke();
        ctx.textAlign = 'center';
        ctx.fillText(labels[index], px, center - 10);
        ctx.textAlign = 'left';
        ctx.fillText(labels[index], center + 9, py + 5);
    });
    points.forEach(function (point) {
        ctx.beginPath();
        ctx.arc(center + Number(point.x) * scale, center - Number(point.y) * scale, 5, 0, Math.PI * 2);
        ctx.fillStyle = point.hit ? '#16813c' : '#cb2538';
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
    });
}

function showGraph() {
    if (results.length === 0) {
        drawCanvas(3, []);
        return;
    }
    const last = results[results.length - 1];
    drawCanvas(Number(last.r), results.filter(function (item) { return item.r === last.r; }));
    document.getElementById('graph-info').textContent =
        'Последняя проверка: X = ' + last.x + ', Y = ' + last.y + ', R = ' + last.r;
}

let lastResponse = null;
function showServerInfo() {
    if (!lastResponse) return;
    serverInfo.textContent = 'Время сервера: ' + formatDate(lastResponse.currentTime) +
        ' · время работы скрипта: ' + formatMs(lastResponse.executionNanos) + ' мс';
}


async function sendRequest(method, body) {
    const options = { method: method, headers: { 'Accept': 'application/json' } };
    if (body) options.body = body;
    let response;
    try {
        response = await fetch(API_URL, options);
    } catch (e) {
        throw new Error('Сервер недоступен.');
    }
    let data;
    try {
        data = await response.json();
    } catch (e) {
        throw new Error('Сервер вернул некорректный ответ (HTTP ' + response.status + ').');
    }
    lastResponse = data;
    showServerInfo();
    if (!response.ok || !data.ok) throw new Error(data.error || 'Ошибка сервера (HTTP ' + response.status + ').');
    results = Array.isArray(data.results) ? data.results : [];
    renderTable();
    showGraph();
    return data;
}

// Error / ""
function checkNumber(text, name, min, max, rangeText) {
    if (!text.trim()) return 'Введите ' + name + '.';
    if (text.length > maxInputLength) return name + ' слишком длинное (не больше ' + maxInputLength + ' символов).';
    const value = parseDecimal(text);
    if (!value) return name + ' должно быть обычным десятичным числом.';
    if (compare(value, min) <= 0 || compare(value, max) >= 0) return name + ' должно быть ' + rangeText + '.';
    return '';
}

function validate() {
    const checked = form.querySelectorAll('input[type="radio"][name="x"]:checked');
    const x = checked.length === 1 ? checked[0].value : '';
    const yText = String(document.getElementById('y').value);
    const rText = String(document.getElementById('r').value);
    if (!xValues.includes(x)) return 'Выберите X из предложенных значений.';
    const error = checkNumber(yText, 'Y', Y_MIN, Y_MAX, 'больше −5 и меньше 3') ||
        checkNumber(rText, 'R', R_MIN, R_MAX, 'больше 2 и меньше 5');
    if (error) return error;
    return new URLSearchParams({ x: x, y: yText.trim(), r: rText.trim() });
}

form.addEventListener('submit', async function (event) {
    event.preventDefault();
    error.textContent = '';
    resultText.textContent = '';
    const body = validate();
    if (typeof body === 'string') {
        error.textContent = body;
        return;
    }
    button.disabled = true;
    try {
        await sendRequest('POST', body);
        const last = results[results.length - 1];
        resultText.textContent = last.hit ? 'Точка попала в область.' : 'Точка не попала в область.';
    } catch (e) {
        error.textContent = e.message;
    } finally {
        button.disabled = false;
    }
});

renderTable();
showGraph();
sendRequest('GET').catch(function (e) { serverInfo.textContent = 'Не удалось загрузить историю: ' + e.message; });

let timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
let offset = new Date().getTimezoneOffset();
function refreshTimezone() {
    const nextTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const nextOffset = new Date().getTimezoneOffset();
    if (nextTimezone !== timezone || nextOffset !== offset) {
        timezone = nextTimezone;
        offset = nextOffset;
        renderTable();
        showServerInfo();
    }
}
window.addEventListener('focus', refreshTimezone);
setInterval(refreshTimezone, 1000);
