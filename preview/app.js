// Sample realistic installed applications on an Android 14 device
const INITIAL_APPS = [
  { id: 'insta', name: 'Instagram', pkg: 'com.instagram.android', icon: '📸', cacheMB: 840, dataMB: 620, appMB: 142, selected: true },
  { id: 'yt', name: 'YouTube', pkg: 'com.google.android.youtube', icon: '▶️', cacheMB: 720, dataMB: 410, appMB: 180, selected: true },
  { id: 'spot', name: 'Spotify', pkg: 'com.spotify.music', icon: '🎵', cacheMB: 650, dataMB: 1200, appMB: 98, selected: true },
  { id: 'chrome', name: 'Google Chrome', pkg: 'com.android.chrome', icon: '🌐', cacheMB: 512, dataMB: 890, appMB: 195, selected: true },
  { id: 'tiktok', name: 'TikTok', pkg: 'com.zhiliaoapp.musically', icon: '🎬', cacheMB: 480, dataMB: 1420, appMB: 210, selected: true },
  { id: 'wa', name: 'WhatsApp', pkg: 'com.whatsapp', icon: '💬', cacheMB: 320, dataMB: 2840, appMB: 85, selected: true },
  { id: 'reddit', name: 'Reddit', pkg: 'com.reddit.frontpage', icon: '🤖', cacheMB: 240, dataMB: 180, appMB: 62, selected: true },
  { id: 'netflix', name: 'Netflix', pkg: 'com.netflix.mediaclient', icon: '🍿', cacheMB: 180, dataMB: 3400, appMB: 110, selected: true },
  { id: 'maps', name: 'Google Maps', pkg: 'com.google.android.apps.maps', icon: '🗺️', cacheMB: 150, dataMB: 240, appMB: 130, selected: true },
  { id: 'x', name: 'X / Twitter', pkg: 'com.twitter.android', icon: '🐦', cacheMB: 120, dataMB: 310, appMB: 88, selected: true }
];

const INITIAL_JUNK = [
  { id: 'j1', name: 'Gallery Thumbnail Cache', path: '/sdcard/DCIM/.thumbnails', icon: '🖼️', sizeMB: 310, selected: true },
  { id: 'j2', name: 'Temporary App Files', path: '/sdcard/Android/data/.tmp', icon: '📄', sizeMB: 185, selected: true },
  { id: 'j3', name: 'WhatsApp Cached Statuses', path: '/sdcard/WhatsApp/Media/.Statuses', icon: '👀', sizeMB: 140, selected: true },
  { id: 'j4', name: 'System Diagnostic Logs', path: '/sdcard/Android/logs', icon: '📝', sizeMB: 64, selected: true },
  { id: 'j5', name: 'Obsolete APK Installers', path: '/sdcard/Download/installers', icon: '📦', sizeMB: 48, selected: true }
];

let apps = JSON.parse(JSON.stringify(INITIAL_APPS));
let junk = JSON.parse(JSON.stringify(INITIAL_JUNK));
let currentTab = 'apps'; // 'apps' | 'junk'
let sortBySize = true;
let isCleaning = false;
let cleaningCancelRequested = false;

// DOM Elements
const appListContainer = document.getElementById('appListContainer');
const junkListContainer = document.getElementById('junkListContainer');
const displayTotalCache = document.getElementById('displayTotalCache');
const displaySelectedCount = document.getElementById('displaySelectedCount');
const displaySelectedSize = document.getElementById('displaySelectedSize');
const cacheProgressBar = document.getElementById('cacheProgressBar');
const btnCleanSize = document.getElementById('btnCleanSize');
const btnSelectAll = document.getElementById('btnSelectAll');
const selectAllText = document.getElementById('selectAllText');
const chipCheckIcon = document.getElementById('chipCheckIcon');
const btnSortOrder = document.getElementById('btnSortOrder');
const sortOrderText = document.getElementById('sortOrderText');
const tabApps = document.getElementById('tabApps');
const tabJunk = document.getElementById('tabJunk');
const tabAppCount = document.getElementById('tabAppCount');
const tabJunkCount = document.getElementById('tabJunkCount');
const btnStartAutoClean = document.getElementById('btnStartAutoClean');
const statTotalCache = document.getElementById('statTotalCache');
const statSelectedCache = document.getElementById('statSelectedCache');
const statAppsCount = document.getElementById('statAppsCount');

