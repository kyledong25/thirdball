import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, clearAuth, errorMessage, restoreAuth } from './api';
import clubLogo from './assets/tamu-table-tennis-club-logo.webp';
import { addDays, byRating, formatDateTime, formatRange, isRatingEstablished, playerRatingLabel, toDateTimeInput, toIso } from './utils';

const navigation = [
  { id: 'dashboard', label: 'Aggie Overview' },
  { id: 'calendar', label: 'Club Calendar' },
  { id: 'updates', label: 'Club Updates' },
  { id: 'players', label: 'Aggie Ladder' },
  { id: 'tournaments', label: 'Tournaments' },
  { id: 'practice', label: 'Practice Nights' }
];

const memberNavigation = [
  { id: 'overview', label: 'My dashboard', tone: 'dashboard' },
  { id: 'results', label: 'Report results', tone: 'results' },
  { id: 'ladder', label: 'Global ladder', tone: 'players' },
  { id: 'events', label: 'Events', tone: 'practice' },
  { id: 'updates', label: 'Club updates', tone: 'updates' },
  { id: 'calendar', label: 'Club calendar', tone: 'calendar' }
];

const memberSidebarNotes = {
  overview: {
    title: 'Keep your edge.',
    text: 'Update your member details and follow your ladder progress after every official result.'
  },
  results: {
    title: 'Agree, then rate.',
    text: 'Member-submitted results stay pending until the named opponent confirms them.'
  },
  ladder: {
    title: 'Climb the ladder.',
    text: 'Rankings update after confirmed TAMU TTC rated matches are recorded.'
  },
  events: {
    title: 'See you at the table.',
    text: 'Sign up for practices and tournaments so the club can plan every session.'
  },
  updates: {
    title: 'Club pulse.',
    text: 'Read club announcements and send feedback directly to the officer team.'
  },
  calendar: {
    title: 'Plan your next rally.',
    text: 'Use the club calendar to keep practices and tournaments on your schedule.'
  }
};

function App() {
  const [account, setAccount] = useState(null);
  const [checkingAuthentication, setCheckingAuthentication] = useState(true);

  useEffect(() => {
    if (!restoreAuth()) {
      setCheckingAuthentication(false);
      return;
    }
    api.currentUser()
      .then(setAccount)
      .catch(() => clearAuth())
      .finally(() => setCheckingAuthentication(false));
  }, []);

  function signOut() {
    clearAuth();
    setAccount(null);
  }

  if (checkingAuthentication) {
    return <div className="auth-shell"><p className="loading-copy">Checking your club account…</p></div>;
  }
  if (!account) {
    return <AuthenticationGate onAuthenticated={setAccount} />;
  }
  return account.role === 'ADMIN'
    ? <AdminApp account={account} onSignOut={signOut} />
    : <MemberApp account={account} onSignOut={signOut} />;
}

function AdminApp({ account, onSignOut }) {
  const [view, setView] = useState('dashboard');
  const [players, setPlayers] = useState([]);
  const [matches, setMatches] = useState([]);
  const [tournaments, setTournaments] = useState([]);
  const [practiceSessions, setPracticeSessions] = useState([]);
  const [calendarEvents, setCalendarEvents] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const refreshData = useCallback(async () => {
    setLoading(true);
    try {
      const [nextPlayers, nextMatches, nextTournaments, nextSessions, nextCalendarEvents, nextAnnouncements, nextFeedback] = await Promise.all([
        api.listPlayers(),
        api.listMatches(),
        api.listTournaments(),
        api.listPracticeSessions(),
        api.listCalendar(),
        api.listAnnouncements(),
        api.listFeedback()
      ]);
      setPlayers(nextPlayers);
      setMatches(nextMatches);
      setTournaments(nextTournaments);
      setPracticeSessions(nextSessions);
      setCalendarEvents(nextCalendarEvents);
      setAnnouncements(nextAnnouncements);
      setFeedback(nextFeedback);
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
    updatePlayerRating: (playerId, rating) => perform(
      () => api.updatePlayerRating(playerId, rating),
      'Member rating updated. The player is now established.'),
    updateDuesStatus: (playerId, duesPaid) => perform(
      () => api.updateDuesStatus(playerId, duesPaid),
      `Member dues marked ${duesPaid ? 'paid' : 'unpaid'}.`),
    removePlayerFromLadder: (playerId) => perform(
      () => api.removePlayerFromLadder(playerId),
      'Player removed from the active club ladder.'),
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
    generateTournamentBracket: (tournamentId) =>
      perform(() => api.generateTournamentBracket(tournamentId), 'Rating-seeded single-elimination bracket generated.'),
    scheduleTournamentMatch: (payload) => perform(() => api.createMatch(payload), 'Tournament match added to the bracket.'),
    submitTournamentResult: (matchId, scores) =>
      perform(() => api.submitMatchResult(matchId, scores), 'Bracket result recorded and USATT ratings updated.'),
    invalidateMatch: (matchId) => perform(
      () => api.invalidateMatch(matchId),
      'Match result invalidated and the affected ratings were restored.'),
    createPracticeSession: (payload) => perform(() => api.createPracticeSession(payload), 'Practice block published.'),
    registerPracticePlayer: (sessionId, playerId) =>
      perform(() => api.registerForPractice(sessionId, playerId), 'Player registered for practice.'),
    createAnnouncement: (payload) => perform(() => api.createAnnouncement(payload), 'Announcement saved.'),
    updateAnnouncement: (announcementId, payload) =>
      perform(() => api.updateAnnouncement(announcementId, payload), 'Announcement updated.')
  }), [perform, refreshData]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand" aria-label="TAMU Table Tennis Club home">
          <span className="brand-logo">
            <img src={clubLogo} alt="Texas A&M University Table Tennis Club logo" />
          </span>
          <div>
            <p className="eyebrow">Texas A&amp;M University · College Station</p>
            <h1>TAMU Table Tennis</h1>
          </div>
        </div>
        <div className="topbar-actions">
          <span className="role-chip">Administrator · {account.displayName}</span>
          <button className="button button-quiet" onClick={refreshData} disabled={loading}>
            {loading ? 'Syncing…' : 'Refresh'}
          </button>
          <button className="button button-quiet" onClick={onSignOut}>Sign out</button>
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
            <strong>Gig 'em. Then serve.</strong>
            <p>Run practice nights, the Aggie ladder, and every tournament bracket from one club desk.</p>
          </div>
        </aside>

        <main className="content">
          {notice && <Notice notice={notice} onClose={() => setNotice(null)} />}
          {view === 'dashboard' && <Dashboard players={players} tournaments={tournaments} practiceSessions={practiceSessions} onNavigate={setView} />}
          {view === 'calendar' && <CalendarView events={calendarEvents} />}
          {view === 'updates' && <ClubUpdatesHub announcements={announcements} feedback={feedback} actions={actions} />}
          {view === 'players' && <PlayersAndLadder players={players} matches={matches} actions={actions} />}
          {view === 'tournaments' && <TournamentHub players={players} tournaments={tournaments} actions={actions} />}
          {view === 'practice' && <PracticeHub players={players} practiceSessions={practiceSessions} actions={actions} />}
        </main>
      </div>
    </div>
  );
}

