INSERT INTO sections (name, start_date, end_date) VALUES ('CS4910', '2024-08-26', '2025-05-10');
INSERT INTO sections (name, start_date, end_date) VALUES ('CS4911', '2025-08-25', '2026-05-09');

INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Alpha', 'A project about data visualization', 'http://teamalpha.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Beta', 'Mobile app for campus navigation', 'http://teambeta.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Gamma', 'AI-powered scheduling tool', null, 'CS4911');

INSERT INTO users (name, email, role, team_id) VALUES ('Dr. Smith',     'smith@tcu.edu',  'INSTRUCTOR', 1);
INSERT INTO users (name, email, role, team_id) VALUES ('Dr. Jones',     'jones@tcu.edu',  'INSTRUCTOR', 2);

INSERT INTO users (name, email, role, team_id) VALUES ('Alice Johnson', 'alice@tcu.edu',  'STUDENT', 1);
INSERT INTO users (name, email, role, team_id) VALUES ('Bob Smith',     'bob@tcu.edu',    'STUDENT', 1);
INSERT INTO users (name, email, role, team_id) VALUES ('Carol White',   'carol@tcu.edu',  'STUDENT', 1);
INSERT INTO users (name, email, role, team_id) VALUES ('Dave Brown',    'dave@tcu.edu',   'STUDENT', 2);
INSERT INTO users (name, email, role, team_id) VALUES ('Eve Davis',     'eve@tcu.edu',    'STUDENT', 2);
INSERT INTO users (name, email, role, team_id) VALUES ('Frank Lee',     'frank@tcu.edu',  'STUDENT', 2);
INSERT INTO users (name, email, role, team_id) VALUES ('Grace Kim',     'grace@tcu.edu',  'STUDENT', 3);
INSERT INTO users (name, email, role, team_id) VALUES ('Hank Jones',    'hank@tcu.edu',   'STUDENT', 3);
