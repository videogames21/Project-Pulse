import { useState } from 'react';
import { INITIAL_SECTIONS, INITIAL_TEAMS, INITIAL_RUBRICS, RUBRIC_CRITERIA } from '../data/mockData';

// ── Reusable Modal ─────────────────────────────────────────────────────────
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

// ── Sections Tab ───────────────────────────────────────────────────────────

function SectionsTab() {
  const [sections, setSections]   = useState(INITIAL_SECTIONS);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm]           = useState({ name: '', startDate: '', endDate: '' });
  const [success, setSuccess]     = useState('');

  function handleCreate() {
    if (!form.name.trim()) return;
    const id = Math.max(0, ...sections.map((s) => s.id)) + 1;
    setSections((prev) => [...prev, { id, ...form, rubricId: 1, activeWeeks: [] }]);
    setForm({ name: '', startDate: '', endDate: '' });
    setShowModal(false);
    flash('Section created.');
  }

  function flash(msg) { setSuccess(msg); setTimeout(() => setSuccess(''), 3000); }

  return (
    <div>
      {success && <div className="alert alert-success">{success}</div>}
      <div className="flex justify-between items-center mb-4">
        <p className="text-muted">Manage Senior Design sections (one per academic year).</p>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ New Section</button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Section Name</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Active Weeks</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {sections.map((s) => (
                <tr key={s.id}>
                  <td style={{ fontWeight: 600 }}>{s.name}</td>
                  <td>{s.startDate}</td>
                  <td>{s.endDate}</td>
                  <td>{s.activeWeeks.length > 0 ? `Weeks ${s.activeWeeks[0]}–${s.activeWeeks[s.activeWeeks.length-1]}` : 'Not configured'}</td>
                  <td>
                    <span className={`badge ${s.activeWeeks.length > 0 ? 'badge-green' : 'badge-gray'}`}>
                      {s.activeWeeks.length > 0 ? 'Active' : 'Pending'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <Modal title="Create Section" onClose={() => setShowModal(false)} onSubmit={handleCreate}>
          <div className="form-group">
            <label>Section Name (YYYY-YYYY)</label>
            <input
              placeholder="e.g. 2025-2026"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label>Start Date</label>
              <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            </div>
            <div className="form-group">
              <label>End Date</label>
              <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── Teams Tab ──────────────────────────────────────────────────────────────

function TeamsTab() {
  const [teams, setTeams]         = useState(INITIAL_TEAMS);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm]           = useState({ name: '', description: '', website: '', sectionId: 1 });
  const [success, setSuccess]     = useState('');

  function handleCreate() {
    if (!form.name.trim()) return;
    const id = Math.max(0, ...teams.map((t) => t.id)) + 1;
    setTeams((prev) => [...prev, { id, ...form, memberCount: 0 }]);
    setForm({ name: '', description: '', website: '', sectionId: 1 });
    setShowModal(false);
    flash('Team created.');
  }

  function handleDelete(id) {
    if (!confirm('Delete this team? This will also delete all associated WARs and peer evaluations.')) return;
    setTeams((prev) => prev.filter((t) => t.id !== id));
  }

  function flash(msg) { setSuccess(msg); setTimeout(() => setSuccess(''), 3000); }

  const sectionName = (sid) => INITIAL_SECTIONS.find((s) => s.id === sid)?.name ?? '—';

  return (
    <div>
      {success && <div className="alert alert-success">{success}</div>}
      <div className="flex justify-between items-center mb-4">
        <p className="text-muted">Create and manage Senior Design project teams.</p>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ New Team</button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Description</th>
                <th>Website</th>
                <th>Section</th>
                <th>Members</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {teams.map((t) => (
                <tr key={t.id}>
                  <td style={{ fontWeight: 600 }}>{t.name}</td>
                  <td className="text-muted">{t.description}</td>
                  <td>
                    {t.website
                      ? <a href={t.website} target="_blank" rel="noreferrer" style={{ color: 'var(--tcu-purple)' }}>{t.website}</a>
                      : '—'}
                  </td>
                  <td>{sectionName(t.sectionId)}</td>
                  <td>{t.memberCount}</td>
                  <td>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(t.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <Modal title="Create Team" onClose={() => setShowModal(false)} onSubmit={handleCreate}>
          <div className="form-group">
            <label>Team Name</label>
            <input
              placeholder="e.g. Team Delta"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              placeholder="Brief project description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Website URL</label>
            <input
              placeholder="https://"
              value={form.website}
              onChange={(e) => setForm({ ...form, website: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Section</label>
            <select value={form.sectionId} onChange={(e) => setForm({ ...form, sectionId: Number(e.target.value) })}>
              {INITIAL_SECTIONS.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── Rubrics Tab ────────────────────────────────────────────────────────────

function RubricsTab() {
  const [rubrics, setRubrics]       = useState(INITIAL_RUBRICS);
  const [expanded, setExpanded]     = useState(1);
  const [showCriterionModal, setShowCriterionModal] = useState(false);
  const [criterionForm, setCriterionForm] = useState({ name: '', description: '', maxScore: 10 });
  const [showRubricModal, setShowRubricModal] = useState(false);
  const [rubricForm, setRubricForm] = useState({ name: '' });
  const [success, setSuccess]       = useState('');

  function addCriterion() {
    if (!criterionForm.name.trim()) return;
    setRubrics((prev) => prev.map((r) => {
      if (r.id !== expanded) return r;
      const newCrit = {
        id: Math.max(0, ...r.criteria.map((c) => c.id)) + 1,
        ...criterionForm,
        maxScore: Number(criterionForm.maxScore),
      };
      const updated = [...r.criteria, newCrit];
      return { ...r, criteria: updated, totalMax: updated.reduce((s, c) => s + c.maxScore, 0) };
    }));
    setCriterionForm({ name: '', description: '', maxScore: 10 });
    setShowCriterionModal(false);
    flash('Criterion added.');
  }

  function createRubric() {
    if (!rubricForm.name.trim()) return;
    const id = Math.max(0, ...rubrics.map((r) => r.id)) + 1;
    setRubrics((prev) => [...prev, { id, name: rubricForm.name, criteria: [], totalMax: 0 }]);
    setRubricForm({ name: '' });
    setShowRubricModal(false);
    flash('Rubric created.');
  }

  function flash(msg) { setSuccess(msg); setTimeout(() => setSuccess(''), 3000); }

  return (
    <div>
      {success && <div className="alert alert-success">{success}</div>}
      <div className="flex justify-between items-center mb-4">
        <p className="text-muted">Define peer evaluation rubrics with scored criteria.</p>
        <button className="btn btn-primary" onClick={() => setShowRubricModal(true)}>+ New Rubric</button>
      </div>

      {rubrics.map((rubric) => (
        <div key={rubric.id} className="card" style={{ marginBottom: '16px' }}>
          <div
            className="card-header"
            style={{ cursor: 'pointer' }}
            onClick={() => setExpanded(expanded === rubric.id ? null : rubric.id)}
          >
            <div>
              <h3 style={{ margin: 0 }}>{rubric.name}</h3>
              <p className="text-muted text-sm" style={{ marginTop: '2px' }}>
                {rubric.criteria.length} criteria · {rubric.totalMax} pts total
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="badge badge-purple">{rubric.totalMax} pts max</span>
              <span>{expanded === rubric.id ? '▲' : '▼'}</span>
            </div>
          </div>

          {expanded === rubric.id && (
            <>
              {rubric.criteria.length === 0 ? (
                <div className="empty-state" style={{ padding: '24px' }}>
                  <p>No criteria yet. Add one below.</p>
                </div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Criterion</th>
                        <th>Description</th>
                        <th>Max Score</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rubric.criteria.map((c) => (
                        <tr key={c.id}>
                          <td style={{ fontWeight: 600 }}>{c.name}</td>
                          <td className="text-muted">{c.description}</td>
                          <td><span className="badge badge-purple">{c.maxScore} pts</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <div style={{ padding: '12px 0 0' }}>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowCriterionModal(true)}
                >
                  + Add Criterion
                </button>
              </div>
            </>
          )}
        </div>
      ))}

      {/* Create Rubric Modal */}
      {showRubricModal && (
        <Modal title="Create Rubric" onClose={() => setShowRubricModal(false)} onSubmit={createRubric}>
          <div className="form-group">
            <label>Rubric Name</label>
            <input
              placeholder="e.g. Standard Peer Evaluation 2025"
              value={rubricForm.name}
              onChange={(e) => setRubricForm({ name: e.target.value })}
            />
          </div>
        </Modal>
      )}

      {/* Add Criterion Modal */}
      {showCriterionModal && (
        <Modal title="Add Criterion" onClose={() => setShowCriterionModal(false)} onSubmit={addCriterion}>
          <div className="form-group">
            <label>Criterion Name</label>
            <input
              placeholder="e.g. Technical Contribution"
              value={criterionForm.name}
              onChange={(e) => setCriterionForm({ ...criterionForm, name: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              placeholder="What does this criterion measure?"
              value={criterionForm.description}
              onChange={(e) => setCriterionForm({ ...criterionForm, description: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Max Score</label>
            <input
              type="number"
              min={1}
              max={100}
              value={criterionForm.maxScore}
              onChange={(e) => setCriterionForm({ ...criterionForm, maxScore: e.target.value })}
            />
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── Invitations Tab ────────────────────────────────────────────────────────

function InvitationsTab() {
  const [sent, setSent]   = useState([
    { id: 1, email: 'david@tcu.edu',  role: 'student',    team: 'Team Alpha', sentAt: '2024-08-25', status: 'accepted' },
    { id: 2, email: 'grace@tcu.edu',  role: 'student',    team: 'Team Beta',  sentAt: '2024-08-25', status: 'pending' },
    { id: 3, email: 'prof@tcu.edu',   role: 'instructor', team: 'Team Alpha', sentAt: '2024-08-20', status: 'accepted' },
  ]);
  const [form, setForm]         = useState({ email: '', role: 'student', team: '' });
  const [showModal, setShowModal] = useState(false);
  const [success, setSuccess]   = useState('');

  function handleSend() {
    if (!form.email.trim()) return;
    const id = Math.max(0, ...sent.map((s) => s.id)) + 1;
    setSent((prev) => [...prev, { id, ...form, sentAt: new Date().toISOString().split('T')[0], status: 'pending' }]);
    setForm({ email: '', role: 'student', team: '' });
    setShowModal(false);
    flash(`Invitation sent to ${form.email}.`);
  }

  function flash(msg) { setSuccess(msg); setTimeout(() => setSuccess(''), 3000); }

  const STATUS_BADGE = { pending: 'badge-orange', accepted: 'badge-green', expired: 'badge-gray' };

  return (
    <div>
      {success && <div className="alert alert-success">{success}</div>}
      <div className="flex justify-between items-center mb-4">
        <p className="text-muted">Invite students and instructors via email (unique registration links).</p>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Send Invitation</button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Role</th>
                <th>Team</th>
                <th>Sent</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {sent.map((inv) => (
                <tr key={inv.id}>
                  <td>{inv.email}</td>
                  <td><span className="badge badge-purple" style={{ textTransform: 'capitalize' }}>{inv.role}</span></td>
                  <td>{inv.team || '—'}</td>
                  <td>{inv.sentAt}</td>
                  <td>
                    <span className={`badge ${STATUS_BADGE[inv.status]}`} style={{ textTransform: 'capitalize' }}>
                      {inv.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <Modal title="Send Invitation" onClose={() => setShowModal(false)} onSubmit={handleSend} submitLabel="Send Invite">
          <div className="form-group">
            <label>Email Address</label>
            <input
              type="email"
              placeholder="name@tcu.edu"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Role</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="student">Student</option>
              <option value="instructor">Instructor</option>
            </select>
          </div>
          <div className="form-group">
            <label>Assign to Team (optional)</label>
            <select value={form.team} onChange={(e) => setForm({ ...form, team: e.target.value })}>
              <option value="">— Select a team —</option>
              {INITIAL_TEAMS.map((t) => <option key={t.id} value={t.name}>{t.name}</option>)}
            </select>
          </div>
          <div className="alert alert-info" style={{ marginTop: '4px' }}>
            A unique registration link will be emailed to the recipient via Gmail.
          </div>
        </Modal>
      )}
    </div>
  );
}

// ── Main AdminDashboard ────────────────────────────────────────────────────

export default function AdminDashboard({ page }) {
  if (page === 'sections')    return <SectionsTab />;
  if (page === 'teams')       return <TeamsTab />;
  if (page === 'rubrics')     return <RubricsTab />;
  if (page === 'invitations') return <InvitationsTab />;
  return null;
}
