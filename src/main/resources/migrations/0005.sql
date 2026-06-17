INSERT INTO study_lines (name) VALUES
    ('Test Study Line');

INSERT INTO study_line_students (user_id, study_line_id) VALUES
    ((SELECT id FROM users WHERE email = 'admin@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'teacher@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'student@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'advisor@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'studyleader@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line'));

INSERT INTO study_line_leaders (user_id, study_line_id) VALUES
    ((SELECT id FROM users WHERE email = 'studyleader@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line'));
