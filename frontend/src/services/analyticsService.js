const ANALYTICS_EVENTS_STORAGE_KEY = "cc.analytics.events";
const MAX_TRACKED_EVENTS = 150;

const readTrackedEvents = () => {
  if (typeof globalThis.window === "undefined") {
    return [];
  }

  try {
    const rawEvents = globalThis.localStorage.getItem(ANALYTICS_EVENTS_STORAGE_KEY);
    if (!rawEvents) {
      return [];
    }

    const parsedEvents = JSON.parse(rawEvents);
    return Array.isArray(parsedEvents) ? parsedEvents : [];
  } catch {
    return [];
  }
};

const writeTrackedEvents = (events) => {
  if (typeof globalThis.window === "undefined") {
    return;
  }

  globalThis.localStorage.setItem(ANALYTICS_EVENTS_STORAGE_KEY, JSON.stringify(events));
};

const trackUiEvent = (eventName, payload = {}) => {
  const normalizedEventName = String(eventName || "").trim();

  if (!normalizedEventName) {
    return;
  }

  const nextEvent = {
    eventName: normalizedEventName,
    payload,
    timestamp: new Date().toISOString(),
  };

  const currentEvents = readTrackedEvents();
  const nextEvents = [...currentEvents, nextEvent].slice(-MAX_TRACKED_EVENTS);
  writeTrackedEvents(nextEvents);
};

export { readTrackedEvents, trackUiEvent };
