// ── Mock data for Project Pulse prototype ──────────────────────────────────
// Replace with real API calls once the Spring Boot backend is built.

export const DEMO_USERS = {
  student:    { id: 1, name: 'Alice Chen',     email: 'alice@tcu.edu',    role: 'student',    team: 'Team Alpha', section: '2024-2025' },
  instructor: { id: 2, name: 'Dr. Johnson',    email: 'johnson@tcu.edu',  role: 'instructor', section: '2024-2025' },
  admin:      { id: 3, name: 'Admin User',     email: 'admin@tcu.edu',    role: 'admin' },
};

export const TEAMMATES = [
  { id: 4, name: 'Bob Smith' },
  { id: 5, name: 'Carol White' },
  { id: 6, name: 'David Lee' },
  { id: 7, name: 'Eve Martinez' },
  // self — added dynamically
];

export const WAR_CATEGORIES = [
  'DEVELOPMENT', 'TESTING', 'BUGFIX', 'COMMUNICATION',
  'DOCUMENTATION', 'DESIGN', 'PLANNING', 'LEARNING',
  'DEPLOYMENT', 'SUPPORT', 'MISCELLANEOUS',
];

export const WAR_STATUSES = ['In Progress', 'Under Testing', 'Done'];

export const RUBRIC_CRITERIA = [
  { id: 1, name: 'Technical Contribution', description: 'Quality and quantity of technical work delivered', maxScore: 20 },
  { id: 2, name: 'Communication',          description: 'Clear, timely, and professional communication', maxScore: 20 },
  { id: 3, name: 'Collaboration',          description: 'Works effectively with team members',           maxScore: 10 },
  { id: 4, name: 'Reliability',            description: 'Meets commitments, attends meetings',           maxScore: 10 },
];

export const INITIAL_WAR_ENTRIES = [
  { id: 1, week: 1, category: 'DEVELOPMENT',   description: 'Set up backend Spring Boot scaffold', plannedHours: 3, actualHours: 4, status: 'Done' },
  { id: 2, week: 1, category: 'PLANNING',      description: 'Sprint 1 planning meeting',           plannedHours: 1, actualHours: 1, status: 'Done' },
  { id: 3, week: 2, category: 'DEVELOPMENT',   description: 'Implement user authentication',       plannedHours: 5, actualHours: 6, status: 'In Progress' },
  { id: 4, week: 2, category: 'DOCUMENTATION', description: 'Write API documentation',             plannedHours: 2, actualHours: 1, status: 'Under Testing' },
];

export const INITIAL_SECTIONS = [
  { id: 1, name: '2024-2025', startDate: '2024-08-19', endDate: '2025-05-10', rubricId: 1, activeWeeks: [5,6,7,8,9,10,11,12,13,14,15] },
  { id: 2, name: '2023-2024', startDate: '2023-08-21', endDate: '2024-05-11', rubricId: 1, activeWeeks: [] },
];

export const INITIAL_TEAMS = [
  { id: 1, name: 'Team Alpha',   description: 'Healthcare scheduling system',     website: 'https://alpha.tcu.edu',   sectionId: 1, memberCount: 5 },
  { id: 2, name: 'Team Beta',    description: 'Inventory management platform',    website: 'https://beta.tcu.edu',    sectionId: 1, memberCount: 6 },
  { id: 3, name: 'Team Gamma',   description: 'Student mentoring mobile app',     website: 'https://gamma.tcu.edu',   sectionId: 1, memberCount: 5 },
];

export const INITIAL_RUBRICS = [
  {
    id: 1,
    name: 'Standard Peer Evaluation 2024',
    criteria: RUBRIC_CRITERIA,
    totalMax: RUBRIC_CRITERIA.reduce((s, c) => s + c.maxScore, 0),
  }
];

// Mock peer eval scores for "My Report" tab
export const MY_REPORT = {
  week: 2,
  criteria: [
    { name: 'Technical Contribution', myScore: 17.5, maxScore: 20 },
    { name: 'Communication',          myScore: 18,   maxScore: 20 },
    { name: 'Collaboration',          myScore:  9,   maxScore: 10 },
    { name: 'Reliability',            myScore:  9.5, maxScore: 10 },
  ],
  publicComments: [
    'Great work on the auth module!',
    'Always available and responsive.',
  ],
  overallGrade: 54,
  totalMax: 60,
};

// Mock section-wide peer eval data for instructor view
export const SECTION_REPORT = [
  { student: 'Alice Chen',    submitted: true,  total: 54, max: 60, evaluatedBy: 4 },
  { student: 'Bob Smith',     submitted: true,  total: 49, max: 60, evaluatedBy: 4 },
  { student: 'Carol White',   submitted: false, total: null, max: 60, evaluatedBy: 3 },
  { student: 'David Lee',     submitted: true,  total: 58, max: 60, evaluatedBy: 4 },
  { student: 'Eve Martinez',  submitted: true,  total: 52, max: 60, evaluatedBy: 4 },
];

// Mock team WAR data for instructor view
export const TEAM_WAR_REPORT = [
  { student: 'Alice Chen',   week: 2, entries: 4, totalPlanned: 11, totalActual: 12, submitted: true },
  { student: 'Bob Smith',    week: 2, entries: 3, totalPlanned:  8, totalActual:  7, submitted: true },
  { student: 'Carol White',  week: 2, entries: 0, totalPlanned:  0, totalActual:  0, submitted: false },
  { student: 'David Lee',    week: 2, entries: 5, totalPlanned: 14, totalActual: 15, submitted: true },
  { student: 'Eve Martinez', week: 2, entries: 4, totalPlanned: 10, totalActual: 11, submitted: true },
];