function AuthenticationGate({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ displayName: '', email: '', password: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function changeMode(nextMode) {
    setMode(nextMode);
    setError('');
  }

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      if (mode === 'register') {
        await api.registerMember(form);
        const account = await api.login(form.email, form.password);
        onAuthenticated(account);
        return;
      }
      const account = await api.login(form.email, form.password);
      onAuthenticated(account);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  const isLogin = mode === 'login';
  const heading = isLogin ? 'Sign in to your club view' : 'Create your member account';
  const description = isLogin
    ? 'Administrators enter the club operations desk. Members see their upcoming events and personal sign-up actions.'
    : 'New accounts are created as members, linked to your club player record, and signed in immediately.';

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <div className="auth-brand">
          <img src={clubLogo} alt="Texas A&M University Table Tennis Club logo" />
          <div><p className="eyebrow">Texas A&amp;M University</p><h1>TAMU Table Tennis</h1></div>
        </div>
        <p className="eyebrow">{isLogin ? 'Member access' : 'Join the club'}</p>
        <h2>{heading}</h2>
        <p>{description}</p>
        {error && <div className="inline-error">{error}</div>}
        <form onSubmit={submit}>
          {mode === 'register' && <label>Full name<input required maxLength="100" value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>}
          <label>Email<input required type="email" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
          <label>Password<input required type="password" autoComplete={isLogin ? 'current-password' : 'new-password'} maxLength="72" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
          <button className="button button-primary" disabled={submitting}>{submitting ? 'Working…' : isLogin ? 'Sign in' : 'Create member account'}</button>
        </form>
        <div className="auth-secondary-actions">
          {isLogin
            ? <button className="text-button" onClick={() => changeMode('register')}>Need a member account? Register →</button>
            : <button className="text-button" onClick={() => changeMode('login')}>Already have an account? Sign in →</button>}
        </div>
      </section>
    </main>
  );
}

