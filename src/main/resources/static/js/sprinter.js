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
    initThemeToggle();
    initDatepickers();
    initTooltips();
    initGlobalSearch();
    initKeyboardShortcuts();
    initAutoHideAlerts();
});

/* ============================================================
   PŘEPÍNAČ TÉMATU (LIGHT / DARK)
   ============================================================ */

/**
 * Inicializuje přepínač tmavého/světlého tématu.
 * Téma se ukládá do localStorage pro okamžitou aplikaci,
 * ale výsledek se persistuje na serveru přes POST /profile/theme.
 */
function initThemeToggle() {
    const toggleBtn = document.getElementById('themeToggle');
    if (!toggleBtn) return;

    // Načtení aktuálního tématu
    const body        = document.body;
    const currentTheme = body.getAttribute('data-bs-theme') || 'light';

    toggleBtn.addEventListener('click', async () => {
        const current = body.getAttribute('data-bs-theme') || 'light';
        const newTheme = current === 'light' ? 'dark' : 'light';

        // Okamžitá aplikace (bez čekání na server)
        applyTheme(newTheme);

        // Uložení na server
        try {
            const resp = await fetch('/sprinter/profile/theme', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `theme=${newTheme}&returnTo=${encodeURIComponent(window.location.pathname)}`
            });
            // Pokud server vrátí redirect, ignorujeme – téma je již aplikováno
        } catch (e) {
            console.warn('Nelze uložit téma na server:', e);
        }

        // Uložení do localStorage jako záloha
        localStorage.setItem('sprinter-theme', newTheme);
    });
}

/**
 * Aplikuje téma na element <body>.
 * @param {string} theme - 'light' nebo 'dark'
 */
function applyTheme(theme) {
    document.body.setAttribute('data-bs-theme', theme);
    document.body.classList.toggle('theme-dark', theme === 'dark');
    document.body.classList.toggle('theme-light', theme === 'light');
}

// Okamžitá aplikace tématu z localStorage (před DOMContentLoaded,
// aby se zabránilo "bliknutí" špatného tématu)
(function () {
    const saved = localStorage.getItem('sprinter-theme');
    if (saved && (saved === 'light' || saved === 'dark')) {
        document.documentElement.setAttribute('data-bs-theme', saved);
    }
})();

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

    let searchTimeout;

    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        const query = e.target.value.trim();
        if (query.length < 2) return;

        searchTimeout = setTimeout(() => {
            // TODO: implementovat globální vyhledávání přes API
            // a zobrazit výsledky v dropdown
        }, 300);
    });

    // Zavření dropdown při kliknutí mimo
    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target)) {
            // skrýt dropdown
        }
    });
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
    patch:     sprinterPatch,
    showToast: showToast,
    applyTheme
};
