export function toIso(value) {
  return new Date(value).toISOString();
}

export function toDateTimeInput(value) {
  const date = value ? new Date(value) : new Date();
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function addDays(days, hour = 18) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  date.setHours(hour, 0, 0, 0);
  return toDateTimeInput(date);
}

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit'
});

export function formatDateTime(value) {
  return value ? dateTimeFormatter.format(new Date(value)) : 'Not scheduled';
}

export function formatRange(start, end) {
  return `${formatDateTime(start)} — ${formatDateTime(end)}`;
}

export function byRating(players) {
  return [...players].sort((a, b) => {
    const establishedDifference = Number(isRatingEstablished(b)) - Number(isRatingEstablished(a));
    return establishedDifference || b.rating - a.rating;
  });
}

export function isRatingEstablished(player) {
  return player.ratingEstablished !== false;
}

export function playerRatingLabel(player) {
  return isRatingEstablished(player)
    ? String(player.rating)
    : `Unrated · ${player.provisionalMatchCount || 0}/5 matches`;
}
