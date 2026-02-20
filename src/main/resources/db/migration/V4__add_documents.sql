-- =============================================================================
-- SPRINTER – Dokumenty
-- Flyway migrace V4
-- =============================================================================

CREATE SEQUENCE documents_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE document_comments_id_seq  START 1 INCREMENT 1;
CREATE SEQUENCE document_links_id_seq     START 1 INCREMENT 1;

-- Dokumenty (nahrazují ARTICLE typ pracovních položek)
CREATE TABLE documents (
    id          BIGINT          NOT NULL DEFAULT nextval('documents_id_seq') PRIMARY KEY,
    title       VARCHAR(500)    NOT NULL,
    content     TEXT,
    project_id  BIGINT          REFERENCES projects(id) ON DELETE CASCADE,
    author_id   BIGINT          NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_documents_project ON documents(project_id);
CREATE INDEX idx_documents_author  ON documents(author_id);

-- Komentáře k dokumentům
CREATE TABLE document_comments (
    id          BIGINT          NOT NULL DEFAULT nextval('document_comments_id_seq') PRIMARY KEY,
    content     TEXT            NOT NULL,
    document_id BIGINT          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    author_id   BIGINT          NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_doc_comments_document ON document_comments(document_id);

-- Vazby dokumentů na pracovní položky
CREATE TABLE document_work_item_links (
    document_id  BIGINT  NOT NULL REFERENCES documents(id)   ON DELETE CASCADE,
    work_item_id BIGINT  NOT NULL REFERENCES work_items(id)  ON DELETE CASCADE,
    PRIMARY KEY (document_id, work_item_id)
);
