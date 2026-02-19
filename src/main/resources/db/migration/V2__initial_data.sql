-- =============================================================================
-- SPRINTER – Počáteční data (demo / výchozí stav)
-- Flyway migrace V2
-- Heslo pro admin: Admin123! (BCrypt)
-- =============================================================================

-- Výchozí správce systému
-- Heslo: Admin123! (BCrypt hash síla 12)
INSERT INTO users (username, email, password_hash, first_name, last_name, system_role, active)
VALUES (
    'admin',
    'admin@sprinter.local',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewmjRNHHPWt5eJRa',  -- Admin123!
    'Správce',
    'Systému',
    'ADMIN',
    TRUE
);

-- Globální štítky dostupné pro všechny projekty
INSERT INTO labels (project_id, name, color) VALUES
    (NULL, 'Bug',         '#ef4444'),
    (NULL, 'Feature',     '#22c55e'),
    (NULL, 'Improvement', '#6DA3C7'),
    (NULL, 'Dokumentace', '#a855f7'),
    (NULL, 'Urgent',      '#f97316'),
    (NULL, 'Blocked',     '#6b7280'),
    (NULL, 'Frontend',    '#3b82f6'),
    (NULL, 'Backend',     '#8b5cf6'),
    (NULL, 'Refactoring', '#f59e0b');