// Modal Elements
const cleanModal = document.getElementById('cleanModal');
const modalAppIcon = document.getElementById('modalAppIcon');
const modalAppName = document.getElementById('modalAppName');
const modalAppPkg = document.getElementById('modalAppPkg');
const modalAppCache = document.getElementById('modalAppCache');
const modalProgressBar = document.getElementById('modalProgressBar');
const modalProgressCount = document.getElementById('modalProgressCount');
const modalFreedBytes = document.getElementById('modalFreedBytes');
const modalLog = document.getElementById('modalLog');
const btnCancelCleaning = document.getElementById('btnCancelCleaning');

// Settings Sheet Elements
const settingsSheet = document.getElementById('settingsSheet');
const btnCloseSheet = document.getElementById('btnCloseSheet');
const sheetAppIcon = document.getElementById('sheetAppIcon');
const sheetAppName = document.getElementById('sheetAppName');
const sheetAppPkg = document.getElementById('sheetAppPkg');
const sheetAppSize = document.getElementById('sheetAppSize');
const sheetDataSize = document.getElementById('sheetDataSize');
const sheetCacheSize = document.getElementById('sheetCacheSize');
const sheetTotalSize = document.getElementById('sheetTotalSize');
const btnManualClearCache = document.getElementById('btnManualClearCache');
let activeSheetApp = null;

// Clock Updater
function updateClock() {
  const now = new Date();
  const h = String(now.getHours()).padStart(2, '0');
  const m = String(now.getMinutes()).padStart(2, '0');
  const clockEl = document.getElementById('statusClock');
  if (clockEl) clockEl.textContent = `${h}:${m}`;
}
setInterval(updateClock, 1000);
updateClock();

// Format Helpers
function formatMB(mb) {
  if (mb >= 1024) {
    return `${(mb / 1024).toFixed(2)} GB`;
  }
  return `${Math.round(mb)} MB`;
}

// Render Apps List
function renderAppList() {
  appListContainer.innerHTML = '';
  
  // Sort
  if (sortBySize) {
    apps.sort((a, b) => b.cacheMB - a.cacheMB);
  } else {
    apps.sort((a, b) => a.name.localeCompare(b.name));
  }

  apps.forEach(app => {
    const card = document.createElement('div');
    card.className = 'app-card';
    card.innerHTML = `
      <div class="app-checkbox ${app.selected ? 'checked' : ''}" data-id="${app.id}">
        ${app.selected ? '✓' : ''}
      </div>
      <div class="app-avatar">${app.icon}</div>
      <div class="app-info">
        <div class="app-name">${app.name}</div>
        <div class="app-pkg">${app.pkg}</div>
      </div>
      <div class="app-cache-size">${app.cacheMB > 0 ? formatMB(app.cacheMB) : '0 B'}</div>
      <button class="btn-open-settings" data-open="${app.id}" title="Open Android App Info Settings">
        ⚙
      </button>
    `;

    // Toggle select on card or checkbox click
    card.addEventListener('click', (e) => {
      if (e.target.closest('.btn-open-settings')) return;
      app.selected = !app.selected;
      updateUI();
    });

    // Open settings sheet
    const btnOpen = card.querySelector('.btn-open-settings');
    btnOpen.addEventListener('click', (e) => {
      e.stopPropagation();
      openSettingsSheet(app);
    });

    appListContainer.appendChild(card);
  });
}

