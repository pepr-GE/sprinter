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
 * @param {HTMLElement} container - container element
 * @param {string}      projectId - ID projektu
 */
async function initGanttChart(container, projectId) {
    try {
        // Zobrazení skeleton loadingu
        container.innerHTML = '<div class="skeleton" style="height:400px;border-radius:8px;"></div>';

        const resp  = await fetch(`/sprinter/api/v1/projects/${projectId}/gantt-items`);
        const items = await resp.json();

        if (!items || items.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="bi bi-bar-chart-steps fs-2 d-block mb-2"></i>
                    Žádné položky s daty pro Ganttův diagram
                </div>`;
            return;
        }

        // Transformace dat do formátu frappe-gantt
        const tasks = items.map(item => ({
            id:          item.id,
            name:        item.text,
            start:       item.startDate || new Date().toISOString().split('T')[0],
            end:         item.endDate   || addDays(item.startDate || new Date().toISOString().split('T')[0], 7),
            progress:    item.progress,
            dependencies: item.dependencies || [],
            custom_class: getGanttBarClass(item.type, item.status)
        }));

        // Čistý kontejner
        container.innerHTML = '';

        // Inicializace frappe-gantt
        ganttInstance = new Gantt(container, tasks, {
            header_height:   50,
            column_width:    30,
            step:            24,
            view_modes:      ['Day', 'Week', 'Month'],
            bar_height:      28,
            bar_corner_radius: 4,
            arrow_curve:     5,
            padding:         18,
            view_mode:       'Week',
            date_format:     'YYYY-MM-DD',
            language:        'cs',

            // Klik na úkol otevře detail
            on_click: (task) => {
                const itemId = task.id.replace('wi-', '');
                window.location.href = `/sprinter/items/${itemId}`;
            },

            // Hover tooltip
            on_view_change: (mode) => {
                updateViewButtons(mode);
            }
        });

    } catch (err) {
        console.error('Chyba při načítání Gantt diagramu:', err);
        container.innerHTML = `
            <div class="alert alert-danger m-3">
                Chyba při načítání dat pro Ganttův diagram.
            </div>`;
    }
}

/**
 * Přepíná pohled (Den / Týden / Měsíc).
 * @param {string} mode - 'Day' | 'Week' | 'Month'
 */
function setViewMode(mode) {
    if (ganttInstance) {
        ganttInstance.change_view_mode(mode);
        updateViewButtons(mode);
    }
}

/**
 * Aktualizuje vizuál přepínacích tlačítek pohledu.
 * @param {string} activeMode
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
 * @param {string} type   - typ položky (TASK, ISSUE, STORY, EPIC)
 * @param {string} status - stav položky
 * @returns {string} CSS třída
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
 * @param {string} dateStr - ISO datum (YYYY-MM-DD)
 * @param {number} days    - počet dní
 * @returns {string} nové datum
 */
function addDays(dateStr, days) {
    const date = new Date(dateStr);
    date.setDate(date.getDate() + days);
    return date.toISOString().split('T')[0];
}
