import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, errorMessage } from './api';
import { addDays, byRating, formatDateTime, formatRange, toDateTimeInput, toIso } from './utils';

const navigation = [
  { id: 'dashboard', label: 'Overview' },
  { id: 'players', label: 'Players & Ladder' },
  { id: 'tournaments', label: 'Tournaments' },
  { id: 'practice', label: 'Practice Blocks' }
];

function App() {
  const [view, setView] = useState('dashboard');
  const [players, setPlayers] = useState([]);
  const [tournaments, setTournaments] = useState([]);
  const [practiceSessions, setPracticeSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const refreshData = useCallback(async () => {
    setLoading(true);
    try {
      const [nextPlayers, nextTournaments, nextSessions] = await Promise.all([
        api.listPlayers(),
        api.listTournaments(),
        api.listPracticeSessions()
      ]);
      setPlayers(nextPlayers);
      setTournaments(nextTournaments);
      setPracticeSessions(nextSessions);
    } catch (error) {
      setNotice({ type: 'error', text: `Could not load club data: ${errorMessage(error)}` });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshData();
  }, [refreshData]);

  const perform = useCallback(async (action, successText) => {
    try {
      const result = await action();
      await refreshData();
      setNotice({ type: 'success', text: successText });
      return result;
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
      return null;
    }
  }, [refreshData]);

  const actions = useMemo(() => ({
    createPlayer: (payload) => perform(() => api.createPlayer(payload), 'Player added to the club ladder.'),
    recordLadderMatch: async ({ playerOneId, playerTwoId, playerOneScore, playerTwoScore }) => {
      try {
        const match = await api.createMatch({ playerOneId, playerTwoId, roundNumber: 1 });
        const result = await api.submitMatchResult(match.id, { playerOneScore, playerTwoScore });
        await refreshData();
        setNotice({ type: 'success', text: 'Match saved and USATT ratings updated.' });
        return result;
      } catch (error) {
        setNotice({ type: 'error', text: errorMessage(error) });
        return null;
      }
    },
    createTournament: (payload) => perform(() => api.createTournament(payload), 'Tournament created. Registration is now open.'),
    registerTournamentPlayer: (tournamentId, playerId) =>
      perform(() => api.registerForTournament(tournamentId, playerId), 'Player registered for the tournament.'),
    scheduleTournamentMatch: (payload) => perform(() => api.createMatch(payload), 'Tournament match added to the bracket.'),
    submitTournamentResult: (matchId, scores) =>
      perform(() => api.submitMatchResult(matchId, scores), 'Bracket result recorded and USATT ratings updated.'),
    createPracticeSession: (payload) => perform(() => api.createPracticeSession(payload), 'Practice block published.'),
    registerPracticePlayer: (sessionId, playerId) =>
      perform(() => api.registerForPractice(sessionId, playerId), 'Player registered for practice.')
  }), [perform, refreshData]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand" aria-label="Third Ball home">
          <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
          <div>
            <p className="eyebrow">University Table Tennis Club</p>
            <h1>Third Ball</h1>
          </div>
        </div>
        <div className="topbar-actions">
          <span className="live-indicator"><span /> Club system online</span>
          <button className="button button-quiet" onClick={refreshData} disabled={loading}>
            {loading ? 'Syncing…' : 'Refresh'}
          </button>
        </div>
      </header>

      <div className="workspace">
        <aside className="sidebar" aria-label="Primary navigation">
          <nav>
            {navigation.map((item) => (
              <button
                className={`nav-item ${view === item.id ? 'is-active' : ''}`}
                key={item.id}
                onClick={() => setView(item.id)}
              >
                <span className={`nav-dot nav-dot-${item.id}`} aria-hidden="true" />
                {item.label}
              </button>
            ))}
          </nav>
          <div className="sidebar-note">
            <strong>250+ members, one home court.</strong>
            <p>Manage sessions, the club ladder, and every elimination match from one place.</p>
          </div>
        </aside>

        <main className="content">
          {notice && <Notice notice={notice} onClose={() => setNotice(null)} />}
          {view === 'dashboard' && <Dashboard players={players} tournaments={tournaments} practiceSessions={practiceSessions} onNavigate={setView} />}
          {view === 'players' && <PlayersAndLadder players={players} actions={actions} />}
          {view === 'tournaments' && <TournamentHub players={players} tournaments={tournaments} actions={actions} />}
          {view === 'practice' && <PracticeHub players={players} practiceSessions={practiceSessions} actions={actions} />}
        </main>
      </div>
    </div>
  );
}

function Notice({ notice, onClose }) {
  return (
    <div className={`notice notice-${notice.type}`} role="status">
      <span>{notice.type === 'success' ? '✓' : '!'}</span>
      <p>{notice.text}</p>
      <button aria-label="Dismiss notification" onClick={onClose}>×</button>
    </div>
  );
}

function PageIntro({ eyebrow, title, children, action }) {
  return (
    <section className="page-intro">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        {children && <p>{children}</p>}
      </div>
      {action}
    </section>
  );
}

function Dashboard({ players, tournaments, practiceSessions, onNavigate }) {
  const ladder = byRating(players);
  const upcomingPractice = [...practiceSessions]
    .filter((session) => new Date(session.endsAt) >= new Date())
    .sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))[0];
  const activeTournament = tournaments.find((tournament) => tournament.status === 'REGISTRATION_OPEN');

  return (
    <>
      <PageIntro eyebrow="Club command center" title="Keep every rally moving.">
        A simple operational view for a busy university club. Start with a player, a practice block, or the next tournament.
      </PageIntro>

      <section className="stat-grid" aria-label="Club totals">
        <StatCard value={players.length} label="Active members" tone="blue" />
        <StatCard value={tournaments.filter((t) => t.status !== 'COMPLETED').length} label="Open tournaments" tone="yellow" />
        <StatCard value={practiceSessions.length} label="Practice blocks" tone="coral" />
      </section>

      <section className="dashboard-grid">
        <article className="panel leader-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Internal USATT ladder</p>
              <h3>Top of the table</h3>
            </div>
            <button className="text-button" onClick={() => onNavigate('players')}>Manage ladder →</button>
          </div>
          {ladder.length ? (
            <ol className="leader-list">
              {ladder.slice(0, 5).map((player, index) => (
                <li key={player.id}>
                  <span className={`rank rank-${index + 1}`}>{index + 1}</span>
                  <span className="avatar">{player.displayName.slice(0, 1).toUpperCase()}</span>
                  <strong>{player.displayName}</strong>
                  <span>{player.rating}</span>
                </li>
              ))}
            </ol>
          ) : <EmptyState compact text="Add club members to start the ladder." />}
        </article>

        <article className="panel spotlight-panel">
          <p className="eyebrow">Next up</p>
          {upcomingPractice ? (
            <>
              <h3>{upcomingPractice.title}</h3>
              <p className="spotlight-time">{formatRange(upcomingPractice.startsAt, upcomingPractice.endsAt)}</p>
              <p>{upcomingPractice.location} · {upcomingPractice.registeredCount}/{upcomingPractice.capacity} registered</p>
              <button className="button button-light" onClick={() => onNavigate('practice')}>Open practice board</button>
            </>
          ) : (
            <>
              <h3>No practice booked</h3>
              <p>Create the next block so members know where to be.</p>
              <button className="button button-light" onClick={() => onNavigate('practice')}>Create a block</button>
            </>
          )}
        </article>

        <article className="panel tournament-summary">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Tournament desk</p>
              <h3>{activeTournament ? activeTournament.name : 'No registration open'}</h3>
            </div>
            <button className="text-button" onClick={() => onNavigate('tournaments')}>Tournament hub →</button>
          </div>
          {activeTournament ? (
            <div className="progress-detail">
              <div><span>Registration</span><strong>{activeTournament.registeredCount}/{activeTournament.maxParticipants}</strong></div>
              <div className="progress"><span style={{ width: `${Math.min(100, (activeTournament.registeredCount / activeTournament.maxParticipants) * 100)}%` }} /></div>
              <p>{formatDateTime(activeTournament.startsAt)} · {activeTournament.location || 'Location TBA'}</p>
            </div>
          ) : <EmptyState compact text="Create an event to open tournament registration." />}
        </article>
      </section>
    </>
  );
}

