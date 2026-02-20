-- =============================================================================
-- SPRINTER – Oblíbené položky (favorites)
-- Flyway migrace V6
-- =============================================================================

CREATE SEQUENCE user_favorites_id_seq START 1 INCREMENT 1;

CREATE TABLE user_favorites (
    id          BIGINT       NOT NULL DEFAULT nextval('user_favorites_id_seq') PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(50)  NOT NULL,   -- 'project', 'document', 'work_item', 'folder'
    entity_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    icon        VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_favorite UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX idx_user_favorites_user ON user_favorites(user_id);
