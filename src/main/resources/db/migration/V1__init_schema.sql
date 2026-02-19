-- =============================================================================
-- SPRINTER – Inicializace databázového schématu
-- Flyway migrace V1
-- =============================================================================

-- Rozšíření pro UUID (používá se pro generování souborových názvů na úrovni DB)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- pro fulltext vyhledávání

-- =============================================================================
-- SEKVENCE
-- =============================================================================

CREATE SEQUENCE users_id_seq               START 1 INCREMENT 1;
CREATE SEQUENCE projects_id_seq            START 1 INCREMENT 1;
CREATE SEQUENCE project_members_id_seq     START 1 INCREMENT 1;
CREATE SEQUENCE sprints_id_seq             START 1 INCREMENT 1;
CREATE SEQUENCE work_items_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE comments_id_seq            START 1 INCREMENT 1;
CREATE SEQUENCE attachments_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE labels_id_seq              START 1 INCREMENT 1;
CREATE SEQUENCE work_item_deps_id_seq      START 1 INCREMENT 1;

-- =============================================================================
-- TABULKY
-- =============================================================================

-- Uživatelé
CREATE TABLE users (
    id              BIGINT          NOT NULL DEFAULT nextval('users_id_seq') PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(80)     NOT NULL,
    last_name       VARCHAR(80)     NOT NULL,
    avatar_path     VARCHAR(500),
    system_role     VARCHAR(20)     NOT NULL DEFAULT 'USER',
    ui_theme        VARCHAR(10)     NOT NULL DEFAULT 'light',
    locale          VARCHAR(5)      NOT NULL DEFAULT 'cs',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT ck_users_role     CHECK (system_role IN ('ADMIN', 'USER'))
);

-- Remember-me tokeny pro Spring Security (persistent token strategy)
CREATE TABLE persistent_logins (
    username    VARCHAR(64)     NOT NULL,
    series      VARCHAR(64)     NOT NULL PRIMARY KEY,
    token       VARCHAR(64)     NOT NULL,
    last_used   TIMESTAMP       NOT NULL
);

-- Projekty a podprojekty (self-referencing hierarchie)
CREATE TABLE projects (
    id          BIGINT          NOT NULL DEFAULT nextval('projects_id_seq') PRIMARY KEY,
    name        VARCHAR(200)    NOT NULL,
    project_key VARCHAR(10)     NOT NULL,
    description TEXT,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    start_date  DATE,
    end_date    DATE,
    parent_id   BIGINT          REFERENCES projects(id) ON DELETE SET NULL,
    owner_id    BIGINT          NOT NULL REFERENCES users(id),
    item_counter BIGINT         NOT NULL DEFAULT 0,
    icon_url    VARCHAR(500),
    color       VARCHAR(20)     DEFAULT '#6DA3C7',
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,

    CONSTRAINT uq_projects_key UNIQUE (project_key),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE','ON_HOLD','COMPLETED','ARCHIVED'))
);

-- Členové projektových týmů
CREATE TABLE project_members (
    id           BIGINT      NOT NULL DEFAULT nextval('project_members_id_seq') PRIMARY KEY,
    project_id   BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    project_role VARCHAR(20) NOT NULL DEFAULT 'TEAM_MEMBER',
    joined_at    TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_project_members     UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_member_role CHECK  (project_role IN ('MANAGER','TEAM_MEMBER','OBSERVER'))
);

-- Sprinty
CREATE TABLE sprints (
    id           BIGINT       NOT NULL DEFAULT nextval('sprints_id_seq') PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name         VARCHAR(200) NOT NULL,
    goal         TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    start_date   DATE,
    end_date     DATE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT ck_sprints_status CHECK (status IN ('PLANNING','ACTIVE','COMPLETED','CANCELLED'))
);

-- Pracovní položky (úkoly, problémy, stories, epicy, články)
CREATE TABLE work_items (
    id              BIGINT       NOT NULL DEFAULT nextval('work_items_id_seq') PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES projects(id)  ON DELETE CASCADE,
    sprint_id       BIGINT       REFERENCES sprints(id) ON DELETE SET NULL,
    parent_id       BIGINT       REFERENCES work_items(id) ON DELETE SET NULL,
    assignee_id     BIGINT       REFERENCES users(id)     ON DELETE SET NULL,
    reporter_id     BIGINT       NOT NULL REFERENCES users(id),
    item_number     BIGINT       NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'TO_DO',
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    story_points    INTEGER,
    start_date      DATE,
    due_date        DATE,
    completed_at    TIMESTAMP,
    estimated_hours DOUBLE PRECISION,
    logged_hours    DOUBLE PRECISION,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    CONSTRAINT ck_work_items_type   CHECK (type   IN ('TASK','ISSUE','STORY','EPIC','ARTICLE')),
    CONSTRAINT ck_work_items_status CHECK (status IN ('TO_DO','IN_PROGRESS','IN_REVIEW','DONE','CANCELLED')),
    CONSTRAINT ck_work_items_prio   CHECK (priority IN ('LOWEST','LOW','MEDIUM','HIGH','HIGHEST','CRITICAL'))
);