// Render Junk List
function renderJunkList() {
  junkListContainer.innerHTML = '';
  junk.forEach(item => {
    const card = document.createElement('div');
    card.className = 'app-card';
    card.innerHTML = `
      <div class="app-checkbox ${item.selected ? 'checked' : ''}" data-id="${item.id}">
        ${item.selected ? '✓' : ''}
      </div>
      <div class="app-avatar">${item.icon}</div>
      <div class="app-info">
        <div class="app-name">${item.name}</div>
        <div class="app-pkg">${item.path}</div>
      </div>
      <div class="app-cache-size">${item.sizeMB > 0 ? formatMB(item.sizeMB) : '0 B'}</div>
    `;

    card.addEventListener('click', () => {
      item.selected = !item.selected;
      updateUI();
    });

    junkListContainer.appendChild(card);
  });
}

// Update Totals & Metrics
function updateUI() {
  const currentList = currentTab === 'apps' ? apps : junk;
  const totalMB = currentList.reduce((acc, item) => acc + (item.cacheMB !== undefined ? item.cacheMB : item.sizeMB), 0);
  const selectedItems = currentList.filter(item => item.selected);
  const selectedMB = selectedItems.reduce((acc, item) => acc + (item.cacheMB !== undefined ? item.cacheMB : item.sizeMB), 0);

  // Storage Card
  const totalFormatted = formatMB(totalMB);
  const parts = totalFormatted.split(' ');
  displayTotalCache.innerHTML = `${parts[0]} <span>${parts[1]}</span>`;

  const selectedFormatted = formatMB(selectedMB);
  displaySelectedSize.textContent = `Selected: ${selectedFormatted}`;
  displaySelectedCount.textContent = `${selectedItems.length} of ${currentList.length} items selected`;

  const pct = totalMB > 0 ? Math.min(100, Math.round((selectedMB / totalMB) * 100)) : 0;
  cacheProgressBar.style.width = `${pct}%`;

  // CTA Button
  btnCleanSize.textContent = selectedFormatted;

  // Select All button state
  const allSelected = currentList.length > 0 && selectedItems.length === currentList.length;
  if (allSelected) {
    selectAllText.textContent = 'Deselect All';
    chipCheckIcon.classList.add('checked');
    chipCheckIcon.textContent = '✓';
  } else {
    selectAllText.textContent = 'Select All';
    chipCheckIcon.classList.remove('checked');
    chipCheckIcon.textContent = '';
  }

  // Sidebar live stats
  statTotalCache.textContent = totalFormatted;
  statSelectedCache.textContent = selectedFormatted;
  statAppsCount.textContent = `${apps.length} Apps`;

  // Counts on tabs
  tabAppCount.textContent = apps.length;
  tabJunkCount.textContent = junk.length;

  if (currentTab === 'apps') {
    renderAppList();
  } else {
    renderJunkList();
  }
}

// Toggle Select All
btnSelectAll.addEventListener('click', () => {
  const currentList = currentTab === 'apps' ? apps : junk;
  const anyUnselected = currentList.some(i => !i.selected);
  currentList.forEach(i => i.selected = anyUnselected);
  updateUI();
});

// Toggle Sort
btnSortOrder.addEventListener('click', () => {
  sortBySize = !sortBySize;
  sortOrderText.textContent = sortBySize ? 'Size ↓' : 'Name ↑';
  renderAppList();
});

// Switch Tabs
tabApps.addEventListener('click', () => {
  currentTab = 'apps';
  tabApps.classList.add('active');
  tabJunk.classList.remove('active');
  appListContainer.classList.remove('hidden');
  junkListContainer.classList.add('hidden');
  btnSortOrder.style.display = 'inline-flex';
  updateUI();
});

tabJunk.addEventListener('click', () => {
  currentTab = 'junk';
  tabJunk.classList.add('active');
  tabApps.classList.remove('active');
  junkListContainer.classList.remove('hidden');
  appListContainer.classList.add('hidden');
  btnSortOrder.style.display = 'none';
  updateUI();
});

// Simulated Android Settings Sheet for Manual 1-Tap Jump
function openSettingsSheet(app) {
  activeSheetApp = app;
  sheetAppIcon.textContent = app.icon;
  sheetAppName.textContent = app.name;
  sheetAppPkg.textContent = app.pkg;
  sheetAppSize.textContent = formatMB(app.appMB);
  sheetDataSize.textContent = formatMB(app.dataMB);
  sheetCacheSize.textContent = app.cacheMB > 0 ? formatMB(app.cacheMB) : '0 B';
  sheetTotalSize.textContent = formatMB(app.appMB + app.dataMB + app.cacheMB);

  settingsSheet.classList.remove('hidden');
}

