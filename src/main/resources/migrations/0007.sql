-- Test account with an expired password.
-- Password for the account: password
-- Password last changed on 2026-03-17.

INSERT INTO users (email, name, password_hash, password_changed_at) VALUES
    ('expired@test.dk', 'Expired Password User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg', TIMESTAMP '2026-03-17 00:00:00');

INSERT INTO user_roles (user_id, role_id) VALUES
    ((SELECT id FROM users WHERE email = 'expired@test.dk'), (SELECT id FROM roles WHERE name = 'student'));

INSERT INTO study_line_students (user_id, study_line_id) VALUES
    ((SELECT id FROM users WHERE email = 'expired@test.dk'), (SELECT id FROM study_lines WHERE name = 'Test Study Line'));
