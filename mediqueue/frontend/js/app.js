/**
 * MediQueue Frontend Analytics Application
 * Connects to Spring Boot REST backend (http://localhost:8080/api)
 * Features: Dynamic Charts, Interactive Patient Flow, Bottleneck Radar, Reports Export
 */

const API_BASE = 'http://localhost:8080/api';

// State Store
let appData = {
    overview: null,
    departments: [],
    hourly: [],
    stages: [],
    bottlenecks: [],
    journeys: [],
    daily: []
};

let chartInstances = {};

// --------------------------- Lifecycle Init ------------------------------ //
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    startClock();
    setupEventListeners();
    fetchAllData();
});

function startClock() {
    const clockEl = document.getElementById('live-clock');
    const update = () => {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString('en-US', { hour12: false });
    };
    update();
    setInterval(update, 1000);
}

// ---------------------------- Navigation --------------------------------- //
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.content-section');
    const titleEl = document.getElementById('page-title');
    const subEl = document.getElementById('page-subtitle');

    const meta = {
        'overview': { title: 'Hospital Operational Overview', sub: 'Real-time Big Data metrics derived from historical workflow records' },
        'departments': { title: 'Department-Wise Congestion Analytics', sub: 'Comparative waiting times, service durations, and compliance rankings' },
        'patient-flow': { title: 'Patient Journey & Flow Analytics', sub: 'Stage-by-stage queue delays from Registration to Discharge' },
        'bottlenecks': { title: 'Rule-Based Bottleneck Radar', sub: 'Identification of recurring operational delays exceeding thresholds' },
        'pipeline': { title: 'Big Data Pipeline & Data Management', sub: 'HDFS, Pig cleaning, Hive analytics, and dataset ingestion status' },
        'reports': { title: 'Analytical Reports & Export Hub', sub: 'Download production Big Data analytical reports in standard CSV format' }
    };

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetSection = item.getAttribute('data-section');

            navItems.forEach(n => n.classList.remove('active'));
            sections.forEach(s => s.classList.remove('active'));

            item.classList.add('active');
            const targetEl = document.getElementById(`section-${targetSection}`);
            if (targetEl) targetEl.classList.add('active');

            if (meta[targetSection]) {
                titleEl.textContent = meta[targetSection].title;
                subEl.textContent = meta[targetSection].sub;
            }

            // Trigger chart resize if needed
            window.dispatchEvent(new Event('resize'));
        });
    });
}

function setupEventListeners() {
    document.getElementById('btn-refresh').addEventListener('click', () => {
        fetchAllData(true);
    });

    // Bottleneck severity filter pills
    document.querySelectorAll('.filter-pill').forEach(pill => {
        pill.addEventListener('click', () => {
            document.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            filterBottlenecks();
        });
    });

    // Threshold multiplier slider
    const slider = document.getElementById('threshold-slider');
    const sliderLabel = document.getElementById('multiplier-label');
    slider.addEventListener('input', (e) => {
        sliderLabel.textContent = `${parseFloat(e.target.value).toFixed(1)}x`;
        filterBottlenecks();
    });

    // Department filter select in Dept section
    document.getElementById('dept-filter-select').addEventListener('change', (e) => {
        filterDepartmentTable(e.target.value);
    });

    // Pipeline runner button
    document.getElementById('btn-run-pipeline').addEventListener('click', triggerPipelineRun);
    document.getElementById('btn-check-pipeline-status').addEventListener('click', checkPipelineStatus);

    // Modal close
    document.getElementById('btn-close-modal').addEventListener('click', () => {
        document.getElementById('journey-modal').classList.remove('active');
    });

    // CSV File dropzone upload validator
    setupCsvDropzone();
}

