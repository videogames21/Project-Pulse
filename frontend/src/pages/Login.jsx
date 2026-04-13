import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

const ROLES = [
  {
    key: 'student',
    label: 'Student',
    icon: '🎓',
    desc: 'Submit WARs, complete peer evaluations, view your report',
  },
  {
    key: 'instructor',
    label: 'Instructor',
    icon: '👩‍🏫',
    desc: 'View team WAR reports and peer evaluation summaries',
  },
  {
    key: 'admin',
    label: 'Admin',
    icon: '⚙️',
    desc: 'Manage sections, teams, rubrics, and user invitations',
  },
];

export default function Login() {
  const { login } = useAuth();
  const [selected, setSelected] = useState(null);

  return (
    <div style={styles.page}>
      <div style={styles.panel}>
        {/* Header */}
        <div style={styles.header}>
          <div style={styles.logoMark}>P</div>
          <h1 style={styles.title}>Project Pulse</h1>
          <p style={styles.subtitle}>
            TCU Senior Design — Student Performance Tracking
          </p>
        </div>

        {/* Demo role picker */}
        <p style={styles.demoNote}>
          Demo mode — select a role to explore the prototype
        </p>

        <div style={styles.roleGrid}>
          {ROLES.map((r) => (
            <button
              key={r.key}
              style={{
                ...styles.roleCard,
                ...(selected === r.key ? styles.roleCardActive : {}),
              }}
              onClick={() => setSelected(r.key)}
            >
              <span style={styles.roleIcon}>{r.icon}</span>
              <strong style={styles.roleLabel}>{r.label}</strong>
              <span style={styles.roleDesc}>{r.desc}</span>
            </button>
          ))}
        </div>

        <button
          className="btn btn-primary w-full"
          style={{ justifyContent: 'center', padding: '12px', fontSize: '1rem', marginTop: '8px' }}
          disabled={!selected}
          onClick={() => login(selected)}
        >
          Enter as {selected ? ROLES.find((r) => r.key === selected)?.label : '…'}
        </button>

        <p style={styles.footer}>
          Texas Christian University · Department of Computer Science
        </p>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #3a1159 0%, #4D1979 60%, #6b2ba0 100%)',
    padding: '20px',
  },
  panel: {
    background: '#fff',
    borderRadius: '16px',
    padding: '40px',
    width: '100%',
    maxWidth: '520px',
    boxShadow: '0 20px 60px rgba(0,0,0,0.25)',
  },
  header: {
    textAlign: 'center',
    marginBottom: '28px',
  },
  logoMark: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '56px',
    height: '56px',
    background: '#4D1979',
    color: '#fff',
    borderRadius: '14px',
    fontSize: '1.6rem',
    fontWeight: '800',
    marginBottom: '12px',
  },
  title: {
    fontSize: '1.8rem',
    fontWeight: '800',
    color: '#4D1979',
    margin: 0,
  },
  subtitle: {
    color: '#6b6480',
    fontSize: '0.875rem',
    marginTop: '4px',
  },
  demoNote: {
    textAlign: 'center',
    fontSize: '0.78rem',
    color: '#6b6480',
    background: '#f5f4f8',
    borderRadius: '6px',
    padding: '6px 12px',
    marginBottom: '16px',
    border: '1px solid #e0dbe8',
  },
  roleGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr 1fr',
    gap: '10px',
    marginBottom: '20px',
  },
  roleCard: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
    padding: '16px 10px',
    border: '2px solid #e0dbe8',
    borderRadius: '10px',
    background: '#fff',
    cursor: 'pointer',
    transition: 'all 0.15s',
    textAlign: 'center',
  },
  roleCardActive: {
    borderColor: '#4D1979',
    background: '#ede7f6',
  },
  roleIcon: {
    fontSize: '1.8rem',
  },
  roleLabel: {
    fontSize: '0.875rem',
    color: '#1a1a2e',
  },
  roleDesc: {
    fontSize: '0.72rem',
    color: '#6b6480',
    lineHeight: '1.35',
  },
  footer: {
    textAlign: 'center',
    fontSize: '0.75rem',
    color: '#9e9aaa',
    marginTop: '20px',
  },
};
