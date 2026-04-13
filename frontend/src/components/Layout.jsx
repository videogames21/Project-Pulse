import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = {
  student: [
    { key: 'war',      label: 'Weekly Activity Report', icon: '📋' },
    { key: 'peereval', label: 'Peer Evaluation',        icon: '⭐' },
    { key: 'myreport', label: 'My Report',              icon: '📊' },
  ],
  instructor: [
    { key: 'section-report', label: 'Section Peer Eval Report', icon: '📊' },
    { key: 'team-war',       label: 'Team WAR Report',          icon: '📋' },
  ],
  admin: [
    { key: 'sections',    label: 'Sections',    icon: '🏛️' },
    { key: 'teams',       label: 'Teams',       icon: '👥' },
    { key: 'rubrics',     label: 'Rubrics',     icon: '📝' },
    { key: 'invitations', label: 'Invitations', icon: '✉️' },
  ],
};

const PAGE_TITLES = {
  war:             'Weekly Activity Report',
  peereval:        'Peer Evaluation',
  myreport:        'My Report',
  'section-report':'Section Peer Evaluation Report',
  'team-war':      'Team WAR Report',
  sections:        'Senior Design Sections',
  teams:           'Senior Design Teams',
  rubrics:         'Evaluation Rubrics',
  invitations:     'Invite Users',
};

const ROLE_COLORS = {
  student:    { bg: '#2e7d32', text: 'Student' },
  instructor: { bg: '#1565c0', text: 'Instructor' },
  admin:      { bg: '#6d1979', text: 'Admin' },
};

export default function Layout({ currentPage, onNavigate, children }) {
  const { user, logout } = useAuth();
  const navItems = NAV_ITEMS[user.role] ?? [];
  const rc = ROLE_COLORS[user.role];

  return (
    <div className="app-shell">
      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h2>Project Pulse</h2>
          <p>TCU Senior Design</p>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section-label">Navigation</div>
          {navItems.map((item) => (
            <div
              key={item.key}
              className={`nav-item${currentPage === item.key ? ' active' : ''}`}
              onClick={() => onNavigate(item.key)}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <span className="user-name">{user.name}</span>
            <span style={{
              display: 'inline-block',
              marginTop: '4px',
              padding: '2px 8px',
              background: rc.bg,
              color: '#fff',
              borderRadius: '999px',
              fontSize: '0.72rem',
              fontWeight: '600',
              width: 'fit-content',
            }}>
              {rc.text}
            </span>
          </div>
          <button
            className="btn btn-secondary btn-sm"
            style={{ width: '100%', justifyContent: 'center', background: 'rgba(255,255,255,0.12)', color: '#fff', border: '1px solid rgba(255,255,255,0.2)' }}
            onClick={logout}
          >
            Sign Out
          </button>
        </div>
      </aside>

      {/* ── Main area ── */}
      <div className="main-content">
        <header className="topbar">
          <span className="topbar-title">{PAGE_TITLES[currentPage]}</span>
          <span className="topbar-meta">
            {user.team && `${user.team} · `}{user.section ?? ''}
          </span>
        </header>
        <main className="page-content">
          {children}
        </main>
      </div>
    </div>
  );
}