// -------------------------- API Data Fetching ---------------------------- //
async function fetchAllData(isRefresh = false) {
    const statusText = document.getElementById('backend-status-text');
    try {
        statusText.textContent = isRefresh ? 'Syncing...' : 'Fetching API...';

        const [overviewRes, deptsRes, hourlyRes, stagesRes, bRes, journeysRes, dailyRes] = await Promise.all([
            fetch(`${API_BASE}/overview`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/departments`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/analytics/hourly`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/flow/stages`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/bottlenecks`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/flow/journeys`).then(r => r.ok ? r.json() : null).catch(() => null),
            fetch(`${API_BASE}/analytics/daily`).then(r => r.ok ? r.json() : null).catch(() => null)
        ]);

        if (overviewRes) {
            appData.overview = overviewRes;
            appData.departments = deptsRes || overviewRes.topCongestedDepartments || [];
            appData.hourly = hourlyRes || overviewRes.hourlyTrends || [];
            appData.stages = stagesRes || overviewRes.flowSummary || [];
            appData.bottlenecks = bRes || [];
            appData.journeys = journeysRes || [];
            appData.daily = dailyRes || [];

            statusText.textContent = 'API Live (Port 8080)';
        } else {
            console.warn('[WARN] Backend API not responding at http://localhost:8080/api. Loading cached demo data.');
            statusText.textContent = 'Demo Mode (Offline)';
            loadFallbackData();
        }

        renderDashboard();

    } catch (err) {
        console.error('[ERROR] Failed fetching data:', err);
        statusText.textContent = 'Connection Error';
        loadFallbackData();
        renderDashboard();
    }
}

// ---------------------------- Render Functions --------------------------- //
function renderDashboard() {
    renderKpis();
    renderHourlyChart();
    renderDeptWaitChart();
    renderOverviewDeptsTable();
    renderOverviewFlowStepper();
    renderFullDepartments();
    renderDeptCharts();
    renderFlowVisualizer();
    renderJourneysTable();
    renderBottlenecks();
    renderDailyTrendsChart();
}

function renderKpis() {
    if (!appData.overview) return;
    const o = appData.overview;
    document.getElementById('kpi-total-visits').textContent = (o.totalVisits || 5000).toLocaleString();
    document.getElementById('kpi-stages-total').textContent = `${(o.totalStages || 22850).toLocaleString()} workflow stages`;
    document.getElementById('kpi-avg-wait').innerHTML = `${(o.avgWaitTimeMin || 19.1).toFixed(1)} <span class="unit">min</span>`;
    document.getElementById('kpi-avg-duration').innerHTML = `${(o.avgTotalVisitDurationMin || 68.5).toFixed(1)} <span class="unit">min</span>`;
    document.getElementById('kpi-total-departments').textContent = o.totalDepartments || 9;
    document.getElementById('kpi-bottlenecks-count').textContent = o.bottlenecksCount || appData.bottlenecks.length || 66;
    document.getElementById('nav-bottleneck-count').textContent = o.bottlenecksCount || appData.bottlenecks.length || 66;
    document.getElementById('kpi-peak-hour').textContent = o.peakCongestionPeriod || '11:00';
}

function renderHourlyChart() {
    const ctx = document.getElementById('chart-hourly');
    if (!ctx) return;

    if (chartInstances.hourly) chartInstances.hourly.destroy();

    const labels = appData.hourly.map(h => h.hourLabel);
    const volumes = appData.hourly.map(h => h.totalPatients);
    const waitTimes = appData.hourly.map(h => h.avgWaitTimeMin);

    chartInstances.hourly = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Arrival Volume (Patients)',
                    data: volumes,
                    backgroundColor: 'rgba(6, 182, 212, 0.45)',
                    borderColor: '#06b6d4',
                    borderWidth: 1.5,
                    borderRadius: 4,
                    yAxisID: 'y'
                },
                {
                    label: 'Avg Queue Wait (min)',
                    data: waitTimes,
                    type: 'line',
                    borderColor: '#f59e0b',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#f59e0b',
                    pointRadius: 3,
                    tension: 0.35,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { labels: { color: '#94a3b8', font: { family: 'Plus Jakarta Sans', size: 11 } } },
                tooltip: { padding: 10, cornerRadius: 8 }
            },
            scales: {
                x: { grid: { color: 'rgba(255, 255, 255, 0.04)' }, ticks: { color: '#64748b', font: { size: 10 } } },
                y: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'Patient Volume', color: '#64748b' },
                    grid: { color: 'rgba(255, 255, 255, 0.04)' },
                    ticks: { color: '#64748b' }
                },
                y1: {
                    type: 'linear',
                    position: 'right',
                    title: { display: true, text: 'Wait Time (min)', color: '#f59e0b' },
                    grid: { drawOnChartArea: false },
                    ticks: { color: '#f59e0b' }
                }
            }
        }
    });
}

function renderDeptWaitChart() {
    const ctx = document.getElementById('chart-dept-wait');
    if (!ctx) return;

    if (chartInstances.deptWait) chartInstances.deptWait.destroy();

    const depts = appData.departments;
    const names = depts.map(d => d.departmentCode || d.departmentName);
    const waits = depts.map(d => d.avgWaitTimeMin);
    const durs = depts.map(d => d.avgServiceDurMin);
    const targets = depts.map(d => d.targetWaitMin);

    chartInstances.deptWait = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: names,
            datasets: [
                {
                    label: 'Avg Wait Time (min)',
                    data: waits,
                    backgroundColor: 'rgba(244, 63, 94, 0.65)',
                    borderRadius: 4
                },
                {
                    label: 'Avg Service Duration (min)',
                    data: durs,
                    backgroundColor: 'rgba(20, 184, 166, 0.65)',
                    borderRadius: 4
                },
                {
                    label: 'Target Threshold (min)',
                    data: targets,
                    type: 'line',
                    borderColor: '#38bdf8',
                    borderDash: [5, 5],
                    borderWidth: 2,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { labels: { color: '#94a3b8', font: { size: 11 } } }
            },
            scales: {
                x: { grid: { color: 'rgba(255, 255, 255, 0.04)' }, ticks: { color: '#94a3b8' } },
                y: { grid: { color: 'rgba(255, 255, 255, 0.04)' }, ticks: { color: '#94a3b8' } }
            }
        }
    });
}

function renderOverviewDeptsTable() {
    const tbody = document.querySelector('#table-overview-depts tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    const top5 = appData.departments.slice(0, 5);
    top5.forEach((d, i) => {
        const tr = document.createElement('tr');
        const statusClass = d.status === 'CONGESTED' ? 'badge-danger' : 'badge-success';
        tr.innerHTML = `
            <td><span class="rank-badge rank-${i+1}">${d.waitTimeRank || i+1}</span></td>
            <td><strong>${d.departmentName}</strong></td>
            <td>${(d.totalVisitsServed || 0).toLocaleString()}</td>
            <td><strong style="color:var(--accent-rose)">${d.avgWaitTimeMin.toFixed(1)}m</strong></td>
            <td>${d.targetWaitMin}m</td>
            <td>${d.pctExceedingTarget}%</td>
            <td><span class="kpi-badge ${statusClass}">${d.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function renderOverviewFlowStepper() {
    const container = document.getElementById('overview-flow-stepper');
    if (!container) return;
    container.innerHTML = '';

    appData.stages.forEach(s => {
        const div = document.createElement('div');
        div.className = 'flow-step-item';
        div.innerHTML = `
            <div class="step-seq">${s.sequence}</div>
            <div class="step-info">
                <div class="step-name">${s.stageName}</div>
                <div class="step-metrics">
                    <span>Queue Wait: <strong>${s.avgQueueWaitMin.toFixed(1)} min</strong></span>
                    <span>Service Time: <strong>${s.avgServiceDurMin.toFixed(1)} min</strong></span>
                    <span>Total: <strong>${s.avgTotalStageMin.toFixed(1)} min</strong></span>
                </div>
            </div>
        `;
        container.appendChild(div);
    });
}

function renderFullDepartments() {
    const tbody = document.querySelector('#table-full-departments tbody');
    const select = document.getElementById('dept-filter-select');
    if (!tbody) return;

    tbody.innerHTML = '';
    if (select && select.options.length <= 1) {
        appData.departments.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d.departmentId;
            opt.textContent = `${d.departmentName} (${d.departmentCode})`;
            select.appendChild(opt);
        });
    }

    appData.departments.forEach((d, i) => {
        const tr = document.createElement('tr');
        tr.setAttribute('data-dept-id', d.departmentId);
        const statusClass = d.status === 'CONGESTED' ? 'badge-danger' : 'badge-success';
        tr.innerHTML = `
            <td><span class="rank-badge rank-${i+1}">${d.waitTimeRank || i+1}</span></td>
            <td><code>${d.departmentCode}</code></td>
            <td><strong>${d.departmentName}</strong></td>
            <td>${(d.totalVisitsServed || 0).toLocaleString()}</td>
            <td><strong style="color:var(--accent-rose)">${d.avgWaitTimeMin.toFixed(1)}m</strong></td>
            <td>${d.medianWaitTimeMin.toFixed(1)}m</td>
            <td>${d.minWaitTimeMin}m - ${d.maxWaitTimeMin}m</td>
            <td>${d.p90WaitTimeMin.toFixed(1)}m</td>
            <td>${d.avgServiceDurMin.toFixed(1)}m</td>
            <td>${d.targetWaitMin}m</td>
            <td>${d.pctExceedingTarget}%</td>
            <td><span class="kpi-badge ${statusClass}">${d.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function filterDepartmentTable(deptId) {
    const rows = document.querySelectorAll('#table-full-departments tbody tr');
    rows.forEach(r => {
        if (deptId === 'ALL' || r.getAttribute('data-dept-id') === String(deptId)) {
            r.style.display = '';
        } else {
            r.style.display = 'none';
        }
    });
}

function renderDeptCharts() {
    const ctxVol = document.getElementById('chart-dept-volume');
    const ctxExc = document.getElementById('chart-dept-exceeding');
    if (!ctxVol || !ctxExc) return;

    if (chartInstances.deptVol) chartInstances.deptVol.destroy();
    if (chartInstances.deptExc) chartInstances.deptExc.destroy();

    const labels = appData.departments.map(d => d.departmentCode || d.departmentName);
    const volumes = appData.departments.map(d => d.totalVisitsServed);
    const exceeding = appData.departments.map(d => d.pctExceedingTarget);

    chartInstances.deptVol = new Chart(ctxVol, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Patients Served',
                data: volumes,
                backgroundColor: 'rgba(59, 130, 246, 0.6)',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.04)' } },
                y: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.04)' } }
            }
        }
    });

    chartInstances.deptExc = new Chart(ctxExc, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: '% Exceeding Target Wait',
                data: exceeding,
                backgroundColor: 'rgba(244, 63, 94, 0.65)',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.04)' } },
                y: { ticks: { color: '#94a3b8' }, max: 100, grid: { color: 'rgba(255,255,255,0.04)' } }
            }
        }
    });
}

function renderFlowVisualizer() {
    const container = document.getElementById('flow-diagram');
    if (!container) return;
    container.innerHTML = '';

    appData.stages.forEach((s, idx) => {
        let delayClass = 'delay-fast';
        if (s.avgQueueWaitMin > 25) delayClass = 'delay-heavy';
        else if (s.avgQueueWaitMin > 15) delayClass = 'delay-medium';

        const node = document.createElement('div');
        node.className = 'flow-node';
        node.innerHTML = `
            <div class="flow-node-seq">${s.sequence}</div>
            <h4>${s.stageName}</h4>
            <div class="flow-delay-badge ${delayClass}">Avg Wait: ${s.avgQueueWaitMin.toFixed(1)}m</div>
            <div class="flow-subtext">Service: ${s.avgServiceDurMin.toFixed(1)}m</div>
            <div class="flow-subtext">Total: ${s.avgTotalStageMin.toFixed(1)}m</div>
            <div class="flow-subtext" style="margin-top:4px;color:var(--text-primary)">(${s.totalPatientsProcessed.toLocaleString()} visits)</div>
        `;
        container.appendChild(node);

        if (idx < appData.stages.length - 1) {
            const arrow = document.createElement('div');
            arrow.className = 'flow-arrow-connector';
            arrow.textContent = '➔';
            container.appendChild(arrow);
        }
    });
}

function renderJourneysTable() {
    const tbody = document.querySelector('#table-sample-journeys tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    appData.journeys.slice(0, 15).forEach(j => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code>${j.visitId}</code></td>
            <td>${j.patientId}</td>
            <td>${j.ageGroup}</td>
            <td>${j.gender}</td>
            <td><span class="kpi-badge badge-info">${j.admissionType}</span></td>
            <td>${j.visitDate}</td>
            <td>${j.totalStagesCount} stages</td>
            <td><strong style="color:var(--accent-rose)">${j.cumulativeWaitMin} min</strong></td>
            <td>${j.cumulativeServiceMin} min</td>
            <td><strong>${j.totalJourneyDurationMin} min</strong></td>
            <td><button class="btn-sm btn-outline" onclick="openJourneyModal('${j.visitId}')">Timeline</button></td>
        `;
        tbody.appendChild(tr);
    });
}

window.openJourneyModal = function(visitId) {
    const journey = appData.journeys.find(j => j.visitId === visitId);
    if (!journey) return;

    document.getElementById('modal-journey-title').textContent = `Patient Journey: ${journey.visitId}`;
    document.getElementById('modal-journey-sub').textContent = `Patient ${journey.patientId} | ${journey.ageGroup} | ${journey.gender} | Total Door-to-Discharge: ${journey.totalJourneyDurationMin} mins`;

    const body = document.getElementById('modal-journey-body');
    let html = '<div class="journey-step-timeline">';

    journey.stages.forEach(s => {
        html += `
            <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div style="display:flex;justify-content:space-between;align-items:center;">
                    <strong style="color:#ffffff">${s.stageSequence}. ${s.stageName}</strong>
                    <span style="font-size:0.75rem;color:var(--accent-cyan);font-family:var(--font-mono)">${s.departmentName}</span>
                </div>
                <div style="font-size:0.75rem;color:var(--text-muted);margin:4px 0;">
                    Arrival: ${s.arrivalTs} ➔ Service: ${s.serviceStartTs} ➔ Done: ${s.serviceEndTs}
                </div>
                <div style="font-size:0.8rem;display:flex;gap:1.5rem;margin-top:6px;">
                    <span>Wait Delay: <strong style="color:var(--accent-rose)">${s.waitTimeMin}m</strong></span>
                    <span>Service Time: <strong style="color:var(--accent-teal)">${s.serviceDurMin}m</strong></span>
                    <span>Total Stage: <strong>${s.totalStageTimeMin}m</strong></span>
                </div>
            </div>
        `;
    });
    html += '</div>';
    body.innerHTML = html;

    document.getElementById('journey-modal').classList.add('active');
};

function renderBottlenecks() {
    const container = document.getElementById('bottlenecks-container');
    if (!container) return;

    // Counts
    const crit = appData.bottlenecks.filter(b => b.severity === 'CRITICAL').length;
    const high = appData.bottlenecks.filter(b => b.severity === 'HIGH').length;
    const mod = appData.bottlenecks.filter(b => b.severity === 'MODERATE').length;

    document.getElementById('count-all-b').textContent = appData.bottlenecks.length;
    document.getElementById('count-crit-b').textContent = crit;
    document.getElementById('count-high-b').textContent = high;
    document.getElementById('count-mod-b').textContent = mod;

    filterBottlenecks();
}

function filterBottlenecks() {
    const container = document.getElementById('bottlenecks-container');
    if (!container) return;
    container.innerHTML = '';

    const activePill = document.querySelector('.filter-pill.active');
    const selectedSev = activePill ? activePill.getAttribute('data-severity') : 'ALL';
    const sliderVal = parseFloat(document.getElementById('threshold-slider').value);

    const filtered = appData.bottlenecks.filter(b => {
        const matchesSev = (selectedSev === 'ALL' || b.severity === selectedSev);
        const matchesMult = b.congestionMultiplier >= sliderVal;
        return matchesSev && matchesMult;
    });

    if (filtered.length === 0) {
        container.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:3rem;color:var(--text-muted)">No bottlenecks match the selected threshold criteria.</div>';
        return;
    }

    filtered.forEach(b => {
        const div = document.createElement('div');
        div.className = `bottleneck-card sev-${b.severity.toLowerCase()}`;
        div.innerHTML = `
            <div class="card-top">
                <div>
                    <div class="btn-stage-sub">${b.stageName} • ${b.hourWindow}</div>
                    <div class="btn-dept-title">${b.departmentName}</div>
                </div>
                <span class="sev-badge ${b.severity}">${b.severity}</span>
            </div>

            <div class="btn-stats-row">
                <div class="btn-stat-item">
                    <span class="btn-stat-label">Avg Wait Time</span>
                    <span class="btn-stat-val" style="color:var(--accent-rose)">${b.avgWaitTimeMin}m</span>
                </div>
                <div class="btn-stat-item">
                    <span class="btn-stat-label">Congestion Ratio</span>
                    <span class="btn-stat-val">${b.congestionMultiplier}x Target</span>
                </div>
                <div class="btn-stat-item">
                    <span class="btn-stat-label">Affected Visits</span>
                    <span class="btn-stat-val">${b.affectedVisits} pts</span>
                </div>
            </div>

            <div class="btn-rationale">
                ${b.rationale}
            </div>
        `;
        container.appendChild(div);
    });
}

function renderDailyTrendsChart() {
    const ctx = document.getElementById('chart-daily-trends');
    if (!ctx) return;

    if (chartInstances.daily) chartInstances.daily.destroy();

    const labels = appData.daily.map(d => d.visitDate);
    const visits = appData.daily.map(d => d.uniqueVisits);
    const waits = appData.daily.map(d => d.avgWaitTimeMin);

    chartInstances.daily = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Daily Patient Visits',
                    data: visits,
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    fill: true,
                    tension: 0.3,
                    yAxisID: 'y'
                },
                {
                    label: 'Daily Avg Wait (min)',
                    data: waits,
                    borderColor: '#f43f5e',
                    borderWidth: 2,
                    tension: 0.3,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.04)' } },
                y: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'Visits', color: '#3b82f6' },
                    ticks: { color: '#3b82f6' },
                    grid: { color: 'rgba(255,255,255,0.04)' }
                },
                y1: {
                    type: 'linear',
                    position: 'right',
                    title: { display: true, text: 'Wait Time (min)', color: '#f43f5e' },
                    ticks: { color: '#f43f5e' },
                    grid: { drawOnChartArea: false }
                }
            }
        }
    });
}

