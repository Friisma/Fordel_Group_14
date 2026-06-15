-- Study Lines
CREATE TABLE study_lines (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

--  User study line relationships
-- Students assigned to study lines
CREATE TABLE study_line_students (
    user_id INT NOT NULL,
    study_line_id INT NOT NULL,

    PRIMARY KEY (user_id, study_line_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (study_line_id) REFERENCES study_lines(id) ON DELETE CASCADE
);

-- Study leaders assigned to study lines
CREATE TABLE study_line_leaders (
    user_id INT NOT NULL,
    study_line_id INT NOT NULL,

    PRIMARY KEY (user_id, study_line_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (study_line_id) REFERENCES study_lines(id) ON DELETE CASCADE
);

-- Extend Users
ALTER TABLE users
    ADD COLUMN study_start_date DATE;

ALTER TABLE users
    ADD COLUMN max_workload INT DEFAULT 5;

ALTER TABLE users
    ADD COLUMN password_changed_at TIMESTAMP;

-- Projects
-- (Every project belongs to exactly one study line)
CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),

    study_line_id INT,

    FOREIGN KEY (study_line_id)
        REFERENCES study_lines(id)
        ON DELETE CASCADE
);

-- Project Students (Authors)
CREATE TABLE project_students (
    user_id INT NOT NULL,
    project_id INT NOT NULL,

    PRIMARY KEY (user_id, project_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);


-- Advisor Invitations
CREATE TABLE advisor_invitations (
    id SERIAL PRIMARY KEY,

    advisor_id INT NOT NULL,
    project_id INT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'pending',

    created_at TIMESTAMP DEFAULT NOW(),

    FOREIGN KEY (advisor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Constraint to prevent duplicate invitations
CREATE UNIQUE INDEX unique_advisor_project_invitation
    ON advisor_invitations(advisor_id, project_id);

-- Active Advisor Invitations
-- (only accepted invitations become active advisors)
CREATE TABLE project_advisors (
    user_id INT NOT NULL,
    project_id INT NOT NULL,

    PRIMARY KEY (user_id, project_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);