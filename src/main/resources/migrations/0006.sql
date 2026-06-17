CREATE TABLE study_line_memberships (
  user_id INT NOT NULL,
  study_line_id INT NOT NULL,
  role_id INT NOT NULL,

  PRIMARY KEY (user_id, study_line_id, role_id),

  FOREIGN KEY (user_id)
      REFERENCES users(id)
      ON DELETE CASCADE,

  FOREIGN KEY (study_line_id)
      REFERENCES study_lines(id)
      ON DELETE CASCADE,

  FOREIGN KEY (role_id)
      REFERENCES roles(id)
      ON DELETE CASCADE
);

-- Populate from existing tables
INSERT INTO study_line_memberships (user_id, study_line_id, role_id)
SELECT
    user_id,
    study_line_id,
    (SELECT id FROM roles WHERE name = 'student')
FROM study_line_students;

INSERT INTO study_line_memberships (user_id, study_line_id, role_id)
SELECT
    user_id,
    study_line_id,
    (SELECT id FROM roles WHERE name = 'study leader')
FROM study_line_leaders;