// -------------------------- Pipeline & Upload ---------------------------- //
async function checkPipelineStatus() {
    const box = document.getElementById('pipeline-exec-status');
    box.textContent = 'Checking cluster services...';
    try {
        const res = await fetch(`${API_BASE}/pipeline/status`).then(r => r.json());
        box.textContent = JSON.stringify(res, null, 2);
    } catch (e) {
        box.textContent = `Error connecting to pipeline endpoint: ${e.message}`;
    }
}

async function triggerPipelineRun() {
    const box = document.getElementById('pipeline-exec-status');
    const btn = document.getElementById('btn-run-pipeline');
    btn.disabled = true;
    box.textContent = 'Executing end-to-end Big Data pipeline (Java JDBC -> HDFS -> Pig -> Hive -> MongoDB)... Please wait ~20-30 seconds...';

    try {
        const res = await fetch(`${API_BASE}/pipeline/run`, { method: 'POST' }).then(r => r.json());
        box.textContent = res.executionLog || JSON.stringify(res, null, 2);
        fetchAllData(true);
    } catch (e) {
        box.textContent = `Pipeline trigger failed: ${e.message}`;
    } finally {
        btn.disabled = false;
    }
}

function setupCsvDropzone() {
    const fileInput = document.getElementById('csv-file-input');
    const reportBox = document.getElementById('validation-report-box');

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;

        reportBox.textContent = `Inspecting ${file.name} (${(file.size/1024).toFixed(1)} KB)...`;
        const reader = new FileReader();
        reader.onload = function(evt) {
            const content = evt.target.result;
            const lines = content.split('\n');
            const header = lines[0].trim().split(',');

            const requiredCols = ['stage_id', 'visit_id', 'arrival_ts', 'service_start_ts', 'service_end_ts'];
            const missing = requiredCols.filter(c => !header.includes(c));

            if (missing.length > 0) {
                reportBox.innerHTML = `<span style="color:var(--accent-rose)">✗ Validation Error: Missing required columns: ${missing.join(', ')}</span>`;
            } else {
                let nullCount = 0;
                let validCount = 0;
                for (let i = 1; i < Math.min(lines.length, 500); i++) {
                    const row = lines[i].split(',');
                    if (row.length >= 5) {
                        if (!row[2] || !row[3] || !row[4]) nullCount++;
                        else validCount++;
                    }
                }
                reportBox.innerHTML = `
                    <span style="color:var(--accent-emerald)">✓ Valid MediQueue Schema Detected!</span><br>
                    - Total Sample Inspected: ${validCount + nullCount} rows<br>
                    - Clean Timestamps: ${validCount}<br>
                    - Null/Incomplete Timestamps Detected: ${nullCount} (Will be cleaned by Apache Pig pipeline)`;
            }
        };
        reader.readAsText(file);
    });
}

