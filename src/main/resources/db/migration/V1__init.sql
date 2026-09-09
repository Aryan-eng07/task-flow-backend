-- TaskFlow initial schema (plan section 1).
-- InnoDB + utf8mb4; DATETIME(6) everywhere (no 2038 ceiling, no implicit TZ conversion).
-- All times stored in UTC.

CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(160) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    full_name            VARCHAR(120) NOT NULL,
    role                 VARCHAR(10)  NOT NULL DEFAULT 'MEMBER',
    wip_limit            INT          NOT NULL DEFAULT 5,
    active               BIT(1)       NOT NULL DEFAULT b'1',
    avg_resolution_hours DECIMAL(8,2) NULL,
    created_at           DATETIME(6)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE skills (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_skills_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_skills (
    user_id     BIGINT  NOT NULL,
    skill_id    BIGINT  NOT NULL,
    proficiency INT     NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    CONSTRAINT fk_user_skills_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE projects (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    key_code      VARCHAR(10)  NOT NULL,
    name          VARCHAR(120) NOT NULL,
    description   TEXT         NULL,
    lead_id       BIGINT       NULL,
    issue_counter INT          NOT NULL DEFAULT 0,
    active        BIT(1)       NOT NULL DEFAULT b'1',
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_projects_key_code UNIQUE (key_code),
    CONSTRAINT fk_projects_lead FOREIGN KEY (lead_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE project_members (
    project_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sprints (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    project_id BIGINT       NOT NULL,
    name       VARCHAR(120) NULL,
    goal       VARCHAR(255) NULL,
    status     VARCHAR(12)  NOT NULL DEFAULT 'PLANNED',
    start_date DATE         NULL,
    end_date   DATE         NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE issues (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    issue_key       VARCHAR(20)  NOT NULL,
    project_id      BIGINT       NOT NULL,
    sprint_id       BIGINT       NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NULL,
    type            VARCHAR(10)  NOT NULL DEFAULT 'TASK',
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(12)  NOT NULL DEFAULT 'TODO',
    reporter_id     BIGINT       NOT NULL,
    assignee_id     BIGINT       NULL,
    assignment_mode VARCHAR(12)  NOT NULL DEFAULT 'UNASSIGNED',
    estimate_hours  DECIMAL(6,2) NULL,
    board_order     INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    resolved_at     DATETIME(6)  NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_issues_issue_key UNIQUE (issue_key),
    CONSTRAINT fk_issues_project  FOREIGN KEY (project_id)  REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_sprint   FOREIGN KEY (sprint_id)   REFERENCES sprints (id)  ON DELETE SET NULL,
    CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_issue_board    ON issues (project_id, status, board_order);
CREATE INDEX idx_issue_assignee ON issues (assignee_id, status);
CREATE INDEX idx_issue_sprint   ON issues (sprint_id, status);

CREATE TABLE issue_required_skills (
    issue_id BIGINT   NOT NULL,
    skill_id BIGINT   NOT NULL,
    weight   INT      NOT NULL DEFAULT 1,
    PRIMARY KEY (issue_id, skill_id),
    CONSTRAINT fk_irs_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_irs_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    issue_id   BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    body       TEXT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_issue  FOREIGN KEY (issue_id)  REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_comment_issue ON comments (issue_id, created_at);

CREATE TABLE assignment_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    issue_id     BIGINT       NOT NULL,
    candidate_id BIGINT       NOT NULL,
    skill_score  DECIMAL(5,4) NULL,
    load_score   DECIMAL(5,4) NULL,
    speed_score  DECIMAL(5,4) NULL,
    total_score  DECIMAL(5,4) NULL,
    selected     BIT(1)       NOT NULL DEFAULT b'0',
    reason       VARCHAR(255) NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assignment_log_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_assignment_log_issue ON assignment_log (issue_id);

CREATE TABLE activity_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    issue_id   BIGINT      NOT NULL,
    actor_id   BIGINT      NOT NULL,
    field      VARCHAR(60) NULL,
    old_value  TEXT        NULL,
    new_value  TEXT        NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_log_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
