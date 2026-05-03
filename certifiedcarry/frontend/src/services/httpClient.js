import axios from "axios";
import { getCurrentFirebaseIdToken } from "./firebaseClient";

const DEFAULT_API_BASE_URL = "/api";
const configuredApiUrl = String(import.meta.env.VITE_API_URL || "").trim();
const apiBaseUrl = configuredApiUrl || DEFAULT_API_BASE_URL;

const configuredTimeout = Number.parseInt(String(import.meta.env.VITE_API_TIMEOUT_MS || ""), 10);
const apiTimeout =
  Number.isInteger(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 15000;

const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: apiTimeout,
  headers: {
    Accept: "application/json",
  },
});

httpClient.interceptors.request.use(async (config) => {
  const requestConfig = config;

  if (requestConfig.headers?.Authorization) {
    return requestConfig;
  }

  try {
    const firebaseIdToken = await getCurrentFirebaseIdToken();

    if (firebaseIdToken) {
      requestConfig.headers = {
        ...requestConfig.headers,
        Authorization: `Bearer ${firebaseIdToken}`,
      };
    }
  } catch {
    // Keep requests working even when Firebase is not configured yet.
  }

  return requestConfig;
});

export { apiBaseUrl, apiTimeout };
export default httpClient;
