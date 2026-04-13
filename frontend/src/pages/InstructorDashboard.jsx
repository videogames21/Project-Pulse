import { useState } from 'react';
import { SECTION_REPORT, TEAM_WAR_REPORT, INITIAL_TEAMS } from '../data/mockData';

// ── Section Peer Eval Report ───────────────────────────────────────────────

function SectionReportTab() {
  const [selectedTeam, setSelectedTeam] = useState('all');
  const [selectedWeek, setSelectedWeek] = useState(2);

  const submitted   = SECTION_REPORT.filter((s) => s.submitted);
  const nonSubmitters = SECTION_REPORT.filter((s) => !s.submitted);

  const avgScore = submitted.length
    ? (submitted.reduce((s, r) => s + (r.total ?? 0), 0) / submitted.length).toFixed(1)
    : 0;

  return (
    <div>
      {/* Controls */}
      <div className="flex items-center gap-3 mb-4">
        <div className="form-group" style={{ margin: 0, flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
          <label style={{ margin: 0, whiteSpace: 'nowrap' }}>Team:</label>
          <select
            value={selectedTeam}
            onChange={(e) => setSelectedTeam(e.target.value)}
            style={{ width: 'auto', padding: '6px 10px' }}
          >
            <option value="all">All Teams</option>
            {INITIAL_TEAMS.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ margin: 0, flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
          <label style={{ margin: 0, whiteSpace: 'nowrap' }}>Week:</label>
          <select
            value={selectedWeek}
            onChange={(e) => setSelectedWeek(Number(e.target.value))}
            style={{ width: 'auto', padding: '6px 10px' }}
          >
            {[1,2,3,4,5].map((w) => <option key={w} value={w}>Week {w}</option>)}
          </select>
        </div>
        <button className="btn btn-secondary btn-sm">Export CSV</button>
      </div>

      {/* Stats */}
      <div className="stats-row" style={{ marginBottom: '20px' }}>
        <div className="stat-card">
          <div className="stat-value">{SECTION_REPORT.length}</div>
          <div className="stat-label">Total Students</div>
        </div>
        <div className="stat-card">
          <div className="stat-value" style={{ color: '#2e7d32' }}>{submitted.length}</div>
          <div className="stat-label">Submitted</div>
        </div>
        <div className="stat-card">
          <div className="stat-value" style={{ color: nonSubmitters.length > 0 ? '#c62828' : '#2e7d32' }}>
            {nonSubmitters.length}
          </div>
          <div className="stat-label">Non-Submitters</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{avgScore}</div>
          <div className="stat-label">Section Avg Score</div>
        </div>
      </div>

      {/* Non-submitter warning */}
      {nonSubmitters.length > 0 && (
        <div className="alert alert-warning" style={{ marginBottom: '16px' }}>
          <strong>Missing submissions:</strong>{' '}
          {nonSubmitters.map((s) => s.student).join(', ')} did not submit a peer evaluation for Week {selectedWeek}.
        </div>
      )}

      {/* Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>Submitted</th>
                <th>Score</th>
                <th>Max</th>
                <th>Percentage</th>
                <th>Evaluations Received</th>
              </tr>
            </thead>
            <tbody>
              {SECTION_REPORT.map((row, i) => {
                const pct = row.total !== null ? Math.round((row.total / row.max) * 100) : null;
                return (
                  <tr key={i}>
                    <td style={{ fontWeight: 600 }}>{row.student}</td>
                    <td>
                      {row.submitted
                        ? <span className="badge badge-green">Yes</span>
                        : <span className="badge badge-orange">No</span>}
                    </td>
                    <td>{row.total ?? '—'}</td>
                    <td>{row.max}</td>
                    <td>
                      {pct !== null ? (
                        <div className="flex items-center gap-2">
                          <div style={{ flex: 1, background: '#f0edf5', borderRadius: '4px', height: '6px', minWidth: '80px' }}>
                            <div style={{
                              width: `${pct}%`,
                              height: '100%',
                              background: pct >= 80 ? '#2e7d32' : pct >= 60 ? '#e65100' : '#c62828',
                              borderRadius: '4px',
                            }} />
                          </div>
                          <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>{pct}%</span>
                        </div>
                      ) : '—'}
                    </td>
                    <td>{row.evaluatedBy} / {SECTION_REPORT.length - 1} teammates</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ── Team WAR Report ────────────────────────────────────────────────────────

function TeamWARTab() {
  const [selectedTeam, setSelectedTeam] = useState(1);
  const [selectedWeek, setSelectedWeek] = useState(2);

  const submitted     = TEAM_WAR_REPORT.filter((r) => r.submitted);
  const nonSubmitters = TEAM_WAR_REPORT.filter((r) => !r.submitted);
  const totalPlanned  = submitted.reduce((s, r) => s + r.totalPlanned, 0);
  const totalActual   = submitted.reduce((s, r) => s + r.totalActual, 0);

  const teamName = INITIAL_TEAMS.find((t) => t.id === selectedTeam)?.name ?? '';

  return (
    <div>
      {/* Controls */}
      <div className="flex items-center gap-3 mb-4">
        <div className="form-group" style={{ margin: 0, flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
          <label style={{ margin: 0, whiteSpace: 'nowrap' }}>Team:</label>
          <select
            value={selectedTeam}
            onChange={(e) => setSelectedTeam(Number(e.target.value))}
            style={{ width: 'auto', padding: '6px 10px' }}
          >
            {INITIAL_TEAMS.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ margin: 0, flexDirection: 'row', alignItems: 'center', gap: '8px' }}>
          <label style={{ margin: 0, whiteSpace: 'nowrap' }}>Week:</label>
          <select
            value={selectedWeek}
            onChange={(e) => setSelectedWeek(Number(e.target.value))}
            style={{ width: 'auto', padding: '6px 10px' }}
          >
            {[1,2,3,4,5].map((w) => <option key={w} value={w}>Week {w}</option>)}
          </select>
        </div>
        <button className="btn btn-secondary btn-sm">Export CSV</button>
      </div>

      <div className="stats-row" style={{ marginBottom: '20px' }}>
        <div className="stat-card">
          <div className="stat-value">{TEAM_WAR_REPORT.length}</div>
          <div className="stat-label">Team Members</div>
        </div>
        <div className="stat-card">
          <div className="stat-value" style={{ color: '#2e7d32' }}>{submitted.length}</div>
          <div className="stat-label">Submitted WARs</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{totalPlanned}h</div>
          <div className="stat-label">Team Planned Hrs</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{totalActual}h</div>
          <div className="stat-label">Team Actual Hrs</div>
        </div>
      </div>

      {nonSubmitters.length > 0 && (
        <div className="alert alert-warning" style={{ marginBottom: '16px' }}>
          <strong>Missing WARs:</strong>{' '}
          {nonSubmitters.map((s) => s.student).join(', ')} did not submit a WAR for Week {selectedWeek}.
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>WAR Submitted</th>
                <th>Activities</th>
                <th>Planned Hours</th>
                <th>Actual Hours</th>
                <th>Variance</th>
              </tr>
            </thead>
            <tbody>
              {TEAM_WAR_REPORT.map((row, i) => {
                const variance = row.totalActual - row.totalPlanned;
                return (
                  <tr key={i}>
                    <td style={{ fontWeight: 600 }}>{row.student}</td>
                    <td>
                      {row.submitted
                        ? <span className="badge badge-green">Yes</span>
                        : <span className="badge badge-orange">No</span>}
                    </td>
                    <td>{row.entries}</td>
                    <td>{row.totalPlanned}h</td>
                    <td>{row.totalActual}h</td>
                    <td style={{ color: variance > 0 ? '#c62828' : variance < 0 ? '#1565c0' : '#2e7d32', fontWeight: 600 }}>
                      {variance > 0 ? '+' : ''}{variance}h
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ── Main InstructorDashboard ───────────────────────────────────────────────

export default function InstructorDashboard({ page }) {
  if (page === 'section-report') return <SectionReportTab />;
  if (page === 'team-war')       return <TeamWARTab />;
  return null;
}
