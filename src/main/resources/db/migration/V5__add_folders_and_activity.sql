-- =============================================================================
-- SPRINTER – Složky dokumentů + sledování přihlášení
-- Flyway migrace V5
-- =============================================================================

-- ---- Sledování přihlášení (pro activity feed) ----
ALTER TABLE users ADD COLUMN IF NOT EXISTS previous_last_login_at TIMESTAMP;

-- ---- Složky dokumentů ----
CREATE SEQUENCE document_folders_id_seq START 1 INCREMENT 1;

CREATE TABLE document_folders (
    id          BIGINT       NOT NULL DEFAULT nextval('document_folders_id_seq') PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    project_id  BIGINT       REFERENCES projects(id) ON DELETE CASCADE,
    parent_id   BIGINT       REFERENCES document_folders(id) ON DELETE CASCADE,
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_folders_project ON document_folders(project_id);
CREATE INDEX idx_doc_folders_parent  ON document_folders(parent_id);

-- Přidání sloupce folder_id do dokumentů
ALTER TABLE documents ADD COLUMN IF NOT EXISTS folder_id BIGINT
    REFERENCES document_folders(id) ON DELETE SET NULL;

CREATE INDEX idx_documents_folder ON documents(folder_id);