function MemberApp({ account, onSignOut }) {
  const [view, setView] = useState('overview');
  const [practiceSessions, setPracticeSessions] = useState([]);
  const [tournaments, setTournaments] = useState([]);
  const [profile, setProfile] = useState(null);
  const [ratingHistory, setRatingHistory] = useState([]);
  const [ladder, setLadder] = useState([]);
  const [matchResultRequests, setMatchResultRequests] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [calendarEvents, setCalendarEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);
  const [signingUpFor, setSigningUpFor] = useState('');
  const sidebarNote = memberSidebarNotes[view];
  const awaitingYourAgreement = matchResultRequests.filter((request) => request.status === 'PENDING'
    && String(request.opponentId) === String(profile?.id)).length;

  const refreshEvents = useCallback(async () => {
    setLoading(true);
    try {
      const [nextPracticeSessions, nextTournaments, nextProfile, nextRatingHistory, nextLadder, nextMatchResultRequests, nextAnnouncements, nextCalendarEvents] = await Promise.all([
        api.listMemberPracticeSessions(),
        api.listMemberTournaments(),
        api.getMemberProfile(),
        api.getMemberRatingHistory(),
        api.listMemberLadder(),
        api.listMemberMatchResults(),
        api.listMemberAnnouncements(),
        api.listCalendar()
      ]);
      setPracticeSessions(nextPracticeSessions);
      setTournaments(nextTournaments);
      setProfile(nextProfile);
      setRatingHistory(nextRatingHistory);
      setLadder(nextLadder);
      setMatchResultRequests(nextMatchResultRequests);
      setAnnouncements(nextAnnouncements);
      setCalendarEvents(nextCalendarEvents);
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refreshEvents(); }, [refreshEvents]);

  async function signUp(kind, id) {
    const key = `${kind}-${id}`;
    setSigningUpFor(key);
    try {
      if (kind === 'practice') await api.signUpForPractice(id);
      else await api.signUpForTournament(id);
      await refreshEvents();
      setNotice({ type: 'success', text: `You are signed up for the ${kind}.` });
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
    } finally {
      setSigningUpFor('');
    }
  }

  async function updateProfile(payload) {
    try {
      const updatedProfile = await api.updateMemberProfile(payload);
      setProfile(updatedProfile);
      setNotice({ type: 'success', text: 'Your member profile has been updated.' });
      return updatedProfile;
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
      return null;
    }
  }

  async function proposeMatchResult(payload) {
    try {
      const result = await api.proposeMemberMatchResult(payload);
      await refreshEvents();
      setNotice({ type: 'success', text: 'Result sent to your opponent for confirmation.' });
      return result;
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
      return null;
    }
  }

  async function respondToMatchResult(proposalId, response) {
    try {
      const result = response === 'agree'
        ? await api.agreeMemberMatchResult(proposalId)
        : await api.declineMemberMatchResult(proposalId);
      await refreshEvents();
      setNotice({ type: 'success', text: response === 'agree'
        ? 'Result agreed. The official match and rating update are now recorded.'
        : 'Result request declined. No rating changes were made.' });
      return result;
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
      return null;
    }
  }

  async function submitFeedback(payload) {
    try {
      const result = await api.submitFeedback(payload);
      setNotice({ type: 'success', text: 'Thanks—your feedback was sent to the club officers.' });
      return result;
    } catch (error) {
      setNotice({ type: 'error', text: errorMessage(error) });
      return null;
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand" aria-label="TAMU Table Tennis Club home">
          <span className="brand-logo"><img src={clubLogo} alt="Texas A&M University Table Tennis Club logo" /></span>
          <div><p className="eyebrow">Texas A&amp;M University · College Station</p><h1>TAMU Table Tennis</h1></div>
        </div>
        <div className="topbar-actions">
          <span className="role-chip">Member · {account.displayName}</span>
          <button className="button button-quiet" onClick={refreshEvents} disabled={loading}>{loading ? 'Syncing…' : 'Refresh'}</button>
          <button className="button button-quiet" onClick={onSignOut}>Sign out</button>
        </div>
      </header>
      <div className="workspace">
        <aside className="sidebar" aria-label="Member navigation">
          <nav>
            {memberNavigation.map((item) => (
              <button className={`nav-item ${view === item.id ? 'is-active' : ''}`} key={item.id} onClick={() => setView(item.id)}>
                <span className={`nav-dot nav-dot-${item.tone}`} aria-hidden="true" />
                {item.label}{item.id === 'results' && awaitingYourAgreement > 0 ? ` (${awaitingYourAgreement})` : ''}
              </button>
            ))}
          </nav>
          <div className="sidebar-note">
            <strong>{sidebarNote.title}</strong>
            <p>{sidebarNote.text}</p>
          </div>
        </aside>
        <main className="member-content">
          {notice && <Notice notice={notice} onClose={() => setNotice(null)} />}
          {view === 'overview' && <>
            <PageIntro eyebrow="Member dashboard" title="Your next time at the table.">
              Keep your member details current, track your rating progress, and use the navigation to report ladder results.
            </PageIntro>
            <section className="member-insights">
              <MemberProfile profile={profile} onSubmit={updateProfile} />
              <RatingHistoryChart profile={profile} points={ratingHistory} />
            </section>
          </>}
          {view === 'results' && <>
            <PageIntro eyebrow="Member ladder match" title="Report and confirm results.">
              Send a score to your opponent. It becomes an official TAMU TTC rated match only after they agree.
            </PageIntro>
            <MemberMatchResultDesk profile={profile} players={ladder} requests={matchResultRequests} onPropose={proposeMatchResult} onRespond={respondToMatchResult} />
          </>}
          {view === 'ladder' && <>
            <PageIntro eyebrow="Club competition" title="Global ladder">
              View current club rankings. Administrators manage dues and ladder membership.
            </PageIntro>
            <MemberLadder players={ladder} currentPlayerId={profile?.id} />
          </>}
          {view === 'events' && <>
            <PageIntro eyebrow="Member event desk" title="Upcoming club events.">
              Browse practices and tournaments, then sign yourself up with one click.
            </PageIntro>
            <section className="member-events">
              <EventSchedule title="Upcoming practices" events={practiceSessions} kind="practice" signingUpFor={signingUpFor} onSignUp={signUp} />
              <EventSchedule title="Upcoming tournaments" events={tournaments} kind="tournament" signingUpFor={signingUpFor} onSignUp={signUp} />
            </section>
          </>}
          {view === 'updates' && <>
            <PageIntro eyebrow="Club communication" title="Club updates and feedback">
              Stay current on announcements, then send questions or ideas directly to the officer team.
            </PageIntro>
            <MemberClubUpdates announcements={announcements} onSubmitFeedback={submitFeedback} />
          </>}
          {view === 'calendar' && <>
            <PageIntro eyebrow="Club schedule" title="Global calendar">
              See every upcoming practice and tournament in one place.
            </PageIntro>
            <CalendarView events={calendarEvents} compact />
          </>}
        </main>
      </div>
    </div>
  );
}

function ClubUpdatesHub({ announcements, feedback, actions }) {
  return (
    <>
      <PageIntro eyebrow="Club communication" title="Announcements and member feedback">
        Publish club updates, keep drafts private, and review the feedback members send from their dashboard.
      </PageIntro>
      <section className="admin-updates-workspace">
        <AnnouncementManager announcements={announcements} onCreate={actions.createAnnouncement} onUpdate={actions.updateAnnouncement} />
        <FeedbackInbox feedback={feedback} />
      </section>
    </>
  );
}

function MemberClubUpdates({ announcements, onSubmitFeedback }) {
  return (
    <section className="member-updates-workspace">
      <AnnouncementFeed announcements={announcements} />
      <FeedbackForm onSubmit={onSubmitFeedback} />
    </section>
  );
}

function AnnouncementFeed({ announcements }) {
  return (
    <section className="panel announcement-feed">
      <div className="panel-heading"><div><p className="eyebrow">From the officer team</p><h3>Club announcements</h3></div><span className="count-chip">{announcements.length} live</span></div>
      {announcements.length ? <div className="announcement-list">{announcements.map((announcement) => (
        <article className="announcement-item" key={announcement.id}>
          <p className="eyebrow">{formatDateTime(announcement.publishedAt || announcement.createdAt)}</p>
          <h4>{announcement.title}</h4>
          <p>{announcement.body}</p>
        </article>
      ))}</div> : <EmptyState compact text="The officer team has not posted any announcements yet." />}
    </section>
  );
}

function FeedbackForm({ onSubmit }) {
  const [form, setForm] = useState({ subject: '', message: '' });
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit(form);
    if (result) setForm({ subject: '', message: '' });
    setSubmitting(false);
  }

  return (
    <section className="panel feedback-form">
      <div className="panel-heading"><div><p className="eyebrow">Member voice</p><h3>Send feedback</h3></div></div>
      <p className="form-hint">Questions, ideas, and concerns are shared with club administrators along with your club name.</p>
      <form onSubmit={submit}>
        <label>Subject<input required maxLength="150" value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} placeholder="Practice suggestion" /></label>
        <label>Message<textarea required maxLength="4000" rows="6" value={form.message} onChange={(event) => setForm({ ...form, message: event.target.value })} placeholder="What would make the club better?" /></label>
        <button className="button button-primary" disabled={submitting}>{submitting ? 'Sending…' : 'Send feedback'}</button>
      </form>
    </section>
  );
}

