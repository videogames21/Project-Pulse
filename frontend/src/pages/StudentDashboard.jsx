import { useState } from 'react';
import {
  INITIAL_WAR_ENTRIES,
  WAR_CATEGORIES,
  WAR_STATUSES,
  TEAMMATES,
  RUBRIC_CRITERIA,
  MY_REPORT,
} from '../data/mockData';
import { useAuth } from '../context/AuthContext';

const STATUS_BADGE = {
  'Done':          'badge-green',
  'In Progress':   'badge-orange',
  'Under Testing': 'badge-blue',
};

// ── Helpers ────────────────────────────────────────────────────────────────

function Modal({ title, onClose, onSubmit, submitLabel = 'Save', children }) {
  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">{children}</div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={onSubmit}>{submitLabel}</button>
        </div>
      </div>
    </div>
  );
}

// ── WAR Tab ────────────────────────────────────────────────────────────────

function WARTab() {
  const [entries, setEntries] = useState(INITIAL_WAR_ENTRIES);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyEntry());
  const [filterWeek, setFilterWeek] = useState('all');
  const [success, setSuccess] = useState('');

  function emptyEntry() {
    return { week: 1, category: 'DEVELOPMENT', description: '', plannedHours: '', actualHours: '', status: 'In Progress' };
  }

  const weeks = [...new Set(entries.map((e) => e.week))].sort((a, b) => a - b);
  const filtered = filterWeek === 'all' ? entries : entries.filter((e) => e.week === Number(filterWeek));

  function openAdd() {
    setForm(emptyEntry());
    setEditing(null);
    setShowModal(true);
  }

  function openEdit(entry) {
    setForm({ ...entry });
    setEditing(entry.id);
    setShowModal(true);
  }

  function handleSave() {
    if (!form.description.trim()) return;
    if (editing !== null) {
      setEntries((prev) => prev.map((e) => (e.id === editing ? { ...form, id: editing } : e)));
      flash('Activity updated.');
    } else {
      const newId = Math.max(0, ...entries.map((e) => e.id)) + 1;
      setEntries((prev) => [...prev, { ...form, id: newId }]);
      flash('Activity added.');
    }
    setShowModal(false);
  }

  function handleDelete(id) {
    setEntries((prev) => prev.filter((e) => e.id !== id));
  }

  function flash(msg) {
    setSuccess(msg);
    setTimeout(() => setSuccess(''), 3000);
  }

  const totalPlanned = filtered.reduce((s, e) => s + Number(e.plannedHours), 0);
  const totalActual  = filtered.reduce((s, e) => s + Number(e.actualHours), 0);

  return (
    <div>
      {success && <div className="alert alert-success">{success}</div>}

      {/* Controls row */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <label style={{ margin: 0 }}>Week:</label>
          <select
            value={filterWeek}
            onChange={(e) => setFilterWeek(e.target.value)}
            style={{ width: 'auto', padding: '6px 10px' }}
          >
            <option value="all">All weeks</option>
            {weeks.map((w) => <option key={w} value={w}>Week {w}</option>)}
          </select>
        </div>
        <button className="btn btn-primary" onClick={openAdd}>+ Add Activity</button>
      </div>

      {/* Summary stats */}
      <div className="stats-row" style={{ marginBottom: '16px' }}>
        <div className="stat-card">
          <div className="stat-value">{filtered.length}</div>
          <div className="stat-label">Activities</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{totalPlanned}h</div>
          <div className="stat-label">Planned Hours</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{totalActual}h</div>
          <div className="stat-label">Actual Hours</div>
        </div>
        <div className="stat-card">
          <div className="stat-value" style={{ color: totalActual > totalPlanned ? '#c62828' : '#2e7d32' }}>
            {totalActual > 0 ? ((totalActual / totalPlanned) * 100).toFixed(0) : 0}%
          </div>
          <div className="stat-label">Hours Utilization</div>
        </div>
      </div>

      {/* Table */}
      {filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📋</div>
          <h3>No activities yet</h3>
          <p>Click "Add Activity" to log your first entry for this week.</p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Week</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Planned (h)</th>
                  <th>Actual (h)</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((entry) => (
                  <tr key={entry.id}>
                    <td style={{ fontWeight: 600 }}>Wk {entry.week}</td>
                    <td><span className="badge badge-purple">{entry.category}</span></td>
                    <td>{entry.description}</td>
                    <td>{entry.plannedHours}</td>
                    <td>{entry.actualHours}</td>
                    <td><span className={`badge ${STATUS_BADGE[entry.status]}`}>{entry.status}</span></td>
                    <td>
                      <div className="flex gap-2">
                        <button className="btn btn-secondary btn-sm" onClick={() => openEdit(entry)}>Edit</button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(entry.id)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add / Edit Modal */}
      {showModal && (
        <Modal
          title={editing !== null ? 'Edit Activity' : 'Add Activity'}
          onClose={() => setShowModal(false)}
          onSubmit={handleSave}
        >
          <div className="grid-2">
            <div className="form-group">
              <label>Week Number</label>
              <input
                type="number"
                min={1}
                max={20}
                value={form.week}
                onChange={(e) => setForm({ ...form, week: Number(e.target.value) })}
              />
            </div>
            <div className="form-group">
              <label>Category</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {WAR_CATEGORIES.map((c) => <option key={c}>{c}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="What did you work on?"
            />
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label>Planned Hours</label>
              <input
                type="number"
                min={0}
                step={0.5}
                value={form.plannedHours}
                onChange={(e) => setForm({ ...form, plannedHours: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Actual Hours</label>
              <input
                type="number"
                min={0}
                step={0.5}
                value={form.actualHours}
                onChange={(e) => setForm({ ...form, actualHours: e.target.value })}
              />
            </div>
          </div>
          <div className="form-group">
            <label>Status</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {WAR_STATUSES.map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── Peer Eval Tab ──────────────────────────────────────────────────────────

function PeerEvalTab() {
  const { user } = useAuth();
  const allMembers = [{ id: user.id, name: `${user.name} (You)` }, ...TEAMMATES];

  const initScores = () => {
    const s = {};
    allMembers.forEach((m) => {
      s[m.id] = {};
      RUBRIC_CRITERIA.forEach((c) => { s[m.id][c.id] = 0; });
    });
    return s;
  };

  const initComments = () => {
    const c = {};
    allMembers.forEach((m) => { c[m.id] = { public: '', private: '' }; });
    return c;
  };

  const [scores, setScores]       = useState(initScores);
  const [comments, setComments]   = useState(initComments);
  const [submitted, setSubmitted] = useState(false);
  const [activeIdx, setActiveIdx] = useState(0);

  function setScore(memberId, criterionId, val) {
    setScores((prev) => ({
      ...prev,
      [memberId]: { ...prev[memberId], [criterionId]: Number(val) },
    }));
  }

  function setComment(memberId, field, val) {
    setComments((prev) => ({
      ...prev,
      [memberId]: { ...prev[memberId], [field]: val },
    }));
  }

  function memberTotal(memberId) {
    return RUBRIC_CRITERIA.reduce((s, c) => s + (scores[memberId]?.[c.id] ?? 0), 0);
  }

  const maxTotal = RUBRIC_CRITERIA.reduce((s, c) => s + c.maxScore, 0);
  const activeMember = allMembers[activeIdx];

  if (submitted) {
    return (
      <div className="empty-state">
        <div className="empty-icon">✅</div>
        <h3>Peer evaluation submitted!</h3>
        <p>Your responses have been recorded. Evaluations cannot be edited after submission.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="alert alert-info" style={{ marginBottom: '20px' }}>
        Evaluate each teammate for <strong>Week 2</strong>. Peer evaluations cannot be edited after submission (Business Rule BR-3).
      </div>

      {/* Member selector tabs */}
      <div className="tabs" style={{ marginBottom: '20px' }}>
        {allMembers.map((m, i) => (
          <button
            key={m.id}
            className={`tab${activeIdx === i ? ' active' : ''}`}
            onClick={() => setActiveIdx(i)}
          >
            {m.name.replace(' (You)', '')}
            <span style={{ marginLeft: '6px', fontSize: '0.75rem', color: 'var(--tcu-gold)' }}>
              {memberTotal(m.id)}/{maxTotal}
            </span>
          </button>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Evaluating: {activeMember.name}</h3>
          <span className="badge badge-purple">
            {memberTotal(activeMember.id)} / {maxTotal} pts
          </span>
        </div>

        {RUBRIC_CRITERIA.map((criterion) => (
          <div key={criterion.id} style={{ marginBottom: '20px' }}>
            <div className="flex items-center justify-between" style={{ marginBottom: '4px' }}>
              <h4 style={{ margin: 0 }}>{criterion.name}</h4>
              <span className="text-muted text-sm">max {criterion.maxScore} pts</span>
            </div>
            <p className="text-muted text-sm" style={{ marginBottom: '8px' }}>{criterion.description}</p>
            <div className="score-row">
              <input
                type="range"
                min={0}
                max={criterion.maxScore}
                step={1}
                value={scores[activeMember.id]?.[criterion.id] ?? 0}
                onChange={(e) => setScore(activeMember.id, criterion.id, e.target.value)}
              />
              <span className="score-display">
                {scores[activeMember.id]?.[criterion.id] ?? 0}/{criterion.maxScore}
              </span>
            </div>
          </div>
        ))}

        <div className="form-group">
          <label>Public Comment (visible to evaluatee)</label>
          <textarea
            value={comments[activeMember.id]?.public ?? ''}
            onChange={(e) => setComment(activeMember.id, 'public', e.target.value)}
            placeholder="Strengths, contributions, observations…"
          />
        </div>
        <div className="form-group">
          <label>Private Comment (instructor only)</label>
          <textarea
            value={comments[activeMember.id]?.private ?? ''}
            onChange={(e) => setComment(activeMember.id, 'private', e.target.value)}
            placeholder="Confidential feedback for the instructor…"
          />
        </div>
      </div>

      <div className="flex justify-between items-center mt-4">
        <div className="flex gap-2">
          <button className="btn btn-secondary" disabled={activeIdx === 0} onClick={() => setActiveIdx((i) => i - 1)}>← Previous</button>
          <button className="btn btn-secondary" disabled={activeIdx === allMembers.length - 1} onClick={() => setActiveIdx((i) => i + 1)}>Next →</button>
        </div>
        {activeIdx === allMembers.length - 1 && (
          <button className="btn btn-primary" onClick={() => setSubmitted(true)}>
            Submit Peer Evaluation
          </button>
        )}
      </div>
    </div>
  );
}

// ── My Report Tab ──────────────────────────────────────────────────────────

function MyReportTab() {
  const { overallGrade, totalMax, criteria, publicComments, week } = MY_REPORT;
  const pct = Math.round((overallGrade / totalMax) * 100);

  return (
    <div>
      <div className="alert alert-info" style={{ marginBottom: '20px' }}>
        Showing peer evaluation report for <strong>Week {week}</strong>.
        Private comments and evaluator identities are not shown (Business Rule BR-5).
      </div>

      {/* Overall score */}
      <div className="stats-row" style={{ marginBottom: '24px' }}>
        <div className="stat-card">
          <div className="stat-value">{overallGrade}/{totalMax}</div>
          <div className="stat-label">Overall Score</div>
        </div>
        <div className="stat-card">
          <div className="stat-value" style={{ color: pct >= 80 ? '#2e7d32' : pct >= 60 ? '#e65100' : '#c62828' }}>
            {pct}%
          </div>
          <div className="stat-label">Percentage</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{publicComments.length}</div>
          <div className="stat-label">Public Comments</div>
        </div>
      </div>

      {/* Per-criterion breakdown */}
      <div className="card" style={{ marginBottom: '20px' }}>
        <div className="card-header">
          <h3>Score Breakdown by Criterion</h3>
        </div>
        {criteria.map((c) => {
          const cpct = Math.round((c.myScore / c.maxScore) * 100);
          return (
            <div key={c.name} style={{ marginBottom: '16px' }}>
              <div className="flex justify-between items-center" style={{ marginBottom: '4px' }}>
                <span style={{ fontWeight: 600 }}>{c.name}</span>
                <span style={{ fontWeight: 700, color: 'var(--tcu-purple)' }}>
                  {c.myScore}/{c.maxScore}
                </span>
              </div>
              <div style={{ background: '#f0edf5', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                <div style={{
                  width: `${cpct}%`,
                  height: '100%',
                  background: cpct >= 80 ? '#2e7d32' : cpct >= 60 ? '#e65100' : '#c62828',
                  borderRadius: '4px',
                  transition: 'width 0.4s ease',
                }} />
              </div>
              <div className="text-muted text-sm" style={{ marginTop: '2px' }}>{cpct}%</div>
            </div>
          );
        })}
      </div>

      {/* Public comments */}
      {publicComments.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h3>Public Comments from Teammates</h3>
          </div>
          {publicComments.map((comment, i) => (
            <div key={i} style={{
              padding: '12px',
              background: 'var(--surface-2)',
              borderRadius: 'var(--radius)',
              marginBottom: i < publicComments.length - 1 ? '8px' : 0,
              borderLeft: '3px solid var(--tcu-gold)',
            }}>
              "{comment}"
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Main StudentDashboard ──────────────────────────────────────────────────

export default function StudentDashboard({ page }) {
  if (page === 'war')      return <WARTab />;
  if (page === 'peereval') return <PeerEvalTab />;
  if (page === 'myreport') return <MyReportTab />;
  return null;
}
