type BackendConfig = {
  baseUrl: string;
  username: string;
  password: string;
  timeoutMs: number;
};

function getRequiredEnv(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function getTimeoutMs(): number {
  const raw = process.env.BACKEND_TIMEOUT_MS?.trim();
  if (!raw) {
    return 10_000;
  }

  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error("BACKEND_TIMEOUT_MS must be a positive number");
  }

  return parsed;
}

export function loadBackendConfig(): BackendConfig {
  const baseUrl = getRequiredEnv("BACKEND_BASE_URL").replace(/\/+$/, "");

  return {
    baseUrl,
    username: getRequiredEnv("BACKEND_USERNAME"),
    password: getRequiredEnv("BACKEND_PASSWORD"),
    timeoutMs: getTimeoutMs(),
  };
}

export type { BackendConfig };