/**
 * SPRINTER – Reporty JavaScript
 * ============================================================
 * Inicializuje Chart.js grafy na stránce reportů.
 * Data jsou načítána z REST API /api/v1/reports/*.
 * ============================================================
 */

'use strict';

document.addEventListener('DOMContentLoaded', () => {
    initStatusChart();
});

/**
 * Inicializuje pie/doughnut chart stavů položek.
 */
async function initStatusChart() {
    const canvas = document.getElementById('statusChart');
    if (!canvas) return;

    const projectId = canvas.getAttribute('data-project-id');
    if (!projectId) return;

    try {
        const resp = await fetch(`/sprinter/api/v1/reports/projects/${projectId}/status-counts`);
        const data = await resp.json();

        // Detekce aktuálního tématu pro barvy textu
        const isDark = document.body.getAttribute('data-bs-theme') === 'dark';
        const textColor = isDark ? '#e6edf3' : '#172b4d';

        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels:   data.labels,
                datasets: [{
                    data:            data.data,
                    backgroundColor: data.backgroundColor,
                    borderColor:     isDark ? '#1f2937' : '#ffffff',
                    borderWidth:     3,
                    hoverBorderWidth: 4,
                    hoverOffset:     6
                }]
            },
            options: {
                responsive:   true,
                maintainAspectRatio: false,
                cutout:       '65%',
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            color:     textColor,
                            font:      { size: 13 },
                            padding:   16,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
                                const pct   = total ? Math.round(ctx.parsed / total * 100) : 0;
                                return ` ${ctx.label}: ${ctx.parsed} (${pct}%)`;
                            }
                        }
                    }
                }
            }
        });

    } catch (err) {
        console.error('Chyba při načítání dat reportu:', err);
        canvas.parentElement.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="bi bi-exclamation-circle me-2"></i>Chyba při načítání grafu
            </div>`;
    }
}