function StatCard({ value, label, tone }) {
  return (
    <article className={`stat-card stat-${tone}`}>
      <strong>{value}</strong>
      <span>{label}</span>
    </article>
  );
}

function PlayersAndLadder({ players, actions }) {
  const orderedPlayers = byRating(players);
  return (
    <>
      <PageIntro eyebrow="Membership & competition" title="Players and club ladder">
        New members begin at 1200. Submit a result once and Third Ball applies the USATT point-exchange chart automatically.
      </PageIntro>
      <section className="two-column-layout">
        <PlayerForm onSubmit={actions.createPlayer} />
        <LadderMatchForm players={orderedPlayers} onSubmit={actions.recordLadderMatch} />
      </section>
      <section className="panel table-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Live internal ranking</p>
            <h3>Club ladder</h3>
          </div>
          <span className="count-chip">{players.length} players</span>
        </div>
        {orderedPlayers.length ? <LadderTable players={orderedPlayers} /> : <EmptyState text="Your club roster will appear here after you add the first player." />}
      </section>
    </>
  );
}

function PlayerForm({ onSubmit }) {
  const [form, setForm] = useState({ displayName: '', email: '' });
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit(form);
    if (result) setForm({ displayName: '', email: '' });
    setSubmitting(false);
  }

  return (
    <section className="panel form-panel">
      <p className="eyebrow">Roster intake</p>
      <h3>Add a player</h3>
      <form onSubmit={submit}>
        <label>Full name<input required maxLength="100" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} placeholder="Jordan Patel" /></label>
        <label>University email<input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="jordan@university.edu" /></label>
        <button className="button button-primary" disabled={submitting}>{submitting ? 'Adding…' : 'Add to ladder'}</button>
      </form>
    </section>
  );
}

