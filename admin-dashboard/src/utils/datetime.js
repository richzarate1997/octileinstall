// <input type="datetime-local"> needs "YYYY-MM-DDTHH:mm" - no timezone, no seconds.
// The value round-trips fine as-is to the backend: Jackson's LocalDateTime deserializer
// (java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) treats seconds as optional.
export function toLocalInputValue(isoString) {
  const date = new Date(isoString);
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
