/**
 * SPRINTER – hlavní JavaScript soubor
 * ============================================================
 * Obsahuje inicializaci globálních komponent:
 * - Přepínač tmavého/světlého tématu
 * - Flatpickr datepickery
 * - Bootstrap tooltips a popovers
 * - Globální vyhledávání
 * - Klávesové zkratky
 * ============================================================
 */

'use strict';

/* ============================================================
   INICIALIZACE PO NAČTENÍ DOKUMENTU
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    // Synchronizace localStorage s tématem uloženým na serveru
    const serverTheme = document.body.getAttribute('data-bs-theme') || 'light';
    localStorage.setItem('sprinter-theme', serverTheme);

    initDatepickers();
    initTooltips();
    initGlobalSearch();
    initKeyboardShortcuts();
    initAutoHideAlerts();
});

/* ============================================================
   TÉMA (LIGHT / DARK) — jen okamžitá aplikace z localStorage
   ============================================================ */

// Okamžitá aplikace tématu z localStorage (před DOMContentLoaded,
// aby se zabránilo "bliknutí" špatného tématu).
// Změna tématu se provádí na stránce profilu uživatele.
(function () {
    const saved = localStorage.getItem('sprinter-theme');
    if (saved && (saved === 'light' || saved === 'dark')) {
        document.documentElement.setAttribute('data-bs-theme', saved);
    }
})();

function applyTheme(theme) {
    document.body.setAttribute('data-bs-theme', theme);
    document.body.classList.toggle('theme-dark', theme === 'dark');
    document.body.classList.toggle('theme-light', theme === 'light');
    localStorage.setItem('sprinter-theme', theme);
}

/* ============================================================
   FLATPICKR DATEPICKERY
   ============================================================ */

/**
 * Inicializuje Flatpickr na všech input[type=date].
 */
function initDatepickers() {
    if (typeof flatpickr === 'undefined') return;

    // Česká lokalizace
    const csLocale = {
        firstDayOfWeek: 1,
        weekdays: {
            shorthand: ['Ne', 'Po', 'Út', 'St', 'Čt', 'Pá', 'So'],
            longhand:  ['Neděle', 'Pondělí', 'Úterý', 'Středa', 'Čtvrtek', 'Pátek', 'Sobota']
        },
        months: {
            shorthand: ['Led','Úno','Bře','Dub','Kvě','Čvn','Čvc','Srp','Zář','Říj','Lis','Pro'],
            longhand:  ['Leden','Únor','Březen','Duben','Květen','Červen',
                        'Červenec','Srpen','Září','Říjen','Listopad','Prosinec']
        }
    };

    document.querySelectorAll('input[type="date"], .datepicker').forEach(el => {
        flatpickr(el, {
            locale:      csLocale,
            dateFormat:  'Y-m-d',
            allowInput:  true,
            altInput:    true,
            altFormat:   'j. n. Y',
            disableMobile: false
        });
    });
}

/* ============================================================
   BOOTSTRAP TOOLTIPS
   ============================================================ */

function initTooltips() {
    // Inicializace Bootstrap tooltipů
    const tooltipEls = document.querySelectorAll('[data-bs-toggle="tooltip"], [title]:not([data-bs-toggle])');
    tooltipEls.forEach(el => {
        if (el.getAttribute('data-bs-toggle') !== 'tooltip' && el.title) {
            el.setAttribute('data-bs-toggle', 'tooltip');
        }
        try {
            new bootstrap.Tooltip(el, {
                trigger: 'hover',
                delay: { show: 300, hide: 100 }
            });
        } catch (e) { /* Bootstrap možná ještě není načten */ }
    });
}

/* ============================================================
   GLOBÁLNÍ VYHLEDÁVÁNÍ
   ============================================================ */

function initGlobalSearch() {
    const searchInput = document.getElementById('globalSearch');
    if (!searchInput) return;

    const wrapper = searchInput.closest('.sprinter-search') || searchInput.parentElement;
    wrapper.style.position = 'relative';
    const dropdown = document.createElement('div');
    dropdown.id = 'searchDropdown';
    dropdown.className = 'search-dropdown';
    dropdown.style.display = 'none';
    wrapper.appendChild(dropdown);

    let searchTimeout;
    const ctxPath = document.querySelector('meta[name="context-path"]')?.content?.replace(/\/$/, '') || '';

    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        const query = e.target.value.trim();
        if (query.length < 2) { dropdown.style.display = 'none'; return; }
        searchTimeout = setTimeout(() => performSearch(query, ctxPath, dropdown), 280);
    });

    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') { dropdown.style.display = 'none'; searchInput.blur(); }
    });

    document.addEventListener('click', (e) => {
        if (!wrapper.contains(e.target)) dropdown.style.display = 'none';
    });
}

async function performSearch(query, ctxPath, dropdown) {
    try {
        const resp = await fetch(`${ctxPath}/api/v1/search?q=${encodeURIComponent(query)}`);
        if (!resp.ok) return;
        renderSearchDropdown(await resp.json(), ctxPath, dropdown);
    } catch (e) { console.warn('Chyba vyhledávání:', e); }
}