function LadderMatchForm({ players, onSubmit }) {
  const [form, setForm] = useState({ playerOneId: '', playerTwoId: '', playerOneScore: '3', playerTwoScore: '0' });
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const next = await onSubmit({
      playerOneId: Number(form.playerOneId),
      playerTwoId: Number(form.playerTwoId),
      playerOneScore: Number(form.playerOneScore),
      playerTwoScore: Number(form.playerTwoScore)
    });
    if (next) setResult(next);
    setSubmitting(false);
  }

  return (
    <section className="panel form-panel match-form-panel">
      <p className="eyebrow">Ladder match</p>
      <h3>Record a result</h3>
      {players.length < 2 ? <EmptyState compact text="Add at least two players before submitting a match." /> : (
        <form onSubmit={submit}>
          <div className="versus-selects">
            <label>Player one<PlayerSelect required players={players} value={form.playerOneId} onChange={(value) => setForm({ ...form, playerOneId: value })} placeholder="Choose player" /></label>
            <span className="versus">vs</span>
            <label>Player two<PlayerSelect required players={players} value={form.playerTwoId} onChange={(value) => setForm({ ...form, playerTwoId: value })} placeholder="Choose player" /></label>
          </div>
          <div className="score-inputs">
            <label>Player one games<input required type="number" min="0" value={form.playerOneScore} onChange={(e) => setForm({ ...form, playerOneScore: e.target.value })} /></label>
            <label>Player two games<input required type="number" min="0" value={form.playerTwoScore} onChange={(e) => setForm({ ...form, playerTwoScore: e.target.value })} /></label>
          </div>
          <button className="button button-primary" disabled={submitting || !form.playerOneId || !form.playerTwoId}>{submitting ? 'Updating rating…' : 'Save match & update rating'}</button>
        </form>
      )}
      {result && <RatingResult result={result} />}
    </section>
  );
}

