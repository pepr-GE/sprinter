/**
 * SPRINTER – Gantt diagram JavaScript
 * ============================================================
 * Inicializuje frappe-gantt pro zobrazení Ganttova diagramu.
 * Data jsou načítána z REST API /api/v1/projects/{id}/gantt-items.
 * ============================================================
 */

'use strict';

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('ganttChart');
    if (!container) return;

    const projectId = container.getAttribute('data-project-id');
    if (!projectId) return;

    initGanttChart(container, projectId);

    // Přepínání pohledu
    document.getElementById('viewDay')  ?.addEventListener('click', () => setViewMode('Day'));
    document.getElementById('viewWeek') ?.addEventListener('click', () => setViewMode('Week'));
    document.getElementById('viewMonth')?.addEventListener('click', () => setViewMode('Month'));
});

let ganttInstance = null;

/**
 * Načte data a inicializuje Ganttův diagram.
 */
async function initGanttChart(container, projectId) {
    try {
        container.innerHTML = '<div class="skeleton" style="height:400px;border-radius:8px;"></div>';

        const ctxPath = document.querySelector('meta[name="context-path"]')?.content?.replace(/\/$/, '') || '';
        const resp = await fetch(`${ctxPath}/api/v1/projects/${projectId}/gantt-items`);

        if (!resp.ok) {
            throw new Error(`Server vrátil chybu ${resp.status}: ${resp.statusText}`);
        }

        const items = await resp.json();

        if (!items || items.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="bi bi-bar-chart-steps fs-2 d-block mb-2"></i>
                    Žádné položky s daty pro Ganttův diagram
                </div>`;
            return;
        }

        const today = new Date().toISOString().split('T')[0];

        // Transformace dat do formátu frappe-gantt
        const tasks = items.map(item => {
            // Pokud chybí start, odvodíme ho 7 dní před koncem; pokud chybí konec, 7 dní po začátku
            const startStr = item.startDate || (item.endDate ? addDays(item.endDate, -7) : today);
            const endStr   = item.endDate   || addDays(startStr, 7);

            return {
                id:           String(item.id),
                name:         item.text,
                start:        startStr,
                end:          endStr,
                progress:     item.progress || 0,
                dependencies: '',
                custom_class: getGanttBarClass(item.type, item.status)
            };
        });

        // Čistý kontejner
        container.innerHTML = '';

        // Inicializace frappe-gantt
        ganttInstance = new Gantt(container, tasks, {
            header_height:    50,
            column_width:     30,
            step:             24,
            view_modes:       ['Day', 'Week', 'Month'],
            bar_height:       28,
            bar_corner_radius: 4,
            arrow_curve:      5,
            padding:          18,
            view_mode:        'Week',
            date_format:      'YYYY-MM-DD',
            popup_trigger:    'click',

            on_click: (task) => {
                const itemId = task.id.replace('wi-', '');
                const cp = document.querySelector('meta[name="context-path"]')?.content?.replace(/\/$/, '') || '';
                window.location.href = `${cp}/items/${itemId}`;
            },

            on_view_change: (mode) => {
                updateViewButtons(mode);
            }
        });

    } catch (err) {
        console.error('Chyba Gantt diagramu:', err);
        container.innerHTML = `
            <div class="alert alert-danger m-3">
                <strong>Chyba při načítání dat pro Ganttův diagram.</strong>
                <div class="small text-muted mt-1">${err.message}</div>
            </div>`;
    }
}

/**
 * Přepíná pohled (Den / Týden / Měsíc).
 */
function setViewMode(mode) {
    if (ganttInstance) {
        ganttInstance.change_view_mode(mode);
        updateViewButtons(mode);
    }
}

/**
 * Aktualizuje vizuál přepínacích tlačítek pohledu.
 */
function updateViewButtons(activeMode) {
    const mapping = { Day: 'viewDay', Week: 'viewWeek', Month: 'viewMonth' };
    Object.entries(mapping).forEach(([mode, id]) => {
        const btn = document.getElementById(id);
        if (btn) {
            btn.classList.toggle('active', mode === activeMode);
        }
    });
}

/**
 * Vrátí CSS třídu pro pruh Ganttova diagramu dle typu a stavu položky.
 */
function getGanttBarClass(type, status) {
    if (status === 'DONE' || status === 'CANCELLED') return 'gantt-bar-done';
    const classes = {
        TASK:  'gantt-bar-task',
        ISSUE: 'gantt-bar-issue',
        STORY: 'gantt-bar-story',
        EPIC:  'gantt-bar-epic'
    };
    return classes[type] || 'gantt-bar-task';
}

/**
 * Přidá k datu N dní.
 */
function addDays(dateStr, days) {
    const date = new Date(dateStr);
    date.setDate(date.getDate() + days);
    return date.toISOString().split('T')[0];
}