-- Komentáře
CREATE TABLE comments (
    id           BIGINT    NOT NULL DEFAULT nextval('comments_id_seq') PRIMARY KEY,
    work_item_id BIGINT    NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    author_id    BIGINT    NOT NULL REFERENCES users(id),
    content      TEXT      NOT NULL,
    is_edited    BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP
);

-- Přílohy
CREATE TABLE attachments (
    id                BIGINT       NOT NULL DEFAULT nextval('attachments_id_seq') PRIMARY KEY,
    work_item_id      BIGINT       NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    uploaded_by_id    BIGINT       NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100),
    file_size         BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Štítky
CREATE TABLE labels (
    id         BIGINT      NOT NULL DEFAULT nextval('labels_id_seq') PRIMARY KEY,
    project_id BIGINT      REFERENCES projects(id) ON DELETE CASCADE,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(10) NOT NULL DEFAULT '#6DA3C7',

    CONSTRAINT uq_labels_project_name UNIQUE (project_id, name)
);

-- Přiřazení štítků k pracovním položkám (M:N)
CREATE TABLE work_item_labels (
    work_item_id BIGINT NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    label_id     BIGINT NOT NULL REFERENCES labels(id)     ON DELETE CASCADE,
    PRIMARY KEY (work_item_id, label_id)
);

-- Závislosti mezi pracovními položkami (pro Gantt)
CREATE TABLE work_item_dependencies (
    id              BIGINT      NOT NULL DEFAULT nextval('work_item_deps_id_seq') PRIMARY KEY,
    predecessor_id  BIGINT      NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    successor_id    BIGINT      NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START',
    lag_days        INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_work_item_dep  UNIQUE (predecessor_id, successor_id),
    CONSTRAINT ck_dep_type       CHECK  (dependency_type IN ('FINISH_TO_START','START_TO_START',
                                         'FINISH_TO_FINISH','START_TO_FINISH','BLOCKS')),
    CONSTRAINT no_self_dep       CHECK  (predecessor_id != successor_id)
);

-- =============================================================================
-- INDEXY pro výkon
-- =============================================================================

CREATE INDEX idx_projects_parent          ON projects(parent_id);
CREATE INDEX idx_projects_owner           ON projects(owner_id);
CREATE INDEX idx_projects_status          ON projects(status);

CREATE INDEX idx_project_members_project  ON project_members(project_id);
CREATE INDEX idx_project_members_user     ON project_members(user_id);

CREATE INDEX idx_sprints_project          ON sprints(project_id);
CREATE INDEX idx_sprints_status           ON sprints(status);

CREATE INDEX idx_work_items_project       ON work_items(project_id);
CREATE INDEX idx_work_items_sprint        ON work_items(sprint_id);
CREATE INDEX idx_work_items_assignee      ON work_items(assignee_id);
CREATE INDEX idx_work_items_reporter      ON work_items(reporter_id);
CREATE INDEX idx_work_items_status        ON work_items(status);
CREATE INDEX idx_work_items_type          ON work_items(type);
CREATE INDEX idx_work_items_parent        ON work_items(parent_id);
CREATE INDEX idx_work_items_item_num      ON work_items(project_id, item_number);
CREATE INDEX idx_work_items_due_date      ON work_items(due_date) WHERE due_date IS NOT NULL;

-- Trigram index pro fulltext vyhledávání v názvech
CREATE INDEX idx_work_items_title_trgm    ON work_items USING GIN (title gin_trgm_ops);
CREATE INDEX idx_projects_name_trgm       ON projects    USING GIN (name gin_trgm_ops);

CREATE INDEX idx_comments_work_item       ON comments(work_item_id);
CREATE INDEX idx_attachments_work_item    ON attachments(work_item_id);

-- =============================================================================
-- FUNKCE A TRIGGERY
-- =============================================================================

-- Automatická aktualizace updated_at při každém UPDATE
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER tr_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER tr_sprints_updated_at
    BEFORE UPDATE ON sprints
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER tr_work_items_updated_at
    BEFORE UPDATE ON work_items
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER tr_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
