INSERT INTO sections (name, start_date, end_date, instructor_id) VALUES ('CS4910', '2024-08-26', '2025-05-10', 2);
INSERT INTO sections (name, start_date, end_date, instructor_id) VALUES ('CS4911', '2025-08-25', '2026-05-09', null);

INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Alpha', 'A project about data visualization', 'http://teamalpha.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Beta', 'Mobile app for campus navigation', 'http://teambeta.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Gamma', 'AI-powered scheduling tool', null, 'CS4911');

-- BCrypt hash for "password" (10 rounds) — dev placeholder only, never use in production
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Admin',  'User',    'admin@tcu.edu',    'ADMIN',      'ACTIVE', null, '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Dr.',    'Johnson', 'johnson@tcu.edu',  'INSTRUCTOR', 'ACTIVE', null, '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Dr.',    'Smith',   'smith@tcu.edu',    'INSTRUCTOR', 'ACTIVE', null, '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Dr.',    'Jones',   'jones@tcu.edu',    'INSTRUCTOR', 'ACTIVE', null, '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Alice',  'Chen',    'alice@tcu.edu',    'STUDENT',    'ACTIVE', 1,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Bob',    'Smith',   'bob@tcu.edu',      'STUDENT',    'ACTIVE', 1,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Carol',  'White',   'carol@tcu.edu',    'STUDENT',    'ACTIVE', 1,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Dave',   'Brown',   'dave@tcu.edu',     'STUDENT',    'ACTIVE', 2,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Eve',    'Davis',   'eve@tcu.edu',      'STUDENT',    'ACTIVE', 2,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Frank',  'Lee',     'frank@tcu.edu',    'STUDENT',    'ACTIVE', 2,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Grace',  'Kim',     'grace@tcu.edu',    'STUDENT',    'ACTIVE', 3,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
INSERT INTO users (first_name, last_name, email, role, status, team_id, password, enabled) VALUES ('Hank',   'Jones',   'hank@tcu.edu',     'STUDENT',    'ACTIVE', 3,    '$2a$10$/rz/mTHR6tfoYIglSdFyDe7pq1tHpDFf5Wzi1jP9Qjf7km.zMynh2', true);
