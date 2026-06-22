CREATE TABLE student_invitations (
    id SERIAL PRIMARY KEY,

    student_id INT NOT NULL,
    project_id INT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'pending',

    created_at TIMESTAMP DEFAULT NOW(),

    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX unique_student_project_invitation
    ON student_invitations(student_id, project_id);