CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(350) NOT NULL UNIQUE,
    password_hash VARCHAR(255)
);