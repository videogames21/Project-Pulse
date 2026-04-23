INSERT INTO sections (name, start_date, end_date) VALUES ('CS4910', '2024-08-26', '2025-05-10');
INSERT INTO sections (name, start_date, end_date) VALUES ('CS4911', '2025-08-25', '2026-05-09');

INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Alpha', 'A project about data visualization', 'http://teamalpha.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Beta', 'Mobile app for campus navigation', 'http://teambeta.com', 'CS4910');
INSERT INTO teams (name, description, website_url, section_name) VALUES ('Team Gamma', 'AI-powered scheduling tool', null, 'CS4911');

-- Users (students and one instructor)
INSERT INTO users (first_name, last_name, email, role) VALUES ('Alice', 'Johnson', 'alice@tcu.edu', 'STUDENT');
INSERT INTO users (first_name, last_name, email, role) VALUES ('Bob', 'Smith', 'bob@tcu.edu', 'STUDENT');
INSERT INTO users (first_name, last_name, email, role) VALUES ('Carol', 'Williams', 'carol@tcu.edu', 'STUDENT');
INSERT INTO users (first_name, last_name, email, role) VALUES ('David', 'Brown', 'david@tcu.edu', 'STUDENT');
INSERT INTO users (first_name, last_name, email, role) VALUES ('Eve', 'Davis', 'eve@tcu.edu', 'STUDENT');
INSERT INTO users (first_name, last_name, email, role) VALUES ('Frank', 'Miller', 'frank@tcu.edu', 'INSTRUCTOR');

-- Team memberships (users 1-4 on Team Alpha, user 5 on Team Beta)
INSERT INTO team_members (team_id, user_id) VALUES (1, 1);
INSERT INTO team_members (team_id, user_id) VALUES (1, 2);
INSERT INTO team_members (team_id, user_id) VALUES (1, 3);
INSERT INTO team_members (team_id, user_id) VALUES (1, 4);
INSERT INTO team_members (team_id, user_id) VALUES (2, 5);

-- Active rubric
INSERT INTO rubrics (name, active) VALUES ('Standard Peer Evaluation Rubric', TRUE);

-- Criteria for the active rubric (rubric_id = 1)
INSERT INTO criteria (name, description, max_score, rubric_id) VALUES ('Contribution', 'Quality and quantity of work contributed to the project', 10, 1);
INSERT INTO criteria (name, description, max_score, rubric_id) VALUES ('Communication', 'Effectiveness of communication with team members', 10, 1);
INSERT INTO criteria (name, description, max_score, rubric_id) VALUES ('Collaboration', 'Willingness to help and work with others', 10, 1);
INSERT INTO criteria (name, description, max_score, rubric_id) VALUES ('Reliability', 'Consistently meets deadlines and commitments', 10, 1);
INSERT INTO criteria (name, description, max_score, rubric_id) VALUES ('Technical Skills', 'Technical expertise demonstrated during the project', 10, 1);