function RatingResult({ result }) {
  return (
    <div className="rating-result">
      <strong>{result.winnerId === result.playerOneId ? result.playerOneName : result.playerTwoName} wins</strong>
      <div>
        <span>{result.playerOneName}: {result.playerOneRatingBefore} → <b>{result.playerOneRatingAfter}</b></span>
        <span>{result.playerTwoName}: {result.playerTwoRatingBefore} → <b>{result.playerTwoRatingAfter}</b></span>
      </div>
    </div>
  );
}

function LadderTable({ players }) {
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Rank</th><th>Player</th><th>Email</th><th className="right">USATT rating</th><th>Status</th></tr></thead>
        <tbody>
          {players.map((player, index) => (
            <tr key={player.id}>
              <td><span className="rank rank-table">{index + 1}</span></td>
              <td><div className="player-cell"><span className="avatar">{player.displayName.slice(0, 1).toUpperCase()}</span><strong>{player.displayName}</strong></div></td>
              <td>{player.email}</td>
              <td className="right rating-number">{player.rating}</td>
              <td><span className={`status-pill ${player.active ? 'status-active' : 'status-muted'}`}>{player.active ? 'Active' : 'Inactive'}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TournamentHub({ players, tournaments, actions }) {
  const [selectedId, setSelectedId] = useState('');
  const selectedTournament = tournaments.find((tournament) => String(tournament.id) === String(selectedId));

  useEffect(() => {
    if (!selectedId && tournaments.length) setSelectedId(String(tournaments[0].id));
  }, [selectedId, tournaments]);

  return (
    <>
      <PageIntro eyebrow="Single-elimination events" title="Tournament control room">
        Open registration, confirm players, then add and progress each bracket match. The bracket below always reads the latest match results.
      </PageIntro>
      <section className="two-column-layout tournament-forms">
        <TournamentForm onSubmit={actions.createTournament} />
        <TournamentRegistration players={players} tournaments={tournaments} selectedId={selectedId} onSelect={setSelectedId} onSubmit={actions.registerTournamentPlayer} />
      </section>
      <section className="panel bracket-panel">
        <div className="panel-heading bracket-heading">
          <div>
            <p className="eyebrow">Live bracket</p>
            <h3>{selectedTournament ? selectedTournament.name : 'Select a tournament'}</h3>
          </div>
          {tournaments.length > 0 && <select aria-label="Select tournament bracket" value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>{tournaments.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}</select>}
        </div>
        {selectedTournament ? <TournamentBracket tournament={selectedTournament} players={players} actions={actions} /> : <EmptyState text="Create a tournament to begin its bracket." />}
      </section>
    </>
  );
}

function TournamentForm({ onSubmit }) {
  const [form, setForm] = useState({
    name: '', description: '', location: '', startsAt: addDays(14, 10), endsAt: addDays(14, 17), maxParticipants: '32'
  });
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit({ ...form, startsAt: toIso(form.startsAt), endsAt: toIso(form.endsAt), maxParticipants: Number(form.maxParticipants) });
    if (result) setForm({ ...form, name: '', description: '', location: '' });
    setSubmitting(false);
  }

  return (
    <section className="panel form-panel">
      <p className="eyebrow">Event setup</p><h3>Create tournament</h3>
      <form onSubmit={submit}>
        <label>Tournament name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Fall Open 2026" /></label>
        <label>Description<textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Singles, best of five games." rows="2" /></label>
        <div className="form-grid"><label>Location<input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="Rec Center 201" /></label><label>Capacity<input required type="number" min="2" value={form.maxParticipants} onChange={(e) => setForm({ ...form, maxParticipants: e.target.value })} /></label></div>
        <div className="form-grid"><label>Starts<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>Ends<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></label></div>
        <button className="button button-primary" disabled={submitting}>{submitting ? 'Creating…' : 'Open registration'}</button>
      </form>
    </section>
  );
}

function TournamentRegistration({ players, tournaments, selectedId, onSelect, onSubmit }) {
  const [playerId, setPlayerId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit(Number(selectedId), Number(playerId));
    if (result) setPlayerId('');
    setSubmitting(false);
  }

  return (
    <section className="panel form-panel registration-panel">
      <p className="eyebrow">Player check-in</p><h3>Register for tournament</h3>
      {!tournaments.length ? <EmptyState compact text="Create a tournament first." /> : !players.length ? <EmptyState compact text="Add a player to open registration." /> : (
        <form onSubmit={submit}>
          <label>Tournament<select value={selectedId} onChange={(e) => onSelect(e.target.value)}>{tournaments.map((tournament) => <option key={tournament.id} value={tournament.id}>{tournament.name} ({tournament.registeredCount}/{tournament.maxParticipants})</option>)}</select></label>
          <label>Player<PlayerSelect required players={players} value={playerId} onChange={setPlayerId} placeholder="Choose player" /></label>
          <button className="button button-secondary" disabled={submitting || !playerId}>{submitting ? 'Registering…' : 'Add to field'}</button>
        </form>
      )}
      {selectedId && <p className="form-hint">Only registered players can be placed into this tournament’s matches.</p>}
    </section>
  );
}

function TournamentBracket({ tournament, players, actions }) {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api.listTournamentMatches(tournament.id)
      .then((data) => { if (active) { setMatches(data); setError(''); } })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [tournament.id, refreshIndex]);

  const rounds = useMemo(() => matches.reduce((grouped, match) => {
    const key = `Round ${match.roundNumber}`;
    (grouped[key] ||= []).push(match);
    return grouped;
  }, {}), [matches]);

  async function schedule(payload) {
    const result = await actions.scheduleTournamentMatch({ ...payload, tournamentId: tournament.id });
    if (result) setRefreshIndex((value) => value + 1);
    return result;
  }

  async function submitResult(matchId, scores) {
    const result = await actions.submitTournamentResult(matchId, scores);
    if (result) setRefreshIndex((value) => value + 1);
    return result;
  }

  return (
    <div className="bracket-workspace">
      <BracketMatchForm players={players} onSubmit={schedule} />
      {loading ? <p className="loading-copy">Loading bracket…</p> : error ? <div className="inline-error">{error}</div> : Object.keys(rounds).length ? (
        <div className="bracket-scroll">
          <div className="bracket-grid">
            {Object.entries(rounds).map(([roundName, roundMatches]) => (
              <section className="bracket-round" key={roundName}>
                <h4>{roundName}</h4>
                <div className="match-stack">
                  {roundMatches.map((match) => <BracketMatch key={match.id} match={match} onSubmitResult={submitResult} />)}
                </div>
              </section>
            ))}
          </div>
        </div>
      ) : <EmptyState text="No matches have been placed in this bracket yet. Add a first-round match above after registering both players." />}
    </div>
  );
}

function BracketMatchForm({ players, onSubmit }) {
  const [form, setForm] = useState({ playerOneId: '', playerTwoId: '', roundNumber: '1', bracketSlot: '' });
  const [submitting, setSubmitting] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const payload = {
      playerOneId: Number(form.playerOneId), playerTwoId: Number(form.playerTwoId), roundNumber: Number(form.roundNumber),
      ...(form.bracketSlot ? { bracketSlot: Number(form.bracketSlot) } : {})
    };
    const result = await onSubmit(payload);
    if (result) setForm({ ...form, playerOneId: '', playerTwoId: '', bracketSlot: '' });
    setSubmitting(false);
  }
  return (
    <form className="bracket-form" onSubmit={submit}>
      <strong>Add bracket match</strong>
      <PlayerSelect required players={players} value={form.playerOneId} onChange={(value) => setForm({ ...form, playerOneId: value })} placeholder="Player one" />
      <span className="versus compact-versus">vs</span>
      <PlayerSelect required players={players} value={form.playerTwoId} onChange={(value) => setForm({ ...form, playerTwoId: value })} placeholder="Player two" />
      <label>Round<input aria-label="Round number" required type="number" min="1" value={form.roundNumber} onChange={(e) => setForm({ ...form, roundNumber: e.target.value })} /></label>
      <label>Slot<input aria-label="Bracket slot" type="number" min="1" value={form.bracketSlot} onChange={(e) => setForm({ ...form, bracketSlot: e.target.value })} placeholder="Optional" /></label>
      <button className="button button-secondary" disabled={submitting || !form.playerOneId || !form.playerTwoId}>{submitting ? 'Adding…' : 'Add match'}</button>
    </form>
  );
}

function BracketMatch({ match, onSubmitResult }) {
  const [scores, setScores] = useState({ playerOneScore: '', playerTwoScore: '' });
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmitResult(match.id, { playerOneScore: Number(scores.playerOneScore), playerTwoScore: Number(scores.playerTwoScore) });
    if (result) setEditing(false);
    setSubmitting(false);
  }
  return (
    <article className={`bracket-match ${match.status === 'COMPLETED' ? 'is-complete' : ''}`}>
      <div className={`bracket-player ${match.winnerId === match.playerOneId ? 'is-winner' : ''}`}><span>{match.playerOneName}</span><strong>{match.playerOneScore ?? '—'}</strong></div>
      <div className={`bracket-player ${match.winnerId === match.playerTwoId ? 'is-winner' : ''}`}><span>{match.playerTwoName}</span><strong>{match.playerTwoScore ?? '—'}</strong></div>
      {match.status === 'SCHEDULED' && !editing && <button className="text-button result-button" onClick={() => setEditing(true)}>Enter result</button>}
      {editing && <form className="bracket-score-form" onSubmit={submit}><input required type="number" min="0" value={scores.playerOneScore} onChange={(e) => setScores({ ...scores, playerOneScore: e.target.value })} /><span>–</span><input required type="number" min="0" value={scores.playerTwoScore} onChange={(e) => setScores({ ...scores, playerTwoScore: e.target.value })} /><button disabled={submitting}>{submitting ? '…' : 'Save'}</button></form>}
    </article>
  );
}