btnCloseSheet.addEventListener('click', () => {
  settingsSheet.classList.add('hidden');
  activeSheetApp = null;
});

btnManualClearCache.addEventListener('click', () => {
  if (!activeSheetApp) return;
  activeSheetApp.cacheMB = 0;
  sheetCacheSize.textContent = '0 B';
  sheetTotalSize.textContent = formatMB(activeSheetApp.appMB + activeSheetApp.dataMB);
  updateUI();
  setTimeout(() => {
    settingsSheet.classList.add('hidden');
    activeSheetApp = null;
  }, 400);
});

// Automated Accessibility Cleaning Simulation
btnStartAutoClean.addEventListener('click', () => {
  if (currentTab === 'apps') {
    startAutoCleanSimulation();
  } else {
    cleanJunkFiles();
  }
});

function cleanJunkFiles() {
  const selected = junk.filter(j => j.selected);
  if (selected.length === 0) {
    alert('No junk items selected.');
    return;
  }
  selected.forEach(j => j.sizeMB = 0);
  alert(`Cleaned ${selected.length} shared junk & temporary storage categories!`);
  updateUI();
}

async function startAutoCleanSimulation() {
  const targets = apps.filter(a => a.selected && a.cacheMB > 0);
  if (targets.length === 0) {
    alert('No apps with cache selected to clean.');
    return;
  }

  isCleaning = true;
  cleaningCancelRequested = false;
  cleanModal.classList.remove('hidden');

  let freedTotalMB = 0;
  const totalAppsCount = targets.length;

  for (let i = 0; i < targets.length; i++) {
    if (cleaningCancelRequested) break;
    const target = targets[i];

    // Update modal view
    modalAppIcon.textContent = target.icon;
    modalAppName.textContent = target.name;
    modalAppPkg.textContent = target.pkg;
    modalAppCache.textContent = formatMB(target.cacheMB);

    const progressPct = Math.round(((i) / totalAppsCount) * 100);
    modalProgressBar.style.width = `${progressPct}%`;
    modalProgressCount.textContent = `App ${i + 1} of ${totalAppsCount}`;
    modalFreedBytes.textContent = `Freed: ${formatMB(freedTotalMB)}`;

    logMessage(`▸ Opening Settings for ${target.name}…`);
    await sleep(400);
    if (cleaningCancelRequested) break;

    logMessage(`▸ AccessibilityService: clicked "Storage & cache"`);
    await sleep(350);
    if (cleaningCancelRequested) break;

    logMessage(`▸ AccessibilityService: clicked "Clear cache" button`);
    freedTotalMB += target.cacheMB;
    target.cacheMB = 0;
    modalAppCache.textContent = '0 B';
    modalFreedBytes.textContent = `Freed: ${formatMB(freedTotalMB)}`;
    await sleep(350);
  }

  modalProgressBar.style.width = '100%';
  logMessage(`✨ Batch clean completed successfully!`);
  await sleep(600);

  cleanModal.classList.add('hidden');
  isCleaning = false;
  updateUI();
}

btnCancelCleaning.addEventListener('click', () => {
  cleaningCancelRequested = true;
  cleanModal.classList.add('hidden');
  isCleaning = false;
  updateUI();
});

function logMessage(text) {
  const line = document.createElement('div');
  line.className = 'log-line';
  line.textContent = text;
  modalLog.appendChild(line);
  modalLog.scrollTop = modalLog.scrollHeight;
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Rescan & Reset buttons in left panel
document.getElementById('ctrlRescan').addEventListener('click', () => {
  updateUI();
});

document.getElementById('ctrlReset').addEventListener('click', () => {
  apps = JSON.parse(JSON.stringify(INITIAL_APPS));
  junk = JSON.parse(JSON.stringify(INITIAL_JUNK));
  updateUI();
});

// Initial render
updateUI();