function AnnouncementManager({ announcements, onCreate, onUpdate }) {
  const [form, setForm] = useState({ title: '', body: '', published: true });
  const [creating, setCreating] = useState(false);

  async function create(event) {
    event.preventDefault();
    setCreating(true);
    const result = await onCreate(form);
    if (result) setForm({ title: '', body: '', published: true });
    setCreating(false);
  }

  return (
    <section className="panel announcement-manager">
      <div className="panel-heading"><div><p className="eyebrow">Administrator communication</p><h3>Announcements</h3></div><span className="count-chip">{announcements.length} total</span></div>
      <form className="announcement-create-form" onSubmit={create}>
        <label>Title<input required maxLength="150" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="Thursday practice reminder" /></label>
        <label>Message<textarea required maxLength="4000" rows="4" value={form.body} onChange={(event) => setForm({ ...form, body: event.target.value })} placeholder="Share a club update with members." /></label>
        <label className="checkbox-label"><input type="checkbox" checked={form.published} onChange={(event) => setForm({ ...form, published: event.target.checked })} />Publish for members now</label>
        <button className="button button-primary" disabled={creating}>{creating ? 'Saving…' : 'Create announcement'}</button>
      </form>
      <div className="announcement-admin-list">
        {announcements.map((announcement) => <AnnouncementEditor announcement={announcement} onUpdate={onUpdate} key={announcement.id} />)}
      </div>
    </section>
  );
}

function AnnouncementEditor({ announcement, onUpdate }) {
  const [draft, setDraft] = useState({ title: announcement.title, body: announcement.body, published: announcement.published });
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDraft({ title: announcement.title, body: announcement.body, published: announcement.published });
  }, [announcement]);

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    const result = await onUpdate(announcement.id, draft);
    if (result) setEditing(false);
    setSaving(false);
  }

  return <article className={`announcement-admin-item ${announcement.published ? 'is-published' : 'is-draft'}`}>
    {editing ? <form onSubmit={save}>
      <label>Title<input required maxLength="150" value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label>
      <label>Message<textarea required maxLength="4000" rows="4" value={draft.body} onChange={(event) => setDraft({ ...draft, body: event.target.value })} /></label>
      <label className="checkbox-label"><input type="checkbox" checked={draft.published} onChange={(event) => setDraft({ ...draft, published: event.target.checked })} />Published for members</label>
      <div className="inline-actions"><button className="button button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button><button type="button" className="text-button" onClick={() => setEditing(false)}>Cancel</button></div>
    </form> : <>
      <div><p className="eyebrow">{announcement.published ? 'Published' : 'Draft'} · {formatDateTime(announcement.publishedAt || announcement.createdAt)}</p><h4>{announcement.title}</h4><p>{announcement.body}</p></div>
      <button className="text-button" onClick={() => setEditing(true)}>Edit announcement</button>
    </>}
  </article>;
}

function FeedbackInbox({ feedback }) {
  return (
    <section className="panel feedback-inbox">
      <div className="panel-heading"><div><p className="eyebrow">Member voice</p><h3>Feedback inbox</h3></div><span className="count-chip">{feedback.length} received</span></div>
      {feedback.length ? <div className="feedback-list">{feedback.map((item) => <article className="feedback-item" key={item.id}>
        <div><p className="eyebrow">{item.playerName} · {formatDateTime(item.submittedAt)}</p><h4>{item.subject}</h4><p>{item.message}</p></div>
      </article>)}</div> : <EmptyState compact text="Member feedback will appear here when it is submitted." />}
    </section>
  );
}

function MemberMatchResultDesk({ profile, players, requests, onPropose, onRespond }) {
  const [form, setForm] = useState({ opponentId: '', reporterScore: '3', opponentScore: '0' });
  const [proposing, setProposing] = useState(false);
  const [respondingId, setRespondingId] = useState('');
  const currentPlayerId = profile?.id;
  const opponents = players.filter((player) => String(player.id) !== String(currentPlayerId));
  const incoming = requests.filter((request) => request.status === 'PENDING'
    && String(request.opponentId) === String(currentPlayerId));

  async function submit(event) {
    event.preventDefault();
    setProposing(true);
    const result = await onPropose({
      opponentId: Number(form.opponentId),
      reporterScore: Number(form.reporterScore),
      opponentScore: Number(form.opponentScore)
    });
    if (result) setForm({ opponentId: '', reporterScore: '3', opponentScore: '0' });
    setProposing(false);
  }

  async function respond(proposalId, response) {
    setRespondingId(`${proposalId}-${response}`);
    await onRespond(proposalId, response);
    setRespondingId('');
  }

  return (
    <section className="member-match-workspace">
      <section className="panel member-result-form">
        <div className="panel-heading"><div><p className="eyebrow">Member ladder match</p><h3>Report a result</h3></div></div>
        <p className="form-hint">Your opponent must agree before a match becomes official and changes either rating.</p>
        {profile && opponents.length ? <form onSubmit={submit}>
          <label>Opponent<PlayerSelect required players={opponents} value={form.opponentId} onChange={(opponentId) => setForm({ ...form, opponentId })} placeholder="Choose opponent" /></label>
          <div className="score-inputs">
            <label>Your games<input required type="number" min="0" value={form.reporterScore} onChange={(event) => setForm({ ...form, reporterScore: event.target.value })} /></label>
            <label>Opponent games<input required type="number" min="0" value={form.opponentScore} onChange={(event) => setForm({ ...form, opponentScore: event.target.value })} /></label>
          </div>
          <button className="button button-primary" disabled={proposing || !form.opponentId}>{proposing ? 'Sending…' : 'Send for agreement'}</button>
        </form> : <EmptyState compact text="At least one other active ladder member is needed to report a result." />}
      </section>
      <section className="panel member-result-requests">
        <div className="panel-heading"><div><p className="eyebrow">Result confirmation</p><h3>Match requests</h3></div><span className="count-chip">{incoming.length} need you</span></div>
        <p className="form-hint">Only you can answer a result submitted against your player record.</p>
        {requests.length ? <div className="member-result-list">{requests.map((request) => {
          const needsYourAgreement = request.status === 'PENDING' && String(request.opponentId) === String(currentPlayerId);
          const youReported = String(request.reporterId) === String(currentPlayerId);
          const statusText = request.status === 'PENDING'
            ? (needsYourAgreement ? 'Your agreement needed' : `Waiting for ${request.opponentName}`)
            : request.status === 'AGREED' ? 'Official match recorded' : 'Request declined';
          return <article className={`member-result-request is-${request.status.toLowerCase()}`} key={request.id}>
            <div>
              <p className="eyebrow">{statusText}</p>
              <h4>{request.reporterName} <span>{request.reporterScore}–{request.opponentScore}</span> {request.opponentName}</h4>
              <small>{youReported ? 'You sent this result' : `Sent ${formatDateTime(request.proposedAt)}`}{request.officialMatchId && ` · Official match #${request.officialMatchId}`}</small>
            </div>
            {needsYourAgreement && <div className="result-request-actions">
              <button className="button button-primary" disabled={Boolean(respondingId)} onClick={() => respond(request.id, 'agree')}>{respondingId === `${request.id}-agree` ? 'Agreeing…' : 'Agree'}</button>
              <button className="button button-secondary" disabled={Boolean(respondingId)} onClick={() => respond(request.id, 'decline')}>{respondingId === `${request.id}-decline` ? 'Declining…' : 'Decline'}</button>
            </div>}
          </article>;
        })}</div> : <EmptyState compact text="Submitted member results will appear here for review." />}
      </section>
    </section>
  );
}

