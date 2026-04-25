export const DEMO_USERS = {
  student:    { id: 3, name: 'Alice Johnson', email: 'alice@tcu.edu',   role: 'student',    team: 'Team Alpha', section: '2024-2025' },
  instructor: { id: 2, name: 'Dr. Johnson', email: 'johnson@tcu.edu', role: 'instructor', section: '2024-2025' },
  admin:      { id: 3, name: 'Admin User',  email: 'admin@tcu.edu',   role: 'admin' },
}

export const WAR_CATEGORIES = [
  'DEVELOPMENT','TESTING','BUGFIX','COMMUNICATION',
  'DOCUMENTATION','DESIGN','PLANNING','LEARNING',
  'DEPLOYMENT','SUPPORT','MISCELLANEOUS',
]

export const WAR_STATUSES = ['In Progress', 'Under Testing', 'Done']

export const RUBRIC_CRITERIA = [
  { id: 1, name: 'Technical Contribution', description: 'Quality of technical work', maxScore: 20 },
  { id: 2, name: 'Communication',          description: 'Clear, timely communication',  maxScore: 20 },
  { id: 3, name: 'Collaboration',          description: 'Works well with team',          maxScore: 10 },
  { id: 4, name: 'Reliability',            description: 'Meets commitments',             maxScore: 10 },
]

export const TEAMMATES = [
  { id: 4, name: 'Bob Smith' },
  { id: 5, name: 'Carol White' },
  { id: 6, name: 'David Lee' },
  { id: 7, name: 'Eve Martinez' },
]

export const MOCK_WAR = [
  { id: 1, week: 1, category: 'DEVELOPMENT',   description: 'Set up Spring Boot scaffold', plannedHours: 3, actualHours: 4, status: 'Done' },
  { id: 2, week: 1, category: 'PLANNING',      description: 'Sprint 1 planning meeting',    plannedHours: 1, actualHours: 1, status: 'Done' },
  { id: 3, week: 2, category: 'DEVELOPMENT',   description: 'Implement user auth',          plannedHours: 5, actualHours: 6, status: 'In Progress' },
  { id: 4, week: 2, category: 'DOCUMENTATION', description: 'Write API docs',               plannedHours: 2, actualHours: 1, status: 'Under Testing' },
]

export const MOCK_MY_REPORT = {
  week: 2,
  overallGrade: 54,
  totalMax: 60,
  criteria: [
    { name: 'Technical Contribution', score: 17.5, max: 20 },
    { name: 'Communication',          score: 18,   max: 20 },
    { name: 'Collaboration',          score: 9,    max: 10 },
    { name: 'Reliability',            score: 9.5,  max: 10 },
  ],
  publicComments: ['Great work on the auth module!', 'Always available and responsive.'],
}

export const MOCK_SECTION_REPORT = [
  { student: 'Alice Johnson',   submitted: true,  total: 54, max: 60, evaluatedBy: 4 },
  { student: 'Bob Smith',    submitted: true,  total: 49, max: 60, evaluatedBy: 4 },
  { student: 'Carol White',  submitted: false, total: null, max: 60, evaluatedBy: 3 },
  { student: 'David Lee',    submitted: true,  total: 58, max: 60, evaluatedBy: 4 },
  { student: 'Eve Martinez', submitted: true,  total: 52, max: 60, evaluatedBy: 4 },
]

export const MOCK_TEAM_WAR = [
  { student: 'Alice Johnson',   submitted: true,  entries: 4, planned: 11, actual: 12 },
  { student: 'Bob Smith',    submitted: true,  entries: 3, planned:  8, actual:  7 },
  { student: 'Carol White',  submitted: false, entries: 0, planned:  0, actual:  0 },
  { student: 'David Lee',    submitted: true,  entries: 5, planned: 14, actual: 15 },
  { student: 'Eve Martinez', submitted: true,  entries: 4, planned: 10, actual: 11 },
]

export const MOCK_SECTIONS = [
  { id: 1, name: '2024-2025', startDate: '2024-08-19', endDate: '2025-05-10', rubricId: 1 },
  { id: 2, name: '2023-2024', startDate: '2023-08-21', endDate: '2024-05-11', rubricId: 1 },
]

export const MOCK_TEAMS = [
  { id: 1, name: 'Team Alpha', description: 'Healthcare scheduling system',  website: 'https://alpha.tcu.edu', sectionId: 1, memberCount: 5 },
  { id: 2, name: 'Team Beta',  description: 'Inventory management platform', website: 'https://beta.tcu.edu',  sectionId: 1, memberCount: 6 },
  { id: 3, name: 'Team Gamma', description: 'Student mentoring mobile app',  website: 'https://gamma.tcu.edu', sectionId: 1, memberCount: 5 },
]

export const MOCK_RUBRICS = [
  { id: 1, name: 'Standard Peer Evaluation 2024', criteria: RUBRIC_CRITERIA },
]

export const MOCK_INVITATIONS = [
  { id: 1, email: 'david@tcu.edu', role: 'student',    team: 'Team Alpha', sentAt: '2024-08-25', status: 'accepted' },
  { id: 2, email: 'grace@tcu.edu', role: 'student',    team: 'Team Beta',  sentAt: '2024-08-25', status: 'pending' },
  { id: 3, email: 'prof@tcu.edu',  role: 'instructor', team: 'Team Alpha', sentAt: '2024-08-20', status: 'accepted' },
]
