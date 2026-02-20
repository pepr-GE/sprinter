-- Přidání pole progress_pct (procento dokončení) do tabulky work_items
ALTER TABLE work_items ADD COLUMN IF NOT EXISTS progress_pct INTEGER DEFAULT 0 CHECK (progress_pct >= 0 AND progress_pct <= 100);