function MemberLadder({ players, currentPlayerId }) {
  const rankedPlayers = byRating(players);

  return (
    <section className="panel member-ladder-panel">
      <div className="panel-heading">
        <div><p className="eyebrow">Club competition</p><h3>Global ladder</h3></div>
        <span className="count-chip">{rankedPlayers.length} active</span>
      </div>
      <p className="form-hint">Rankings are read-only. Club administrators manage dues and ladder membership.</p>
      {rankedPlayers.length ? <ol className="member-ladder-list">{rankedPlayers.map((player, index) => {
        const isCurrentMember = String(player.id) === String(currentPlayerId);
        return <li className={isCurrentMember ? 'is-current-member' : ''} key={player.id}>
          <span className={`rank ${index < 3 ? `rank-${index + 1}` : ''}`}>{index + 1}</span>
          <span className="avatar" aria-hidden="true">{player.displayName.slice(0, 1).toUpperCase()}</span>
          <span className="member-ladder-name"><strong>{player.displayName}</strong>{isCurrentMember && <small>Your position</small>}</span>
          <span className="member-ladder-rating">{playerRatingLabel(player)}</span>
        </li>;
      })}</ol> : <EmptyState compact text="The club ladder will appear as members are added." />}
    </section>
  );
}

function MemberProfile({ profile, onSubmit }) {
  const [form, setForm] = useState({ graduationYear: '', skillLevel: '', phone: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (profile) {
      setForm({
        graduationYear: profile.graduationYear ?? '',
        skillLevel: profile.skillLevel ?? '',
        phone: profile.phone ?? ''
      });
    }
  }, [profile]);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    await onSubmit({
      graduationYear: form.graduationYear === '' ? null : Number(form.graduationYear),
      skillLevel: form.skillLevel || null,
      phone: form.phone.trim() || null
    });
    setSubmitting(false);
  }

  return (
    <section className="panel member-profile-panel">
      <div className="panel-heading">
        <div><p className="eyebrow">Your club profile</p><h3>Member details</h3></div>
        {profile && <span className={`status-pill ${profile.duesPaid ? 'status-active' : 'status-unpaid'}`}>{profile.duesPaid ? 'Dues paid' : 'Dues unpaid'}</span>}
      </div>
      <p className="form-hint">Dues status is managed by club administrators. Keep your class year, experience, and contact number current.</p>
      <form onSubmit={submit}>
        <div className="form-grid">
          <label>Graduation year<input type="number" min="2000" max="2100" value={form.graduationYear} onChange={(event) => setForm({ ...form, graduationYear: event.target.value })} placeholder="2028" /></label>
          <label>Skill level<select value={form.skillLevel} onChange={(event) => setForm({ ...form, skillLevel: event.target.value })}><option value="">Choose level</option><option>Beginner</option><option>Intermediate</option><option>Advanced</option><option>Competitive</option></select></label>
        </div>
        <label>Phone<input type="tel" maxLength="30" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} placeholder="(979) 555-0123" /></label>
        <button className="button button-secondary" disabled={submitting}>{submitting ? 'Saving…' : 'Save profile'}</button>
      </form>
    </section>
  );
}

