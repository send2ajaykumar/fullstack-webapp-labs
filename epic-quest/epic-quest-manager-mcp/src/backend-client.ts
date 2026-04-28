import { BackendConfig } from "./config.js";

type LoginResponse = {
  access_token: string;
  token_type: string;
  expires_in: number;
};

type QuestAcceptanceResponse = {
  id: number;
  title: string;
  description?: string;
  difficultyLevel: number;
  requiredRarity: string;
  createdAt?: string;
  updatedAt?: string;
};

class BackendApiError extends Error {
  readonly status: number;
  readonly details: string;

  constructor(status: number, message: string, details: string) {
    super(message);
    this.name = "BackendApiError";
    this.status = status;
    this.details = details;
  }
}

class EpicQuestBackendClient {
  private readonly config: BackendConfig;
  private token: string | null = null;
  private tokenExpiresAt = 0;

  constructor(config: BackendConfig) {
    this.config = config;
  }

  async acceptQuestForHero(heroId: number, questId: number): Promise<QuestAcceptanceResponse> {
    const token = await this.getAccessToken();

    return this.request<QuestAcceptanceResponse>(
      `/heroes/${heroId}/quests/${questId}/accept`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );
  }

  private async getAccessToken(): Promise<string> {
    const now = Date.now();
    if (this.token && now < this.tokenExpiresAt - 5_000) {
      return this.token;
    }

    const login = await this.request<LoginResponse>("/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: this.config.username,
        password: this.config.password,
      }),
    });

    this.token = login.access_token;
    this.tokenExpiresAt = Date.now() + login.expires_in * 1000;
    return login.access_token;
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.timeoutMs);

    try {
      const response = await fetch(`${this.config.baseUrl}${path}`, {
        ...init,
        headers: {
          Accept: "application/json",
          ...(init.headers ?? {}),
        },
        signal: controller.signal,
      });

      const responseText = await response.text();
      if (!response.ok) {
        throw new BackendApiError(
          response.status,
          this.mapHttpError(response.status, responseText),
          responseText,
        );
      }

      if (!responseText) {
        return {} as T;
      }

      return JSON.parse(responseText) as T;
    } catch (error) {
      if (error instanceof BackendApiError) {
        throw error;
      }

      if (error instanceof Error && error.name === "AbortError") {
        throw new Error(`Backend request timed out after ${this.config.timeoutMs}ms`);
      }

      throw new Error(
        error instanceof Error
          ? `Backend request failed: ${error.message}`
          : "Backend request failed",
      );
    } finally {
      clearTimeout(timeout);
    }
  }

  private mapHttpError(status: number, responseText: string): string {
    const normalized = responseText.trim();

    switch (status) {
      case 400:
        return normalized || "The backend rejected the request due to a business rule or validation failure.";
      case 401:
        return normalized || "Authentication against the backend failed.";
      case 403:
        return normalized || "The configured backend user is not allowed to perform this action.";
      case 404:
        return normalized || "The requested hero or quest was not found.";
      case 409:
        return normalized || "The backend reported a conflict.";
      default:
        return normalized || `The backend returned HTTP ${status}.`;
    }
  }
}

export { BackendApiError, EpicQuestBackendClient };
export type { QuestAcceptanceResponse };