/**
 * SPRINTER – Kanban Board JavaScript
 * ============================================================
 * Implementuje drag & drop na Kanban tabuli pomocí Sortable.js.
 * Změny stavu jsou persistovány přes REST API.
 * ============================================================
 */

'use strict';

(function () {

    /**
     * Inicializace Kanban boardu po načtení DOM.
     */
    document.addEventListener('DOMContentLoaded', () => {
        const board = document.getElementById('kanbanBoard');
        if (!board) return;

        initKanbanBoard(board);
    });

    /**
     * Inicializuje drag & drop na Kanban boardu.
     * @param {HTMLElement} board - container element boardu
     */
    function initKanbanBoard(board) {
        const columns = board.querySelectorAll('.kanban-column-body');

        columns.forEach(column => {
            Sortable.create(column, {
                group: {
                    name: 'kanban',     // společná skupina pro přesun mezi sloupci
                    pull: true,
                    put: true
                },
                animation:     150,
                ghostClass:    'kanban-card-ghost',
                dragClass:     'kanban-card-dragging',
                chosenClass:   'kanban-card-chosen',
                delay:         100,         // malé zpoždění pro prevenci náhodného přesunutí
                delayOnTouchOnly: true,

                /**
                 * Při zahájení přetahování
                 */
                onStart(evt) {
                    document.body.classList.add('is-dragging');
                    evt.item.classList.add('dragging');
                },

                /**
                 * Při ukončení přetahování (ať v původním nebo novém sloupci)
                 */
                onEnd(evt) {
                    document.body.classList.remove('is-dragging');
                    evt.item.classList.remove('dragging');

                    const newColumn = evt.to;
                    const newStatus = newColumn.getAttribute('data-status');
                    const itemId    = evt.item.getAttribute('data-id');
                    const oldStatus = evt.item.getAttribute('data-status');

                    // Pokud se stav nezměnil, neprovádíme API volání
                    if (newStatus === oldStatus) return;

                    // Optimistické UI – okamžitě aktualizujeme atribut
                    evt.item.setAttribute('data-status', newStatus);

                    // Persistování změny na server
                    updateItemStatus(itemId, newStatus)
                        .then(data => {
                            if (data.success) {
                                // Aktualizace počtu v hlavičce sloupce
                                updateColumnCounts(board);
                                window.Sprinter?.showToast(
                                    `Položka ${data.key} přesunuta do: ${getStatusLabel(newStatus)}`,
                                    'success'
                                );
                            } else {
                                // Rollback – vrácení karty zpět
                                revertCard(evt, oldStatus, board);
                                window.Sprinter?.showToast(data.error || 'Chyba při změně stavu', 'error');
                            }
                        })
                        .catch(err => {
                            console.error('Chyba při aktualizaci stavu:', err);
                            revertCard(evt, oldStatus, board);
                            window.Sprinter?.showToast('Chyba připojení k serveru', 'error');
                        });
                },

                /**
                 * Zvýraznění cílového sloupce při přetahování
                 */
                onMove(evt) {
                    evt.related.parentElement.classList.add('drag-over');
                    return true;
                }
            });
        });

        // Počáteční výpočet počtů v sloupcích
        updateColumnCounts(board);
    }

    /**
     * Volá API pro změnu stavu pracovní položky.
     * @param {string} itemId  - ID pracovní položky
     * @param {string} status  - nový stav (enum name)
     * @returns {Promise<object>} - odpověď serveru
     */
    async function updateItemStatus(itemId, status) {
        const resp = await window.Sprinter.patch(
            `/sprinter/api/v1/work-items/${itemId}/status`,
            { status }
        );
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        return resp.json();
    }

    /**
     * Vrátí kartu zpět do původního sloupce po chybě.
     * @param {object} evt       - Sortable onEnd event
     * @param {string} oldStatus - původní stav
     * @param {HTMLElement} board - container boardu
     */
    function revertCard(evt, oldStatus, board) {
        const originalColumn = board.querySelector(
            `.kanban-column-body[data-status="${oldStatus}"]`
        );
        if (originalColumn && evt.item) {
            originalColumn.insertBefore(evt.item, originalColumn.children[evt.oldIndex]);
            evt.item.setAttribute('data-status', oldStatus);
        }
    }

    /**
     * Aktualizuje počty karet v záhlaví sloupců.
     * @param {HTMLElement} board
     */
    function updateColumnCounts(board) {
        board.querySelectorAll('.kanban-column').forEach(col => {
            const status = col.querySelector('.kanban-column-body')?.getAttribute('data-status');
            const count  = col.querySelectorAll('.kanban-card').length;
            const badge  = col.querySelector('.column-count');
            if (badge) badge.textContent = count;
        });
    }

    /**
     * Vrátí přátelský název stavu.
     * @param {string} status - enum name
     * @returns {string}
     */
    function getStatusLabel(status) {
        const labels = {
            TO_DO:       'K řešení',
            IN_PROGRESS: 'Probíhá',
            IN_REVIEW:   'Kontrola',
            DONE:        'Dokončeno',
            CANCELLED:   'Zrušeno'
        };
        return labels[status] || status;
    }

})();