function PracticeHub({ players, practiceSessions, actions }) {
  return (
    <>
      <PageIntro eyebrow="Open-play operations" title="Practice blocks">
        Publish a single evening or a multi-day block, cap attendance, and make sign-up visibility effortless for members.
      </PageIntro>
      <section className="two-column-layout">
        <PracticeForm onSubmit={actions.createPracticeSession} />
        <PracticeRegistration players={players} practiceSessions={practiceSessions} onSubmit={actions.registerPracticePlayer} />
      </section>
      <section className="session-board">
        {practiceSessions.length ? [...practiceSessions].sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt)).map((session) => <PracticeCard key={session.id} session={session} />) : <EmptyState text="No practice blocks yet. Publish the next session for your club." />}
      </section>
    </>
  );
}

function PracticeForm({ onSubmit }) {
  const [form, setForm] = useState({ title: 'Open Practice', description: '', location: '', startsAt: addDays(2, 18), endsAt: addDays(2, 21), registrationDeadline: addDays(2, 17), capacity: '40' });
  const [submitting, setSubmitting] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit({ ...form, startsAt: toIso(form.startsAt), endsAt: toIso(form.endsAt), registrationDeadline: form.registrationDeadline ? toIso(form.registrationDeadline) : null, capacity: Number(form.capacity) });
    if (result) setForm({ ...form, description: '', location: '' });
    setSubmitting(false);
  }
  return (
    <section className="panel form-panel">
      <p className="eyebrow">Publish a session</p><h3>Create practice block</h3>
      <form onSubmit={submit}>
        <label>Title<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
        <label>Description<textarea rows="2" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Open tables, multiball, and coaching." /></label>
        <div className="form-grid"><label>Location<input required value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="Rec Center 201" /></label><label>Capacity<input required type="number" min="1" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} /></label></div>
        <div className="form-grid"><label>Starts<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>Ends<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></label></div>
        <label>Registration closes<input type="datetime-local" value={form.registrationDeadline} onChange={(e) => setForm({ ...form, registrationDeadline: e.target.value })} /></label>
        <button className="button button-primary" disabled={submitting}>{submitting ? 'Publishing…' : 'Publish practice'}</button>
      </form>
    </section>
  );
}

