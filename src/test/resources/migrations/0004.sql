-- Temporary test accounts for local development and integration testing only.
-- Password for all accounts: password

INSERT INTO users (email, name, password_hash) VALUES
    ('admin@test.dk', 'Admin User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg'),
    ('teacher@test.dk', 'Teacher User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg'),
    ('student@test.dk', 'Student User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg'),
    ('advisor@test.dk', 'Advisor User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg'),
    ('studyleader@test.dk', 'Study Leader User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg'),
    ('allroles@test.dk', 'All Roles User', '$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg');

INSERT INTO user_roles (user_id, role_id) VALUES
    ((SELECT id FROM users WHERE email = 'admin@test.dk'), (SELECT id FROM roles WHERE name = 'administrator')),
    ((SELECT id FROM users WHERE email = 'teacher@test.dk'), (SELECT id FROM roles WHERE name = 'teacher')),
    ((SELECT id FROM users WHERE email = 'student@test.dk'), (SELECT id FROM roles WHERE name = 'student')),
    ((SELECT id FROM users WHERE email = 'advisor@test.dk'), (SELECT id FROM roles WHERE name = 'advisor')),
    ((SELECT id FROM users WHERE email = 'studyleader@test.dk'), (SELECT id FROM roles WHERE name = 'study leader')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM roles WHERE name = 'administrator')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM roles WHERE name = 'advisor')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM roles WHERE name = 'teacher')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM roles WHERE name = 'study leader')),
    ((SELECT id FROM users WHERE email = 'allroles@test.dk'), (SELECT id FROM roles WHERE name = 'student'));