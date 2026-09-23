/**
 * TrustABAC-IoT Dashboard JavaScript
 * Phase 7E: Real-Time Monitoring & Telemetry Presentation Layer
 * 
 * Strict Constraints Applied:
 * 1. Observational only: No decision formulas or calculations on client.
 * 2. STOMP 1.2 client over native WebSocket without external dependencies.
 * 3. Initial state via REST, live stream via WebSocket.
 * 4. Bounded timeline memory (max 200 events).
 * 5. Correlation ID filtering over server-provided metadata.
 */

(function () {
    'use strict';

    // Global dashboard state
    const state = {
        wsConnected: false,
        stompClient: null,
        events: [],
        maxEvents: 200,
        filterCorrelationId: '',
        trustHistory: [80.0],
        riskHistory: [14.0],
        maxChartPoints: 30,
        stats: {
            scenarios: 0,
            requests: 0,
            allows: 0,
            restricts: 0,
            denies: 0
        }
    };

    // Subscribed topic mapping
    const TOPICS = [
        '/topic/devices',
        '/topic/trust',
        '/topic/risk',
        '/topic/authorization',
        '/topic/blockchain',
        '/topic/simulator',
        '/topic/security'
    ];

    /**
     * Minimal self-contained STOMP 1.2 Client over native WebSocket.
     */
    class SimpleStompClient {
        constructor(wsUrl) {
            this.wsUrl = wsUrl;
            this.ws = null;
            this.subscriptions = new Map();
            this.subCounter = 0;
            this.connected = false;
            this.onConnect = null;
            this.onError = null;
            this.onDisconnect = null;
            this.reconnectTimer = null;
        }

        connect(headers = {}, onConnect = null, onError = null) {
            if (onConnect) this.onConnect = onConnect;
            if (onError) this.onError = onError;
            
            try {
                this.ws = new WebSocket(this.wsUrl);
            } catch (err) {
                if (this.onError) this.onError(err);
                this.scheduleReconnect();
                return;
            }

            this.ws.onopen = () => {
                const connectFrame = 'CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0';
                this.ws.send(connectFrame);
            };

            this.ws.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.ws.onclose = () => {
                this.connected = false;
                if (this.onDisconnect) this.onDisconnect();
                this.scheduleReconnect();
            };

            this.ws.onerror = (err) => {
                if (this.onError) this.onError(err);
            };
        }

        scheduleReconnect() {
            if (this.reconnectTimer) return;
            this.reconnectTimer = setTimeout(() => {
                this.reconnectTimer = null;
                this.connect({}, this.onConnect, this.onError);
            }, 3000);
        }

        disconnect() {
            if (this.reconnectTimer) {
                clearTimeout(this.reconnectTimer);
                this.reconnectTimer = null;
            }
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
            this.connected = false;
        }

        subscribe(destination, callback) {
            for (const [subId, sub] of this.subscriptions.entries()) {
                if (sub.destination === destination) {
                    sub.callback = callback;
                    return subId;
                }
            }
            const subId = 'sub-' + (++this.subCounter);
            this.subscriptions.set(subId, { destination, callback });
            if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
                const frame = `SUBSCRIBE\nid:${subId}\ndestination:${destination}\nack:auto\n\n\0`;
                this.ws.send(frame);
            }
            return subId;
        }

        resubscribeAll() {
            for (const [subId, sub] of this.subscriptions.entries()) {
                const frame = `SUBSCRIBE\nid:${subId}\ndestination:${sub.destination}\nack:auto\n\n\0`;
                this.ws.send(frame);
            }
        }

        handleMessage(data) {
            if (!data) return;
            const trimmed = data.trim();
            if (!trimmed) return; // Heartbeat frame

            const lines = data.split('\n');
            const command = lines[0].trim();
            let i = 1;
            const headers = {};
            while (i < lines.length && lines[i].trim() !== '') {
                const colonIdx = lines[i].indexOf(':');
                if (colonIdx > 0) {
                    const key = lines[i].substring(0, colonIdx).trim();
                    const val = lines[i].substring(colonIdx + 1).trim();
                    headers[key] = val;
                }
                i++;
            }
            const body = lines.slice(i + 1).join('\n').replace(/\0$/, '');

            if (command === 'CONNECTED') {
                this.connected = true;
                this.resubscribeAll();
                if (this.onConnect) this.onConnect(headers);
            } else if (command === 'MESSAGE') {
                const subId = headers['subscription'];
                const sub = this.subscriptions.get(subId);
                if (sub && sub.callback) {
                    try {
                        const parsedBody = body ? JSON.parse(body) : null;
                        sub.callback({ headers, body: parsedBody, rawBody: body });
                    } catch (e) {
                        sub.callback({ headers, body: null, rawBody: body });
                    }
                }
            } else if (command === 'ERROR') {
                if (this.onError) this.onError(body);
            }
        }
    }

    /**
     * Determine WebSocket URL dynamically from current window location (host and port).
     */
    function getWebSocketUrl() {
        const loc = window.location;
        const proto = loc.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = loc.host ? loc.host : (loc.hostname ? `${loc.hostname}:${loc.port || '8090'}` : '127.0.0.1:8090');
        return `${proto}//${host}/ws`;
    }

    /**
     * Initialize Dashboard.
     */
    function initDashboard() {
        initHealthPolling();
        initContextBadges();
        initSimulatorPolling();
        initRestDeviceStates();
        initWebSocketConnection();
        setupEventListeners();
    }

    /**
     * Dynamically fetch and display active booking, property, and guest user.
     */
    async function initContextBadges() {
        const bookings = await apiCall('/api/bookings');
        if (Array.isArray(bookings) && bookings.length > 0) {
            const active = bookings.find(b => b.bookingStatus === 'ACTIVE') || bookings[0];
            if (active) {
                const elBooking = document.getElementById('contextBookingId');
                const elProperty = document.getElementById('contextPropertyId');
                const elGuest = document.getElementById('contextGuestUserId');
                if (elBooking && active.bookingReference) elBooking.textContent = active.bookingReference;
                if (elProperty && active.propertyId) elProperty.textContent = active.propertyId;
                if (elGuest && active.guestUserId) elGuest.textContent = active.guestUserId;
            }
        }
    }

    /**
     * Set up UI event listeners and window global functions for simulator buttons.
     */
    function setupEventListeners() {
        window.startSimulator = () => apiCall('/api/simulator/start', 'POST');
        window.stopSimulator = () => apiCall('/api/simulator/stop', 'POST');
        window.resetSimulator = () => {
            apiCall('/api/simulator/reset', 'POST').then(() => {
                resetLocalStats();
            });
        };
        window.runScenario = (name) => {
            apiCall(`/api/simulator/scenarios/${encodeURIComponent(name)}/run`, 'POST');
        };
        window.filterTimeline = () => {
            const input = document.getElementById('corrSearchInput');
            state.filterCorrelationId = input ? input.value.trim().toLowerCase() : '';
            renderTimeline();
        };
        window.clearTimelineFilter = () => {
            const input = document.getElementById('corrSearchInput');
            if (input) input.value = '';
            state.filterCorrelationId = '';
            renderTimeline();
        };
    }

    function resetLocalStats() {
        state.stats = { scenarios: 0, requests: 0, allows: 0, restricts: 0, denies: 0 };
        updateStatsBar();
        state.events = [];
        renderTimeline();
        const tbody = document.getElementById('authTableBody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="empty-table">Awaiting real-time authorization events...</td></tr>';
        const proofsFeed = document.getElementById('blockchainProofsFeed');
        if (proofsFeed) proofsFeed.innerHTML = '<div class="empty-proofs" id="emptyProofs">Awaiting on-chain smart contract transactions...</div>';
        const alertsCont = document.getElementById('securityAlertsContainer');
        if (alertsCont) alertsCont.innerHTML = '<div class="no-alerts" id="noAlertsMsg">No active security alerts detected. System operating normally.</div>';
    }

    /**
     * REST API Helper with JSON parsing and error handling.
     */
    async function apiCall(endpoint, method = 'GET', body = null) {
        try {
            const opts = {
                method,
                headers: { 'Content-Type': 'application/json' }
            };
            if (body) opts.body = JSON.stringify(body);
            const res = await fetch(endpoint, opts);
            if (!res.ok) {
                console.warn(`REST call to ${endpoint} failed with status ${res.status}`);
                return null;
            }
            return await res.json();
        } catch (err) {
            console.warn(`REST error for ${endpoint}:`, err);
            return null;
        }
    }

    /**
     * Health Checks Polling.
     */
    async function initHealthPolling() {
        async function checkHealth() {
            // 1. Spring Boot Actuator / Health (/api/health returns {"status":"UP"})
            const health = await apiCall('/api/health');
            const isBackendUp = health && (health.status === 'UP' || health.service === 'trustabac-iot');
            setIndicator('indicatorBackend', isBackendUp ? 'green' : 'red', isBackendUp ? 'UP' : 'DOWN');
            
            // 2. Ganache Blockchain (/api/blockchain/status returns {rpcReachable, latestBlock, ...})
            const bc = await apiCall('/api/blockchain/status');
            const bcOk = bc && (bc.rpcReachable === true || bc.connected === true);
            const blockNum = bc ? (bc.latestBlock !== undefined && bc.latestBlock !== null ? bc.latestBlock : bc.blockNumber) : null;
            setIndicator('indicatorGanache', bcOk ? 'green' : 'red', bcOk ? (blockNum !== null && blockNum !== undefined ? `UP (Block #${blockNum})` : 'UP') : 'DOWN');

            // 3. RabbitMQ Messaging (/api/messaging/status returns {brokerReachable, ...})
            const msg = await apiCall('/api/messaging/status');
            const msgOk = msg && (msg.brokerReachable === true || msg.connected === true || msg.status === 'UP' || msg.status === 'CONNECTED');
            setIndicator('indicatorRabbitMQ', msgOk ? 'green' : 'red', msgOk ? 'UP' : 'DOWN');

            // 4. MySQL DB (deduced from backend status)
            setIndicator('indicatorMySQL', isBackendUp ? 'green' : 'red', isBackendUp ? 'UP' : 'DOWN');
        }

        checkHealth();
        setInterval(checkHealth, 4000);
    }

    /**
     * Simulator Status Polling (Fallback for initial state).
     */
    async function initSimulatorPolling() {
        const sim = await apiCall('/api/simulator/status');
        if (sim) {
            updateSimulatorStatus(sim);
        }
    }

    /**
     * Load Initial Device States from REST.
     */
    async function initRestDeviceStates() {
        const devices = await apiCall('/api/devices');
        if (Array.isArray(devices)) {
            devices.forEach(dev => updateDeviceCard(dev));
        }
    }

    /**
     * Initialize STOMP WebSocket Connection and Subscriptions.
     */
    function initWebSocketConnection() {
        const url = getWebSocketUrl();
        setIndicator('indicatorWebSocket', 'amber', 'CONNECTING');
        const wsText = document.getElementById('wsStatusText');
        if (wsText) wsText.textContent = 'CONNECTING';

        const client = new SimpleStompClient(url);

        client.onConnect = () => {
            state.wsConnected = true;
            setIndicator('indicatorWebSocket', 'green', 'CONNECTED');
            if (wsText) wsText.textContent = 'CONNECTED';
        };

        client.onError = (err) => {
            state.wsConnected = false;
            setIndicator('indicatorWebSocket', 'red', 'ERROR');
            if (wsText) wsText.textContent = 'ERROR';
        };

        client.onDisconnect = () => {
            state.wsConnected = false;
            setIndicator('indicatorWebSocket', 'red', 'DISCONNECTED');
            if (wsText) wsText.textContent = 'DISCONNECTED';
        };

        // Subscribe to all 7 topics
        client.subscribe('/topic/devices', onDeviceEvent);
        client.subscribe('/topic/trust', onTrustEvent);
        client.subscribe('/topic/risk', onRiskEvent);
        client.subscribe('/topic/authorization', onAuthEvent);
        client.subscribe('/topic/blockchain', onBlockchainEvent);
        client.subscribe('/topic/simulator', onSimulatorEvent);
        client.subscribe('/topic/security', onSecurityEvent);

        client.connect();
        state.stompClient = client;
    }

    /**
     * Health Indicator Helper.
     */
    function setIndicator(elementId, color, text) {
        const el = document.getElementById(elementId);
        if (!el) return;
        const dot = el.querySelector('.dot');
        const val = el.querySelector('.indicator-val');
        if (dot) {
            dot.className = `dot dot-${color}`;
        }
        if (val) {
            val.textContent = text;
        }
    }

    /**
     * Top-Level Event Ingestion & Bounded Timeline Tracker.
     */
    function recordEvent(type, payload, correlationId = '') {
        const eventId = payload?.eventId || ('evt-' + Date.now() + '-' + Math.floor(Math.random() * 1000));
        const corrId = correlationId || payload?.correlationId || payload?.causationId || '';
        const timestamp = payload?.timestamp || new Date().toISOString();

        const eventObj = {
            id: eventId,
            type: type,
            correlationId: corrId,
            causationId: payload?.causationId || '',
            timestamp: timestamp,
            payload: payload
        };

        // Prepend to timeline memory and bound at maxEvents
        state.events.unshift(eventObj);
        if (state.events.length > state.maxEvents) {
            state.events.pop();
        }

        renderTimeline();
    }

    /**
     * Timeline Renderer with Correlation ID Filtering.
     */
    function renderTimeline() {
        const container = document.getElementById('timelineContainer');
        if (!container) return;

        const filter = state.filterCorrelationId;
        const filtered = filter
            ? state.events.filter(e => 
                (e.correlationId && e.correlationId.toLowerCase().includes(filter)) ||
                (e.id && e.id.toLowerCase().includes(filter)) ||
                (e.type && e.type.toLowerCase().includes(filter))
              )
            : state.events;

        if (filtered.length === 0) {
            container.innerHTML = filter 
                ? `<div class="empty-timeline">No events match filter: "${filter}"</div>`
                : `<div class="empty-timeline">Timeline ready. Events will stream in real-time as scenarios execute.</div>`;
            return;
        }

        let html = '';
        filtered.forEach(evt => {
            const timeStr = evt.timestamp ? new Date(evt.timestamp).toLocaleTimeString() : '';
            const summary = getEventSummary(evt);
            html += `
                <div class="timeline-event-item">
                    <div class="timeline-header">
                        <span class="timeline-type">${escapeHtml(evt.type)}</span>
                        <span class="timeline-time">${timeStr}</span>
                    </div>
                    <div class="timeline-body">${summary}</div>
                    <div class="timeline-meta">
                        <span>Event ID: ${escapeHtml(evt.id)}</span>
                        ${evt.correlationId ? `<span class="meta-corr">Corr: ${escapeHtml(evt.correlationId)}</span>` : ''}
                        ${evt.causationId ? `<span>Cause: ${escapeHtml(evt.causationId)}</span>` : ''}
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    function getEventSummary(evt) {
        const p = evt.payload;
        if (!p) return 'Telemetry signal received.';
        if (evt.type === 'DEVICE_EVENT') {
            return `Device <strong>${escapeHtml(p.deviceId || '')}</strong> state: <strong>${escapeHtml(p.status || p.state || '')}</strong> [Decision: ${escapeHtml(p.lastDecision || 'N/A')}]`;
        }
        if (evt.type === 'TRUST_EVENT') {
            return `Subject <strong>${escapeHtml(p.subjectId || p.deviceId || '')}</strong> score updated to <strong>${p.trustScore !== undefined ? p.trustScore : p.score}</strong> (${p.eventType || 'UPDATE'})`;
        }
        if (evt.type === 'RISK_EVENT') {
            return `Resource <strong>${escapeHtml(p.resourceId || '')}</strong> assessed risk: <strong>${p.overallRiskScore !== undefined ? p.overallRiskScore : p.riskScore}</strong> [${p.riskLevel || 'NORMAL'}]`;
        }
        if (evt.type === 'AUTH_EVENT') {
            return `Decision <strong>${escapeHtml(p.decision || '')}</strong> for <strong>${escapeHtml(p.deviceId || p.resourceId || '')}</strong> → Enforcement: <strong>${escapeHtml(p.enforcementOutcome || p.enforcementStatus || '')}</strong>`;
        }
        if (evt.type === 'BLOCKCHAIN_EVENT') {
            return `Smart Contract Block <strong>#${p.blockNumber || 0}</strong> Tx: <code>${shortHash(p.transactionHash)}</code> Gas: ${p.gasUsed || 0}`;
        }
        if (evt.type === 'SIMULATOR_EVENT') {
            return `Scenario <strong>${escapeHtml(p.scenario || p.scenarioName || 'RUN')}</strong> Step: ${escapeHtml(p.action || p.stepName || '')} (${p.status || 'ACTIVE'})`;
        }
        if (evt.type === 'SECURITY_EVENT') {
            return `<strong style="color:var(--status-deny)">ALERT:</strong> ${escapeHtml(p.description || p.message || p.alertType || 'Security threat detected')}`;
        }
        return JSON.stringify(p);
    }

    /**
     * WebSocket Topic Handlers
     */

    function unwrapEnvelope(msg) {
        if (!msg || !msg.body) return null;
        const env = msg.body;
        if (env && typeof env === 'object' && env.payload && typeof env.payload === 'object') {
            return {
                ...env.payload,
                eventId: env.eventId || env.payload.eventId,
                correlationId: env.correlationId || env.payload.correlationId,
                causationId: env.causationId || env.payload.causationId,
                eventType: env.eventType || env.payload.eventType,
                source: env.source || env.payload.source,
                timestamp: env.timestamp || env.payload.timestamp,
                deviceId: env.deviceIdentifier || env.deviceId || env.payload.deviceIdentifier || env.payload.deviceId
            };
        }
        return env;
    }

    function onDeviceEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateDeviceCard(payload);
        recordEvent('DEVICE_EVENT', payload, payload.correlationId);
    }

    function onTrustEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateTrustPanel(payload);
        recordEvent('TRUST_EVENT', payload, payload.correlationId);
    }

    function onRiskEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateRiskPanel(payload);
        recordEvent('RISK_EVENT', payload, payload.correlationId);
    }

    function onAuthEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateAuthTable(payload);
        recordEvent('AUTH_EVENT', payload, payload.correlationId);
    }

    function onBlockchainEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateBlockchainPanel(payload);
        recordEvent('BLOCKCHAIN_EVENT', payload, payload.correlationId);
    }

    function onSimulatorEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateSimulatorStatus(payload);
        recordEvent('SIMULATOR_EVENT', payload, payload.correlationId);
    }

    function onSecurityEvent(msg) {
        const payload = unwrapEnvelope(msg);
        if (!payload) return;
        updateSecurityPanel(payload);
        recordEvent('SECURITY_EVENT', payload, payload.correlationId);
    }

    /**
     * UI Updaters
     */

    function updateDeviceCard(dev) {
        const devId = dev.deviceId || dev.id;
        if (!devId) return;

        // Find canonical card matching ID or alias
        const card = document.getElementById(`card-${devId}`) ||
                     (devId.includes('CAM') ? document.getElementById('card-CAM-001') : null) ||
                     (devId.includes('OWNER') || devId.includes('CURTAIN') ? document.getElementById('card-OWNER-001') : null);

        if (!card) return;

        const targetId = card.id.replace('card-', '');

        const stateEl = document.getElementById(`state-${targetId}`);
        const trustEl = document.getElementById(`trust-${targetId}`);
        const riskEl = document.getElementById(`risk-${targetId}`);
        const decEl = document.getElementById(`dec-${targetId}`);
        const enfEl = document.getElementById(`enf-${targetId}`);

        if (stateEl && (dev.status || dev.state || dev.physicalState)) {
            const rawState = (dev.status || dev.state || dev.physicalState).toUpperCase();
            stateEl.textContent = rawState;
            stateEl.className = 'device-state-display ' + getStateClass(rawState);
        }

        if (trustEl && dev.trustScore !== undefined) {
            trustEl.textContent = Number(dev.trustScore).toFixed(1);
        }

        if (riskEl && dev.riskScore !== undefined) {
            riskEl.textContent = Number(dev.riskScore).toFixed(1);
        }

        if (decEl && dev.lastDecision) {
            const dec = dev.lastDecision.toUpperCase();
            decEl.textContent = dec;
            decEl.className = 'decision-badge ' + getDecisionBadgeClass(dec);
        }

        if (enfEl && (dev.enforcementOutcome || dev.lastEnforcementOutcome)) {
            const enf = (dev.enforcementOutcome || dev.lastEnforcementOutcome).toUpperCase();
            enfEl.textContent = enf;
            enfEl.className = 'enforce-badge ' + getEnforceBadgeClass(enf);
        }
    }

    function getStateClass(state) {
        if (state.includes('LOCKED') && !state.includes('UNLOCKED')) return 'state-locked';
        if (state.includes('UNLOCKED') || state.includes('ONLINE') || state.includes('OPEN') || state.includes('ON')) return 'state-active';
        if (state.includes('OFF') || state.includes('CLOSED')) return 'state-off';
        if (state.includes('STANDBY')) return 'state-standby';
        if (state.includes('PROTECTED') || state.includes('RESTRICTED')) return 'state-protected';
        return 'state-active';
    }

    function getDecisionBadgeClass(dec) {
        if (dec === 'ALLOW') return 'badge-allow';
        if (dec === 'RESTRICT') return 'badge-restrict';
        if (dec === 'DENY') return 'badge-deny';
        return 'badge-allow';
    }

    function getEnforceBadgeClass(enf) {
        if (enf === 'EXECUTED') return 'status-executed';
        if (enf === 'DOWNGRADED') return 'status-downgraded';
        if (enf === 'BLOCKED') return 'status-blocked';
        return 'status-executed';
    }

    function updateTrustPanel(data) {
        const score = data.trustScore !== undefined ? Number(data.trustScore) : (data.score !== undefined ? Number(data.score) : 80.0);
        const scoreEl = document.getElementById('trustScoreNum');
        if (scoreEl) scoreEl.textContent = score.toFixed(1);

        const statusTag = document.getElementById('trustStatusTag');
        if (statusTag) {
            if (score >= 70) {
                statusTag.textContent = 'TRUSTED';
                statusTag.className = 'status-tag status-trusted';
            } else if (score >= 40) {
                statusTag.textContent = 'SUSPICIOUS';
                statusTag.className = 'status-tag status-low';
            } else {
                statusTag.textContent = 'REVOKED / UNTRUSTED';
                statusTag.className = 'status-tag status-high';
            }
        }

        const deltaTag = document.getElementById('trustDeltaTag');
        if (deltaTag && data.delta !== undefined) {
            const d = Number(data.delta);
            deltaTag.textContent = (d >= 0 ? '+' : '') + d.toFixed(1);
        }

        const devEl = document.getElementById('trustDevice');
        if (devEl && (data.deviceId || data.subjectId)) devEl.textContent = data.deviceId || data.subjectId;

        const evEl = document.getElementById('trustEventType');
        if (evEl && data.eventType) evEl.textContent = data.eventType;

        const timeEl = document.getElementById('trustTimestamp');
        if (timeEl) timeEl.textContent = new Date().toLocaleTimeString();

        // Push to sparkline history
        state.trustHistory.push(score);
        if (state.trustHistory.length > state.maxChartPoints) state.trustHistory.shift();
        renderSparkline('trustSparklinePath', state.trustHistory, 0, 100);
    }

    function updateRiskPanel(data) {
        const score = data.overallRiskScore !== undefined ? Number(data.overallRiskScore) : (data.riskScore !== undefined ? Number(data.riskScore) : 14.0);
        const scoreEl = document.getElementById('riskScoreNum');
        if (scoreEl) scoreEl.textContent = score.toFixed(1);

        const statusTag = document.getElementById('riskStatusTag');
        if (statusTag) {
            const level = (data.riskLevel || (score > 60 ? 'HIGH' : score > 30 ? 'MEDIUM' : 'LOW')).toUpperCase();
            statusTag.textContent = level + ' RISK';
            statusTag.className = 'status-tag ' + (level === 'HIGH' ? 'status-high' : level === 'MEDIUM' ? 'status-low' : 'status-trusted');
        }

        const resEl = document.getElementById('riskResource');
        if (resEl && (data.resourceId || data.resource)) resEl.textContent = data.resourceId || data.resource;

        const opEl = document.getElementById('riskOperation');
        if (opEl && (data.operation || data.action)) opEl.textContent = data.operation || data.action;

        const reasonEl = document.getElementById('riskReason');
        if (reasonEl && data.reason) reasonEl.textContent = data.reason;

        // Push to sparkline history
        state.riskHistory.push(score);
        if (state.riskHistory.length > state.maxChartPoints) state.riskHistory.shift();
        renderSparkline('riskSparklinePath', state.riskHistory, 0, 100);
    }

    function renderSparkline(elementId, points, minVal, maxVal) {
        const path = document.getElementById(elementId);
        if (!path || points.length === 0) return;

        const width = 400;
        const height = 80;
        const padding = 5;

        const n = points.length;
        const stepX = n > 1 ? width / (n - 1) : width;

        let d = '';
        points.forEach((val, idx) => {
            const x = idx * stepX;
            // Invert y because SVG y goes down
            const clamped = Math.max(minVal, Math.min(maxVal, val));
            const normY = (clamped - minVal) / (maxVal - minVal || 1);
            const y = (height - padding) - (normY * (height - 2 * padding));
            if (idx === 0) {
                d += `M ${x.toFixed(1)} ${y.toFixed(1)}`;
            } else {
                d += ` L ${x.toFixed(1)} ${y.toFixed(1)}`;
            }
        });

        path.setAttribute('d', d);
    }

    function updateAuthTable(data) {
        const tbody = document.getElementById('authTableBody');
        if (!tbody) return;

        // Clear empty placeholder row if present
        const empty = tbody.querySelector('.empty-table');
        if (empty) {
            tbody.innerHTML = '';
        }

        const timeStr = new Date().toLocaleTimeString();
        const devId = data.deviceId || data.resourceId || 'DEVICE';
        const op = data.requestedOperation || data.operation || 'READ';
        const trust = data.trustScore !== undefined ? Number(data.trustScore).toFixed(1) : '-';
        const risk = data.riskScore !== undefined ? Number(data.riskScore).toFixed(1) : '-';
        const dec = (data.decision || 'ALLOW').toUpperCase();
        const enf = (data.enforcementOutcome || data.enforcementStatus || 'EXECUTED').toUpperCase();
        const effOp = data.effectiveOperation || op;

        // Increment stats
        state.stats.requests++;
        if (dec === 'ALLOW') state.stats.allows++;
        else if (dec === 'RESTRICT') state.stats.restricts++;
        else if (dec === 'DENY') state.stats.denies++;
        updateStatsBar();

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${timeStr}</td>
            <td><strong>${escapeHtml(devId)}</strong></td>
            <td>${escapeHtml(op)}</td>
            <td>${trust} / ${risk}</td>
            <td><span class="decision-badge ${getDecisionBadgeClass(dec)}">${dec}</span></td>
            <td><span class="enforce-badge ${getEnforceBadgeClass(enf)}">${enf}</span></td>
            <td>${escapeHtml(effOp)}</td>
        `;

        tbody.insertBefore(row, tbody.firstChild);

        // Keep maximum 50 rows in table
        while (tbody.children.length > 50) {
            tbody.removeChild(tbody.lastChild);
        }
    }

    function updateBlockchainPanel(data) {
        const feed = document.getElementById('blockchainProofsFeed');
        if (!feed) return;

        const empty = document.getElementById('emptyProofs');
        if (empty) {
            feed.removeChild(empty);
        }

        const blockNum = data.blockNumber !== undefined ? data.blockNumber : (data.block || 0);
        const gasUsed = data.gasUsed !== undefined ? data.gasUsed : 0;
        const txHash = data.transactionHash || data.txHash || '0x0000000000000000000000000000000000000000';
        const contract = data.contractAddress || 'AdaptiveAccessControl';
        const decision = (data.decision || 'ALLOW').toUpperCase();
        const timeStr = new Date().toLocaleTimeString();

        const item = document.createElement('div');
        item.className = 'proof-item';
        item.innerHTML = `
            <div class="proof-header">
                <span class="proof-block">Block #${blockNum}</span>
                <span class="decision-badge ${getDecisionBadgeClass(decision)}">${decision}</span>
            </div>
            <div class="proof-tx" title="Click to copy full tx hash: ${escapeHtml(txHash)}" onclick="navigator.clipboard.writeText('${escapeHtml(txHash)}')">
                Tx: ${escapeHtml(shortHash(txHash))} (copyable)
            </div>
            <div class="proof-footer">
                <span class="proof-gas">Gas: ${Number(gasUsed).toLocaleString()}</span>
                <span>${timeStr}</span>
            </div>
        `;

        feed.insertBefore(item, feed.firstChild);

        // Keep maximum 30 items
        while (feed.children.length > 30) {
            feed.removeChild(feed.lastChild);
        }
    }

    function updateSimulatorStatus(data) {
        const pill = document.getElementById('simStatusPill');
        if (pill) {
            const status = (data.status || (data.running ? 'RUNNING' : 'IDLE')).toUpperCase();
            pill.textContent = `STATUS: ${status}`;
            pill.style.borderColor = data.running ? 'var(--status-allow)' : 'var(--border-color)';
        }

        if (data.scenarioName || data.scenario) {
            state.stats.scenarios++;
            updateStatsBar();
        }
    }

    function updateSecurityPanel(data) {
        const container = document.getElementById('securityAlertsContainer');
        if (!container) return;

        const noAlerts = document.getElementById('noAlertsMsg');
        if (noAlerts) {
            container.removeChild(noAlerts);
        }

        const isWarning = data.severity === 'WARNING' || data.level === 'WARN';
        const title = data.title || data.alertType || 'Security Alert';
        const desc = data.description || data.message || 'Mitigation applied';
        const timeStr = new Date().toLocaleTimeString();

        const item = document.createElement('div');
        item.className = `alert-item ${isWarning ? 'alert-warning' : ''}`;
        item.innerHTML = `
            <div class="alert-header">
                <span>${escapeHtml(title)}</span>
                <span>${timeStr}</span>
            </div>
            <div class="alert-desc">${escapeHtml(desc)}</div>
        `;

        container.insertBefore(item, container.firstChild);

        while (container.children.length > 20) {
            container.removeChild(container.lastChild);
        }
    }

    function updateStatsBar() {
        const s = document.getElementById('statScenarios');
        const r = document.getElementById('statRequests');
        const a = document.getElementById('statAllows');
        const res = document.getElementById('statRestricts');
        const d = document.getElementById('statDenies');

        if (s) s.textContent = state.stats.scenarios;
        if (r) r.textContent = state.stats.requests;
        if (a) a.textContent = state.stats.allows;
        if (res) res.textContent = state.stats.restricts;
        if (d) d.textContent = state.stats.denies;
    }

    /**
     * Helpers
     */
    function shortHash(hash) {
        if (!hash || typeof hash !== 'string') return '0x000...000';
        if (hash.length <= 16) return hash;
        return hash.substring(0, 10) + '...' + hash.substring(hash.length - 8);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initDashboard);
    } else {
        initDashboard();
    }

})();
