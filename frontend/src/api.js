import axios from 'axios';

const client = axios.create({
  // Use the Vite proxy locally; set VITE_API_BASE_URL to the deployed API later.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' }
});

const AUTH_STORAGE_KEY = 'thirdball.basic-auth';

function encodeBasicCredentials(email, password) {
  const bytes = new TextEncoder().encode(`${email}:${password}`);
  const binaryValue = String.fromCharCode(...bytes);
  return `Basic ${window.btoa(binaryValue)}`;
}

export function restoreAuth() {
  const encodedCredentials = window.sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (!encodedCredentials) return false;
  client.defaults.headers.common.Authorization = encodedCredentials;
  return true;
}

export function clearAuth() {
  delete client.defaults.headers.common.Authorization;
  window.sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

export const api = {
  login: async (email, password) => {
    const encodedCredentials = encodeBasicCredentials(email, password);
    client.defaults.headers.common.Authorization = encodedCredentials;
    try {
      const { data } = await client.get('/auth/me');
      window.sessionStorage.setItem(AUTH_STORAGE_KEY, encodedCredentials);
      return data;
    } catch (error) {
      delete client.defaults.headers.common.Authorization;
      throw error;
    }
  },
  currentUser: () => client.get('/auth/me').then(({ data }) => data),
  registerMember: (payload) => client.post('/auth/register', payload).then(({ data }) => data),

  listPlayers: () => client.get('/players').then(({ data }) => data),
  createPlayer: (payload) => client.post('/players', payload).then(({ data }) => data),
  removePlayerFromLadder: (playerId) =>
    client.post(`/players/${playerId}/remove-from-ladder`).then(({ data }) => data),
  updatePlayerRating: (playerId, rating) =>
    client.put(`/players/${playerId}/rating`, { rating }).then(({ data }) => data),

  listTournaments: () => client.get('/tournaments').then(({ data }) => data),
  createTournament: (payload) => client.post('/tournaments', payload).then(({ data }) => data),
  registerForTournament: (tournamentId, playerId) =>
    client.post(`/tournaments/${tournamentId}/registrations`, { playerId }).then(({ data }) => data),
  listTournamentMatches: (tournamentId) =>
    client.get(`/tournaments/${tournamentId}/matches`).then(({ data }) => data),

  createMatch: (payload) => client.post('/matches', payload).then(({ data }) => data),
  listMatches: () => client.get('/matches').then(({ data }) => data),
  submitMatchResult: (matchId, payload) =>
    client.post(`/matches/${matchId}/result`, payload).then(({ data }) => data),
  invalidateMatch: (matchId) => client.post(`/matches/${matchId}/invalidate`).then(({ data }) => data),

  listPracticeSessions: () => client.get('/practice-sessions').then(({ data }) => data),
  createPracticeSession: (payload) => client.post('/practice-sessions', payload).then(({ data }) => data),
  registerForPractice: (sessionId, playerId) =>
    client.post(`/practice-sessions/${sessionId}/registrations`, { playerId }).then(({ data }) => data),

  listMemberPracticeSessions: () => client.get('/member/practice-sessions').then(({ data }) => data),
  listMemberTournaments: () => client.get('/member/tournaments').then(({ data }) => data),
  signUpForPractice: (sessionId) => client.post(`/member/practice-sessions/${sessionId}/signup`).then(({ data }) => data),
  signUpForTournament: (tournamentId) => client.post(`/member/tournaments/${tournamentId}/signup`).then(({ data }) => data)
};

export function errorMessage(error) {
  return error.response?.data?.message || error.message || 'Something went wrong. Please try again.';
}