// --------------------------- Fallback Demo Data -------------------------- //
function loadFallbackData() {
    appData.overview = {
        totalVisits: 5000,
        totalStages: 22850,
        totalDepartments: 9,
        avgWaitTimeMin: 19.12,
        avgServiceDurationMin: 18.45,
        avgTotalVisitDurationMin: 68.5,
        bottlenecksCount: 66,
        peakCongestionPeriod: "11:00"
    };

    appData.departments = [
        { departmentId: 5, departmentCode: "RAD", departmentName: "Radiology", totalVisitsServed: 3412, avgWaitTimeMin: 42.8, medianWaitTimeMin: 38.0, minWaitTimeMin: 8, maxWaitTimeMin: 110, p90WaitTimeMin: 78.5, avgServiceDurMin: 22.4, targetWaitMin: 25, pctExceedingTarget: 48.2, status: "CONGESTED", waitTimeRank: 1 },
        { departmentId: 7, departmentCode: "PHARM", departmentName: "Central Pharmacy", totalVisitsServed: 4210, avgWaitTimeMin: 28.5, medianWaitTimeMin: 24.0, minWaitTimeMin: 5, maxWaitTimeMin: 85, p90WaitTimeMin: 54.0, avgServiceDurMin: 8.2, targetWaitMin: 10, pctExceedingTarget: 56.4, status: "CONGESTED", waitTimeRank: 2 },
        { departmentId: 3, departmentCode: "ORTHO", departmentName: "Orthopedics", totalVisitsServed: 2890, avgWaitTimeMin: 24.1, medianWaitTimeMin: 21.0, minWaitTimeMin: 6, maxWaitTimeMin: 72, p90WaitTimeMin: 44.0, avgServiceDurMin: 18.5, targetWaitMin: 20, pctExceedingTarget: 36.1, status: "CONGESTED", waitTimeRank: 3 },
        { departmentId: 2, departmentCode: "CARD", departmentName: "Cardiology", totalVisitsServed: 2450, avgWaitTimeMin: 19.8, medianWaitTimeMin: 18.0, minWaitTimeMin: 5, maxWaitTimeMin: 55, p90WaitTimeMin: 34.0, avgServiceDurMin: 21.0, targetWaitMin: 20, pctExceedingTarget: 22.5, status: "NORMAL", waitTimeRank: 4 },
        { departmentId: 1, departmentCode: "OPD", departmentName: "Outpatient General Medicine", totalVisitsServed: 5000, avgWaitTimeMin: 14.6, medianWaitTimeMin: 13.0, minWaitTimeMin: 3, maxWaitTimeMin: 42, p90WaitTimeMin: 26.0, avgServiceDurMin: 15.2, targetWaitMin: 15, pctExceedingTarget: 18.0, status: "NORMAL", waitTimeRank: 5 },
        { departmentId: 4, departmentCode: "PEDS", departmentName: "Pediatrics", totalVisitsServed: 1980, avgWaitTimeMin: 14.2, medianWaitTimeMin: 12.0, minWaitTimeMin: 4, maxWaitTimeMin: 38, p90WaitTimeMin: 25.0, avgServiceDurMin: 14.8, targetWaitMin: 15, pctExceedingTarget: 16.5, status: "NORMAL", waitTimeRank: 6 },
        { departmentId: 6, departmentCode: "LAB", departmentName: "Pathology & Biochemistry", totalVisitsServed: 3890, avgWaitTimeMin: 13.9, medianWaitTimeMin: 12.0, minWaitTimeMin: 4, maxWaitTimeMin: 40, p90WaitTimeMin: 24.0, avgServiceDurMin: 11.0, targetWaitMin: 15, pctExceedingTarget: 14.2, status: "NORMAL", waitTimeRank: 7 },
        { departmentId: 9, departmentCode: "SURG", departmentName: "General Surgery Clinic", totalVisitsServed: 1620, avgWaitTimeMin: 12.5, medianWaitTimeMin: 11.0, minWaitTimeMin: 3, maxWaitTimeMin: 35, p90WaitTimeMin: 22.0, avgServiceDurMin: 24.5, targetWaitMin: 25, pctExceedingTarget: 8.5, status: "NORMAL", waitTimeRank: 8 },
        { departmentId: 8, departmentCode: "EMERG", departmentName: "Emergency & Trauma", totalVisitsServed: 850, avgWaitTimeMin: 6.8, medianWaitTimeMin: 5.0, minWaitTimeMin: 1, maxWaitTimeMin: 20, p90WaitTimeMin: 12.0, avgServiceDurMin: 28.0, targetWaitMin: 10, pctExceedingTarget: 4.2, status: "NORMAL", waitTimeRank: 9 }
    ];

    appData.hourly = [
        { arrivalHour: 6, hourLabel: "06:00", totalPatients: 142, avgWaitTimeMin: 8.5, avgServiceDurMin: 12.0, pctCongested: 2.1 },
        { arrivalHour: 7, hourLabel: "07:00", totalPatients: 310, avgWaitTimeMin: 11.2, avgServiceDurMin: 13.5, pctCongested: 5.4 },
        { arrivalHour: 8, hourLabel: "08:00", totalPatients: 460, avgWaitTimeMin: 15.8, avgServiceDurMin: 15.0, pctCongested: 12.0 },
        { arrivalHour: 9, hourLabel: "09:00", totalPatients: 620, avgWaitTimeMin: 22.4, avgServiceDurMin: 16.5, pctCongested: 28.5 },
        { arrivalHour: 10, hourLabel: "10:00", totalPatients: 710, avgWaitTimeMin: 29.8, avgServiceDurMin: 18.0, pctCongested: 42.1 },
        { arrivalHour: 11, hourLabel: "11:00", totalPatients: 780, avgWaitTimeMin: 33.5, avgServiceDurMin: 19.2, pctCongested: 48.0 },
        { arrivalHour: 12, hourLabel: "12:00", totalPatients: 490, avgWaitTimeMin: 21.0, avgServiceDurMin: 16.0, pctCongested: 24.0 },
        { arrivalHour: 13, hourLabel: "13:00", totalPatients: 350, avgWaitTimeMin: 14.5, avgServiceDurMin: 14.0, pctCongested: 10.2 },
        { arrivalHour: 14, hourLabel: "14:00", totalPatients: 380, avgWaitTimeMin: 15.2, avgServiceDurMin: 14.5, pctCongested: 11.5 },
        { arrivalHour: 15, hourLabel: "15:00", totalPatients: 430, avgWaitTimeMin: 17.0, avgServiceDurMin: 15.0, pctCongested: 15.8 },
        { arrivalHour: 16, hourLabel: "16:00", totalPatients: 550, avgWaitTimeMin: 23.5, avgServiceDurMin: 17.0, pctCongested: 31.0 },
        { arrivalHour: 17, hourLabel: "17:00", totalPatients: 640, avgWaitTimeMin: 31.2, avgServiceDurMin: 18.5, pctCongested: 46.2 },
        { arrivalHour: 18, hourLabel: "18:00", totalPatients: 520, avgWaitTimeMin: 27.4, avgServiceDurMin: 17.5, pctCongested: 38.0 },
        { arrivalHour: 19, hourLabel: "19:00", totalPatients: 280, avgWaitTimeMin: 16.0, avgServiceDurMin: 14.0, pctCongested: 14.0 },
        { arrivalHour: 20, hourLabel: "20:00", totalPatients: 160, avgWaitTimeMin: 10.2, avgServiceDurMin: 12.5, pctCongested: 4.0 }
    ];

    appData.stages = [
        { sequence: 1, stageName: "REGISTRATION", totalPatientsProcessed: 5000, avgQueueWaitMin: 12.4, avgServiceDurMin: 5.6, avgTotalStageMin: 18.0 },
        { sequence: 2, stageName: "CONSULTATION", totalPatientsProcessed: 5000, avgQueueWaitMin: 19.8, avgServiceDurMin: 18.2, avgTotalStageMin: 38.0 },
        { sequence: 3, stageName: "DIAGNOSTIC", totalPatientsProcessed: 3200, avgQueueWaitMin: 27.5, avgServiceDurMin: 20.4, avgTotalStageMin: 47.9 },
        { sequence: 4, stageName: "PHARMACY", totalPatientsProcessed: 4210, avgQueueWaitMin: 28.5, avgServiceDurMin: 8.2, avgTotalStageMin: 36.7 },
        { sequence: 5, stageName: "DISCHARGE", totalPatientsProcessed: 4750, avgQueueWaitMin: 8.2, avgServiceDurMin: 6.5, avgTotalStageMin: 14.7 }
    ];

    appData.bottlenecks = [
        { bottleneckId: "BTN-0001", departmentId: 5, departmentName: "Radiology", stageName: "DIAGNOSTIC", arrivalHour: 10, hourWindow: "10:00 - 11:00", affectedVisits: 142, avgWaitTimeMin: 82.4, peakWaitTimeMin: 110, targetWaitMin: 25, congestionMultiplier: 3.3, severity: "CRITICAL", distinctDaysObserved: 6, rationale: "Queue wait time averaged 82.4 min (3.3x of 25m target) across 142 patients during Monday 10:00 morning diagnostic peak." },
        { bottleneckId: "BTN-0002", departmentId: 7, departmentName: "Central Pharmacy", stageName: "PHARMACY", arrivalHour: 17, hourWindow: "17:00 - 18:00", affectedVisits: 310, avgWaitTimeMin: 64.8, peakWaitTimeMin: 85, targetWaitMin: 10, congestionMultiplier: 6.48, severity: "CRITICAL", distinctDaysObserved: 28, rationale: "Prescription collection queue averaged 64.8 min (6.5x of 10m target) during daily 17:00 evening discharge surge." },
        { bottleneckId: "BTN-0003", departmentId: 5, departmentName: "Radiology", stageName: "DIAGNOSTIC", arrivalHour: 11, hourWindow: "11:00 - 12:00", affectedVisits: 128, avgWaitTimeMin: 58.2, peakWaitTimeMin: 88, targetWaitMin: 25, congestionMultiplier: 2.33, severity: "HIGH", distinctDaysObserved: 6, rationale: "Subsequent hour spillover following 10:00 MRI queue buildup, exceeding target by 2.3x." },
        { bottleneckId: "BTN-0004", departmentId: 7, departmentName: "Central Pharmacy", stageName: "PHARMACY", arrivalHour: 18, hourWindow: "18:00 - 19:00", affectedVisits: 260, avgWaitTimeMin: 49.5, peakWaitTimeMin: 74, targetWaitMin: 10, congestionMultiplier: 4.95, severity: "HIGH", distinctDaysObserved: 27, rationale: "Extended evening prescription dispensing backlog across 27 distinct days." },
        { bottleneckId: "BTN-0005", departmentId: 3, departmentName: "Orthopedics", stageName: "CONSULTATION", arrivalHour: 9, hourWindow: "09:00 - 10:00", affectedVisits: 98, avgWaitTimeMin: 42.1, peakWaitTimeMin: 68, targetWaitMin: 20, congestionMultiplier: 2.11, severity: "HIGH", distinctDaysObserved: 5, rationale: "Wednesday walk-in clinic surge causing consultations to backup past 40 minutes." },
        { bottleneckId: "BTN-0006", departmentId: 2, departmentName: "Cardiology", stageName: "CONSULTATION", arrivalHour: 10, hourWindow: "10:00 - 11:00", affectedVisits: 75, avgWaitTimeMin: 34.0, peakWaitTimeMin: 52, targetWaitMin: 20, congestionMultiplier: 1.70, severity: "MODERATE", distinctDaysObserved: 12, rationale: "Morning clinic ECG triage delays extending wait times by 1.7x target." }
    ];

    appData.journeys = [
        {
            visitId: "VST-00000001", patientId: "PAT-0001364", ageGroup: "36-50", gender: "F", admissionType: "ROUTINE", visitDate: "2026-10-09",
            totalStagesCount: 3, cumulativeWaitMin: 46, cumulativeServiceMin: 25, totalJourneyDurationMin: 71,
            stages: [
                { stageSequence: 1, stageName: "REGISTRATION", departmentName: "Outpatient General Medicine", arrivalTs: "2026-10-09 12:41:21", serviceStartTs: "2026-10-09 12:59:35", serviceEndTs: "2026-10-09 13:03:51", waitTimeMin: 18, serviceDurMin: 4, totalStageTimeMin: 22 },
                { stageSequence: 2, stageName: "CONSULTATION", departmentName: "General Surgery Clinic", arrivalTs: "2026-10-09 13:07:51", serviceStartTs: "2026-10-09 13:23:12", serviceEndTs: "2026-10-09 13:35:59", waitTimeMin: 15, serviceDurMin: 12, totalStageTimeMin: 27 },
                { stageSequence: 3, stageName: "PHARMACY", departmentName: "Central Pharmacy", arrivalTs: "2026-10-09 13:41:59", serviceStartTs: "2026-10-09 13:55:07", serviceEndTs: "2026-10-09 14:04:27", waitTimeMin: 13, serviceDurMin: 9, totalStageTimeMin: 22 }
            ]
        },
        {
            visitId: "VST-00000002", patientId: "PAT-0001736", ageGroup: "65+", gender: "F", admissionType: "OPD", visitDate: "2026-09-23",
            totalStagesCount: 4, cumulativeWaitMin: 114, cumulativeServiceMin: 42, totalJourneyDurationMin: 156,
            stages: [
                { stageSequence: 1, stageName: "REGISTRATION", departmentName: "Outpatient General Medicine", arrivalTs: "2026-09-23 16:30:00", serviceStartTs: "2026-09-23 16:45:00", serviceEndTs: "2026-09-23 16:50:00", waitTimeMin: 15, serviceDurMin: 5, totalStageTimeMin: 20 },
                { stageSequence: 2, stageName: "CONSULTATION", departmentName: "Cardiology", arrivalTs: "2026-09-23 16:55:00", serviceStartTs: "2026-09-23 17:15:00", serviceEndTs: "2026-09-23 17:35:00", waitTimeMin: 20, serviceDurMin: 20, totalStageTimeMin: 40 },
                { stageSequence: 3, stageName: "DIAGNOSTIC", departmentName: "Pathology & Biochemistry", arrivalTs: "2026-09-23 17:40:00", serviceStartTs: "2026-09-23 17:49:00", serviceEndTs: "2026-09-23 17:55:00", waitTimeMin: 9, serviceDurMin: 6, totalStageTimeMin: 15 },
                { stageSequence: 4, stageName: "PHARMACY", departmentName: "Central Pharmacy", arrivalTs: "2026-09-23 18:51:20", serviceStartTs: "2026-09-23 20:02:00", serviceEndTs: "2026-09-23 20:13:13", waitTimeMin: 70, serviceDurMin: 11, totalStageTimeMin: 81 }
            ]
        }
    ];

    appData.daily = [
        { visitDate: "2026-09-15", uniqueVisits: 172, totalStages: 780, avgWaitTimeMin: 18.2, avgServiceDurMin: 16.5 },
        { visitDate: "2026-09-16", uniqueVisits: 185, totalStages: 840, avgWaitTimeMin: 19.5, avgServiceDurMin: 17.0 },
        { visitDate: "2026-09-17", uniqueVisits: 168, totalStages: 760, avgWaitTimeMin: 18.0, avgServiceDurMin: 16.2 },
        { visitDate: "2026-09-18", uniqueVisits: 195, totalStages: 890, avgWaitTimeMin: 20.4, avgServiceDurMin: 17.5 },
        { visitDate: "2026-09-19", uniqueVisits: 140, totalStages: 620, avgWaitTimeMin: 16.8, avgServiceDurMin: 15.0 },
        { visitDate: "2026-09-20", uniqueVisits: 95, totalStages: 410, avgWaitTimeMin: 14.2, avgServiceDurMin: 14.5 },
        { visitDate: "2026-09-21", uniqueVisits: 210, totalStages: 950, avgWaitTimeMin: 23.5, avgServiceDurMin: 18.2 },
        { visitDate: "2026-09-22", uniqueVisits: 190, totalStages: 870, avgWaitTimeMin: 19.8, avgServiceDurMin: 17.1 },
        { visitDate: "2026-09-23", uniqueVisits: 182, totalStages: 825, avgWaitTimeMin: 19.1, avgServiceDurMin: 16.8 },
        { visitDate: "2026-09-24", uniqueVisits: 178, totalStages: 810, avgWaitTimeMin: 18.7, avgServiceDurMin: 16.5 }
    ];
}