function renderSearchDropdown(data, ctxPath, dropdown) {
    const hasProjects  = data.projects  && data.projects.length  > 0;
    const hasItems     = data.items     && data.items.length     > 0;
    const hasDocuments = data.documents && data.documents.length > 0;
    if (!hasProjects && !hasItems && !hasDocuments) {
        dropdown.innerHTML = '<div class="search-dropdown-empty">Nic nenalezeno</div>';
        dropdown.style.display = 'block';
        return;
    }
    let html = '';
    if (hasProjects) {
        html += '<div class="search-dropdown-section">Projekty</div>';
        data.projects.forEach(p => {
            html += `<a href="${ctxPath}${escapeHtml(p.url)}" class="search-dropdown-item">
                <i class="bi bi-folder2 me-2 text-muted"></i><span>${escapeHtml(p.name)}</span>
                <small class="text-muted ms-auto">${escapeHtml(p.key)}</small></a>`;
        });
    }
    if (hasItems) {
        html += '<div class="search-dropdown-section">Položky</div>';
        data.items.forEach(i => {
            html += `<a href="${ctxPath}${escapeHtml(i.url)}" class="search-dropdown-item">
                <span class="me-2 text-muted small">${escapeHtml(i.key)}</span>
                <span>${escapeHtml(i.title)}</span>
                <small class="text-muted ms-auto">${escapeHtml(i.projectName)}</small></a>`;
        });
    }
    if (hasDocuments) {
        html += '<div class="search-dropdown-section">Dokumenty</div>';
        data.documents.forEach(d => {
            html += `<a href="${ctxPath}${escapeHtml(d.url)}" class="search-dropdown-item">
                <i class="bi bi-file-earmark-text me-2 text-muted"></i><span>${escapeHtml(d.title)}</span>
                <small class="text-muted ms-auto">${d.projectName ? escapeHtml(d.projectName) : ''}</small></a>`;
        });
    }
    dropdown.innerHTML = html;
    dropdown.style.display = 'block';
}

/* ============================================================
   KLÁVESOVÉ ZKRATKY
   ============================================================ */

function initKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Ignorovat pokud je focus v input poli
        const tag = document.activeElement?.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;

        // Ctrl+K / Cmd+K – otevřít globální vyhledávání
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            document.getElementById('globalSearch')?.focus();
        }

        // C – otevřít dialog pro rychlé vytvoření
        if (e.key === 'c' && !e.ctrlKey && !e.metaKey) {
            const modal = document.getElementById('quickCreateModal');
            if (modal) {
                bootstrap.Modal.getOrCreateInstance(modal).show();
            }
        }
    });
}

/* ============================================================
   AUTOMATICKÉ SKRÝVÁNÍ ALERT ZPRÁV
   ============================================================ */

function initAutoHideAlerts() {
    // Úspěšné zprávy zmizí automaticky po 5 sekundách
    document.querySelectorAll('.alert-success').forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });
}

/* ============================================================
   UTILITY FUNKCE
   ============================================================ */

/**
 * Odešle PATCH požadavek na server.
 * @param {string} url     - endpoint URL
 * @param {object} data    - data jako objekt (bude serializován na JSON)
 * @returns {Promise<Response>}
 */
async function sprinterPatch(url, data) {
    return fetch(url, {
        method:  'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Accept':        'application/json'
        },
        body: JSON.stringify(data)
    });
}

/**
 * Zobrazí toast notifikaci.
 * @param {string} message  - text zprávy
 * @param {string} type     - 'success' | 'error' | 'warning' | 'info'
 */
function showToast(message, type = 'info') {
    const icons = {
        success: 'bi-check-circle-fill',
        error:   'bi-exclamation-circle-fill',
        warning: 'bi-exclamation-triangle-fill',
        info:    'bi-info-circle-fill'
    };
    const colors = {
        success: '#22c55e',
        error:   '#ef4444',
        warning: '#f59e0b',
        info:    '#6DA3C7'
    };

    // Vytvoření toast elementu
    const toast = document.createElement('div');
    toast.className = 'sprinter-toast';
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 9999;
        background: var(--bg-surface);
        border: 1px solid var(--border-color);
        border-left: 4px solid ${colors[type]};
        border-radius: 8px;
        padding: 12px 16px;
        box-shadow: var(--shadow-md);
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 280px;
        max-width: 400px;
        animation: fadeIn 0.2s ease;
        font-size: 14px;
    `;
    toast.innerHTML = `
        <i class="bi ${icons[type]}" style="color: ${colors[type]}; font-size: 18px;"></i>
        <span>${escapeHtml(message)}</span>
    `;

    document.body.appendChild(toast);

    // Automatické skrytí po 4 sekundách
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

/**
 * Escapuje HTML specialní znaky.
 * @param {string} str
 * @returns {string}
 */
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// Exportujeme pro použití v jiných modulech
window.Sprinter = {
    patch:      sprinterPatch,
    showToast:  showToast,
    applyTheme: applyTheme
};