function RatingHistoryChart({ profile, points }) {
  const series = useMemo(() => points.filter((point) => Number.isFinite(point.rating)), [points]);
  const currentRating = profile && isRatingEstablished(profile) ? profile.rating : null;
  const chart = useMemo(() => {
    if (!series.length) return null;
    const width = 560;
    const height = 190;
    const padding = { top: 20, right: 20, bottom: 31, left: 42 };
    const ratings = series.map((point) => point.rating);
    const low = Math.min(...ratings);
    const high = Math.max(...ratings);
    const range = Math.max(20, high - low);
    const minimum = low - Math.ceil(range * 0.2);
    const maximum = high + Math.ceil(range * 0.2);
    const innerWidth = width - padding.left - padding.right;
    const innerHeight = height - padding.top - padding.bottom;
    const coordinates = series.map((point, index) => ({
      ...point,
      x: padding.left + (series.length === 1 ? innerWidth / 2 : (index / (series.length - 1)) * innerWidth),
      y: padding.top + ((maximum - point.rating) / (maximum - minimum)) * innerHeight
    }));
    return { width, height, padding, minimum, maximum, coordinates };
  }, [series]);

  return (
    <section className="panel rating-history-panel">
      <div className="panel-heading">
        <div><p className="eyebrow">Member rating history</p><h3>My ladder progress</h3></div>
        <span className="rating-current">{currentRating ?? 'Unrated'}</span>
      </div>
      {chart ? <>
        <svg className="rating-chart" viewBox={`0 0 ${chart.width} ${chart.height}`} role="img" aria-label="Your rating history line chart">
          {[0, 0.5, 1].map((position) => {
            const y = chart.padding.top + position * (chart.height - chart.padding.top - chart.padding.bottom);
            const rating = Math.round(chart.maximum - position * (chart.maximum - chart.minimum));
            return <g key={position}><line x1={chart.padding.left} x2={chart.width - chart.padding.right} y1={y} y2={y} className="rating-grid-line" /><text x="0" y={y + 4} className="rating-axis-label">{rating}</text></g>;
          })}
          {chart.coordinates.length > 1 && <polyline points={chart.coordinates.map((point) => `${point.x},${point.y}`).join(' ')} className="rating-chart-line" />}
          {chart.coordinates.map((point, index) => <g key={`${point.matchId}-${index}`}><circle cx={point.x} cy={point.y} r="4.5" className={point.baseline ? 'rating-chart-dot is-baseline' : point.won ? 'rating-chart-dot is-win' : 'rating-chart-dot is-loss'} /><title>{point.baseline ? `Started at ${point.rating}` : `${point.won ? 'Won' : 'Lost'} vs ${point.opponentName}: ${point.rating}`}</title></g>)}
          <text x={chart.padding.left} y={chart.height - 7} className="rating-axis-label">{formatDateTime(series[0].occurredAt).split(',')[0]}</text>
          {series.length > 1 && <text x={chart.width - chart.padding.right} y={chart.height - 7} textAnchor="end" className="rating-axis-label">{formatDateTime(series[series.length - 1].occurredAt).split(',')[0]}</text>}
        </svg>
        <div className="rating-history-key"><span><i className="is-win" />Win</span><span><i className="is-loss" />Loss</span><span><i className="is-baseline" />Starting point</span></div>
      </> : <div className="rating-history-empty"><strong>{currentRating ?? 'Unrated'}</strong><p>Completed rated matches will build your progress chart here.</p></div>}
    </section>
  );
}

function CalendarView({ events, compact = false }) {
  return (
    <section className={`panel calendar-panel ${compact ? 'is-compact' : ''}`}>
      <div className="panel-heading">
        <div><p className="eyebrow">Club-wide schedule</p><h3>Global calendar</h3></div>
        <span className="count-chip">{events.length} upcoming</span>
      </div>
      {events.length ? <div className="calendar-list">{events.map((event) => (
        <article className="calendar-event" key={`${event.type}-${event.id}`}>
          <div className="calendar-date"><span>{new Date(event.startsAt).toLocaleDateString('en-US', { month: 'short' })}</span><strong>{new Date(event.startsAt).getDate()}</strong></div>
          <div><span className={`event-type event-type-${event.type.toLowerCase()}`}>{event.type === 'PRACTICE' ? 'Practice' : 'Tournament'}</span><h4>{event.title}</h4><p>{formatRange(event.startsAt, event.endsAt)} · {event.location || 'Location TBA'}</p>{event.description && <small>{event.description}</small>}</div>
        </article>
      ))}</div> : <EmptyState compact text="No upcoming club events have been posted." />}
    </section>
  );
}