function PracticeRegistration({ players, practiceSessions, onSubmit }) {
  const [sessionId, setSessionId] = useState('');
  const [playerId, setPlayerId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  useEffect(() => { if (!sessionId && practiceSessions.length) setSessionId(String(practiceSessions[0].id)); }, [practiceSessions, sessionId]);
  async function submit(event) {
    event.preventDefault(); setSubmitting(true);
    const result = await onSubmit(Number(sessionId), Number(playerId));
    if (result) setPlayerId('');
    setSubmitting(false);
  }
  return (
    <section className="panel form-panel registration-panel">
      <p className="eyebrow">Member sign-up</p><h3>Register for practice</h3>
      {!practiceSessions.length ? <EmptyState compact text="Publish a practice block first." /> : !players.length ? <EmptyState compact text="Add a club member first." /> : <form onSubmit={submit}>
        <label>Practice block<select value={sessionId} onChange={(e) => setSessionId(e.target.value)}>{practiceSessions.map((session) => <option key={session.id} value={session.id}>{session.title} · {formatDateTime(session.startsAt)}</option>)}</select></label>
        <label>Player<PlayerSelect required players={players} value={playerId} onChange={setPlayerId} placeholder="Choose player" /></label>
        <button className="button button-secondary" disabled={submitting || !playerId}>{submitting ? 'Registering…' : 'Confirm registration'}</button>
      </form>}
    </section>
  );
}

function PracticeCard({ session }) {
  const percentage = Math.min(100, (session.registeredCount / session.capacity) * 100);
  return (
    <article className="practice-card">
      <div className="session-date"><span>{new Date(session.startsAt).toLocaleDateString('en-US', { month: 'short' })}</span><strong>{new Date(session.startsAt).getDate()}</strong></div>
      <div className="session-main"><p className="eyebrow">{session.location}</p><h3>{session.title}</h3><p>{session.description || 'No additional session notes.'}</p><span>{formatRange(session.startsAt, session.endsAt)}</span></div>
      <div className="session-capacity"><strong>{session.registeredCount}<small>/{session.capacity}</small></strong><span>registered</span><div className="progress"><span style={{ width: `${percentage}%` }} /></div>{session.registrationDeadline && <small>Closes {formatDateTime(session.registrationDeadline)}</small>}</div>
    </article>
  );
}

function PlayerSelect({ players, value, onChange, placeholder, required = false }) {
  return <select required={required} value={value} onChange={(event) => onChange(event.target.value)}><option value="">{placeholder}</option>{players.map((player) => <option key={player.id} value={player.id}>{player.displayName} · {player.rating}</option>)}</select>;
}

function EmptyState({ text, compact = false }) {
  return <div className={`empty-state ${compact ? 'is-compact' : ''}`}><span aria-hidden="true">◌</span><p>{text}</p></div>;
}

export default App;
