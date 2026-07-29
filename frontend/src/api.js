import axios from 'axios';

const client = axios.create({
  // Use the Vite proxy locally; set VITE_API_BASE_URL to the deployed API later.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' }
});

export const api = {
  listPlayers: () => client.get('/players').then(({ data }) => data),
  createPlayer: (payload) => client.post('/players', payload).then(({ data }) => data),

  listTournaments: () => client.get('/tournaments').then(({ data }) => data),
  createTournament: (payload) => client.post('/tournaments', payload).then(({ data }) => data),
  registerForTournament: (tournamentId, playerId) =>
    client.post(`/tournaments/${tournamentId}/registrations`, { playerId }).then(({ data }) => data),
  listTournamentMatches: (tournamentId) =>
    client.get(`/tournaments/${tournamentId}/matches`).then(({ data }) => data),

  createMatch: (payload) => client.post('/matches', payload).then(({ data }) => data),
  submitMatchResult: (matchId, payload) =>
    client.post(`/matches/${matchId}/result`, payload).then(({ data }) => data),

  listPracticeSessions: () => client.get('/practice-sessions').then(({ data }) => data),
  createPracticeSession: (payload) => client.post('/practice-sessions', payload).then(({ data }) => data),
  registerForPractice: (sessionId, playerId) =>
    client.post(`/practice-sessions/${sessionId}/registrations`, { playerId }).then(({ data }) => data)
};

export function errorMessage(error) {
  return error.response?.data?.message || error.message || 'Something went wrong. Please try again.';
}