function EventSchedule({ title, events, kind, signingUpFor, onSignUp }) {
  return (
    <section className="panel member-event-panel">
      <div className="panel-heading"><div><p className="eyebrow">{kind === 'practice' ? 'Practice schedule' : 'Tournament calendar'}</p><h3>{title}</h3></div><span className="count-chip">{events.length} upcoming</span></div>
      {events.length ? <div className="member-event-list">{events.map((event) => {
        const eventName = kind === 'practice' ? event.title : event.name;
        const capacity = kind === 'practice' ? event.capacity : event.maxParticipants;
        const signUpKey = `${kind}-${event.id}`;
        return <article className="member-event" key={event.id}>
          <div><p className="eyebrow">{event.location || 'Location TBA'}</p><h4>{eventName}</h4><p>{formatRange(event.startsAt, event.endsAt)}</p><small>{event.registeredCount}/{capacity} registered</small></div>
          <button className="button button-primary" disabled={signingUpFor === signUpKey} onClick={() => onSignUp(kind, event.id)}>{signingUpFor === signUpKey ? 'Signing up…' : 'Sign up'}</button>
        </article>;
      })}</div> : <EmptyState compact text={`No upcoming ${kind === 'practice' ? 'practices' : 'tournaments'} have been posted.`} />}
    </section>
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
      <PageIntro eyebrow="TAMU Table Tennis Club" title="The Aggie rally desk.">
        Keep practices, the club ladder, and tournament play moving from one maroon-and-white home base.
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
              <p className="eyebrow">Aggie USATT ladder</p>
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
                  <span>{playerRatingLabel(player)}</span>
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

function PlayersAndLadder({ players, matches, actions }) {
  const orderedPlayers = byRating(players);
  return (
    <>
      <PageIntro eyebrow="Membership & competition" title="Players and the Aggie ladder">
        New members begin unrated. After five matches against rated players, their provisional USATT rating is set; every later match uses the fixed 0–50 point exchange chart.
      </PageIntro>
      <section className="two-column-layout">
        <PlayerForm onSubmit={actions.createPlayer} />
        <LadderMatchForm players={orderedPlayers} onSubmit={actions.recordLadderMatch} />
      </section>
      <AdminRatingEditor players={orderedPlayers} onSubmit={actions.updatePlayerRating} />
      <section className="panel table-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Live internal ranking</p>
            <h3>Club ladder</h3>
          </div>
          <span className="count-chip">{players.length} players</span>
        </div>
        {orderedPlayers.length ? <LadderTable players={orderedPlayers} onRemove={actions.removePlayerFromLadder} onUpdateDues={actions.updateDuesStatus} /> : <EmptyState text="Your club roster will appear here after you add the first player." />}
      </section>
      <MatchResultReview matches={matches} onInvalidate={actions.invalidateMatch} />
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
        <label>Texas A&amp;M email<input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="jordan@tamu.edu" /></label>
        <button className="button button-primary" disabled={submitting}>{submitting ? 'Adding…' : 'Add unrated player'}</button>
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
  const hasProvisionalPlayer = result.playerOneRatingBefore == null || result.playerTwoRatingBefore == null;
  const provisionalFinalized = (result.playerOneRatingBefore == null && result.playerOneRatingAfter != null)
    || (result.playerTwoRatingBefore == null && result.playerTwoRatingAfter != null);

  return (
    <div className="rating-result">
      <strong>{result.winnerId === result.playerOneId ? result.playerOneName : result.playerTwoName} wins</strong>
      <div>
        <span>{result.playerOneName}: {result.playerOneRatingBefore ?? 'Unrated'} → <b>{result.playerOneRatingAfter ?? 'Unrated'}</b></span>
        <span>{result.playerTwoName}: {result.playerTwoRatingBefore ?? 'Unrated'} → <b>{result.playerTwoRatingAfter ?? 'Unrated'}</b></span>
      </div>
      {hasProvisionalPlayer && <p>{provisionalFinalized
        ? 'Provisional rating finalized. Future matches now use the USATT exchange chart.'
        : 'Provisional result recorded. A starting rating is calculated after five matches against rated players.'}</p>}
    </div>
  );
}

function AdminRatingEditor({ players, onSubmit }) {
  const [playerId, setPlayerId] = useState('');
  const [rating, setRating] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function selectPlayer(nextPlayerId) {
    setPlayerId(nextPlayerId);
    const player = players.find((candidate) => String(candidate.id) === String(nextPlayerId));
    setRating(player ? String(player.rating) : '');
  }

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmit(Number(playerId), Number(rating));
    if (result) {
      setPlayerId(String(result.id));
      setRating(String(result.rating));
    }
    setSubmitting(false);
  }

  return (
    <section className="panel form-panel admin-rating-editor">
      <p className="eyebrow">Administrator action</p>
      <h3>Correct a member rating</h3>
      <form onSubmit={submit}>
        <div className="form-grid">
          <label>Member<PlayerSelect required players={players} value={playerId} onChange={selectPlayer} placeholder="Choose member" /></label>
          <label>New rating<input required type="number" value={rating} onChange={(event) => setRating(event.target.value)} /></label>
        </div>
        <p className="form-hint">This direct correction marks an unrated member as established.</p>
        <button className="button button-secondary" disabled={submitting || !playerId || rating === ''}>{submitting ? 'Saving…' : 'Save rating'}</button>
      </form>
    </section>
  );
}

function LadderTable({ players, onRemove, onUpdateDues }) {
  const [removingPlayerId, setRemovingPlayerId] = useState('');
  const [updatingDuesId, setUpdatingDuesId] = useState('');

  async function removePlayer(player) {
    if (!window.confirm(`Remove ${player.displayName} from the active club ladder? Their match history will be retained.`)) return;
    setRemovingPlayerId(String(player.id));
    await onRemove(player.id);
    setRemovingPlayerId('');
  }

  async function updateDues(player) {
    setUpdatingDuesId(String(player.id));
    await onUpdateDues(player.id, !player.duesPaid);
    setUpdatingDuesId('');
  }

  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Rank</th><th>Player</th><th>Profile</th><th>Email</th><th className="right">USATT rating</th><th>Dues</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
          {players.map((player, index) => (
            <tr key={player.id}>
              <td><span className="rank rank-table">{index + 1}</span></td>
              <td><div className="player-cell"><span className="avatar">{player.displayName.slice(0, 1).toUpperCase()}</span><strong>{player.displayName}</strong></div></td>
              <td><span className="profile-summary">{player.graduationYear ? `Class of ${player.graduationYear}` : 'Class year —'}<small>{player.skillLevel || 'Skill level —'}</small></span></td>
              <td>{player.email}</td>
              <td className="right rating-number">
                {isRatingEstablished(player) ? player.rating : 'Unrated'}
                {!isRatingEstablished(player) && <small>{player.provisionalMatchCount || 0}/5 matches</small>}
              </td>
              <td><button className={`dues-button ${player.duesPaid ? 'is-paid' : 'is-unpaid'}`} disabled={updatingDuesId === String(player.id)} onClick={() => updateDues(player)}>{updatingDuesId === String(player.id) ? 'Saving…' : player.duesPaid ? 'Paid' : 'Unpaid'}</button></td>
              <td><span className={`status-pill ${player.active ? 'status-active' : 'status-muted'}`}>{player.active ? 'Active' : 'Inactive'}</span></td>
              <td><button className="text-button danger-button" disabled={removingPlayerId === String(player.id)} onClick={() => removePlayer(player)}>{removingPlayerId === String(player.id) ? 'Removing…' : 'Remove'}</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MatchResultReview({ matches, onInvalidate }) {
  const [invalidatingMatchId, setInvalidatingMatchId] = useState('');
  const reviewedMatches = matches.filter((match) => match.status === 'COMPLETED' || match.status === 'CANCELLED');

  async function invalidate(match) {
    if (!window.confirm(`Invalidate the ${match.playerOneName} vs ${match.playerTwoName} result? This restores the ratings from before the match.`)) return;
    setInvalidatingMatchId(String(match.id));
    await onInvalidate(match.id);
    setInvalidatingMatchId('');
  }

  return (
    <section className="panel match-review-panel">
      <div className="panel-heading">
        <div><p className="eyebrow">Administrator action</p><h3>Match result review</h3></div>
        <span className="count-chip">{reviewedMatches.length} recorded</span>
      </div>
      <p className="form-hint">Invalidate the newest result for its players to restore their pre-match ratings. Older results stay protected until newer results are addressed.</p>
      {reviewedMatches.length ? <div className="match-review-list">{reviewedMatches.map((match) => {
        const winnerName = match.winnerId === match.playerOneId ? match.playerOneName : match.playerTwoName;
        const isInvalidated = match.status === 'CANCELLED';
        return <article className={`match-review-item ${isInvalidated ? 'is-invalidated' : ''}`} key={match.id}>
          <div><strong>{match.playerOneName} <span>{match.playerOneScore}–{match.playerTwoScore}</span> {match.playerTwoName}</strong><small>{isInvalidated ? 'Invalidated result' : `${winnerName} won`} · {formatDateTime(match.completedAt)}</small></div>
          {isInvalidated
            ? <span className="status-pill status-muted">Invalidated</span>
            : <button className="text-button danger-button" disabled={invalidatingMatchId === String(match.id)} onClick={() => invalidate(match)}>{invalidatingMatchId === String(match.id) ? 'Invalidating…' : 'Invalidate result'}</button>}
        </article>;
      })}</div> : <EmptyState compact text="Completed ladder and tournament results will appear here for administrator review." />}
    </section>
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
        Open registration, confirm the field, then generate a rating-seeded single-elimination bracket. Winners automatically advance as results are entered.
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
  const [generating, setGenerating] = useState(false);

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

  async function generateBracket() {
    if (!window.confirm(`Generate a rating-seeded bracket for ${tournament.name}? Registration will close and the field will be locked.`)) return;
    setGenerating(true);
    const result = await actions.generateTournamentBracket(tournament.id);
    if (result) setRefreshIndex((value) => value + 1);
    setGenerating(false);
  }

  async function submitResult(matchId, scores) {
    const result = await actions.submitTournamentResult(matchId, scores);
    if (result) setRefreshIndex((value) => value + 1);
    return result;
  }

  async function invalidateResult(matchId) {
    const result = await actions.invalidateMatch(matchId);
    if (result) setRefreshIndex((value) => value + 1);
    return result;
  }

  return (
    <div className="bracket-workspace">
      {tournament.status === 'REGISTRATION_OPEN' && <BracketGenerator tournament={tournament} generating={generating} onGenerate={generateBracket} />}
      {tournament.status === 'REGISTRATION_OPEN' && <details className="manual-bracket-details"><summary>Need a manual bracket match instead?</summary><BracketMatchForm players={players} onSubmit={schedule} /></details>}
      {loading ? <p className="loading-copy">Loading bracket…</p> : error ? <div className="inline-error">{error}</div> : Object.keys(rounds).length ? (
        <div className="bracket-scroll">
          <div className="bracket-grid">
            {Object.entries(rounds).map(([roundName, roundMatches]) => (
              <section className="bracket-round" key={roundName}>
                <h4>{roundName}</h4>
                <div className="match-stack">
                  {roundMatches.map((match) => <BracketMatch key={match.id} match={match} onSubmitResult={submitResult} onInvalidateResult={invalidateResult} />)}
                </div>
              </section>
            ))}
          </div>
        </div>
      ) : <EmptyState text={tournament.status === 'REGISTRATION_OPEN' ? 'Register at least two players, then generate the rating-seeded bracket.' : 'The generated bracket is being prepared.'} />}
    </div>
  );
}

function BracketGenerator({ tournament, generating, onGenerate }) {
  const canGenerate = tournament.registeredCount >= 2;
  return (
    <section className="bracket-generator">
      <div><strong>Generate rating-seeded bracket</strong><p>Seeds use established USATT ratings first; unranked members follow in registration order. Opening byes advance automatically.</p></div>
      <button className="button button-primary" disabled={!canGenerate || generating} onClick={onGenerate}>{generating ? 'Generating…' : 'Generate bracket'}</button>
      {!canGenerate && <small>Register at least two active players to generate a bracket.</small>}
    </section>
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

function BracketMatch({ match, onSubmitResult, onInvalidateResult }) {
  const [scores, setScores] = useState({ playerOneScore: '', playerTwoScore: '' });
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [invalidating, setInvalidating] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    const result = await onSubmitResult(match.id, { playerOneScore: Number(scores.playerOneScore), playerTwoScore: Number(scores.playerTwoScore) });
    if (result) setEditing(false);
    setSubmitting(false);
  }
  async function invalidate() {
    if (!window.confirm(`Invalidate the ${match.playerOneName} vs ${match.playerTwoName} result?`)) return;
    setInvalidating(true);
    await onInvalidateResult(match.id);
    setInvalidating(false);
  }
  const readyForResult = match.status === 'SCHEDULED' && match.playerOneId && match.playerTwoId;
  return (
    <article className={`bracket-match ${match.status === 'COMPLETED' ? 'is-complete' : ''} ${match.status === 'CANCELLED' ? 'is-cancelled' : ''} ${match.status === 'BYE' ? 'is-bye' : ''}`}>
      <div className={`bracket-player ${match.winnerId === match.playerOneId ? 'is-winner' : ''}`}><span>{match.playerOneName || 'Awaiting player'}</span><strong>{match.playerOneScore ?? '—'}</strong></div>
      <div className={`bracket-player ${match.winnerId === match.playerTwoId ? 'is-winner' : ''}`}><span>{match.playerTwoName || 'Awaiting player'}</span><strong>{match.playerTwoScore ?? '—'}</strong></div>
      {readyForResult && !editing && <button className="text-button result-button" onClick={() => setEditing(true)}>Enter result</button>}
      {editing && <form className="bracket-score-form" onSubmit={submit}><input required type="number" min="0" value={scores.playerOneScore} onChange={(e) => setScores({ ...scores, playerOneScore: e.target.value })} /><span>–</span><input required type="number" min="0" value={scores.playerTwoScore} onChange={(e) => setScores({ ...scores, playerTwoScore: e.target.value })} /><button disabled={submitting}>{submitting ? '…' : 'Save'}</button></form>}
      {match.status === 'COMPLETED' && <button className="text-button result-button danger-button" disabled={invalidating} onClick={invalidate}>{invalidating ? 'Invalidating…' : 'Invalidate result'}</button>}
      {match.status === 'CANCELLED' && <span className="status-pill status-muted bracket-status">Invalidated</span>}
      {match.status === 'BYE' && <span className="status-pill status-bye bracket-status">Bye advanced</span>}
      {match.status === 'SCHEDULED' && !readyForResult && <span className="bracket-awaiting">Awaiting feeder result</span>}
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
  return <select required={required} value={value} onChange={(event) => onChange(event.target.value)}><option value="">{placeholder}</option>{players.map((player) => <option key={player.id} value={player.id}>{player.displayName} · {playerRatingLabel(player)}</option>)}</select>;
}

function EmptyState({ text, compact = false }) {
  return <div className={`empty-state ${compact ? 'is-compact' : ''}`}><span aria-hidden="true">◌</span><p>{text}</p></div>;
}

export default App;
