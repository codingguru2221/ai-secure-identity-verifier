const TOKEN_KEY = "ai_verifier_token";
const USERNAME_KEY = "ai_verifier_username";
const ROLE_KEY = "ai_verifier_role";

export interface AuthSession {
  token: string;
  username: string;
  role: string;
}

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function setAuthSession(session: AuthSession): void {
  localStorage.setItem(TOKEN_KEY, session.token);
  localStorage.setItem(USERNAME_KEY, session.username);
  localStorage.setItem(ROLE_KEY, session.role);
}

export function getAuthSession(): AuthSession | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const username = localStorage.getItem(USERNAME_KEY);
  const role = localStorage.getItem(ROLE_KEY);
  if (!token || !username || !role) {
    return null;
  }
  return { token, username, role };
}

export function clearAuthToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(ROLE_KEY);
}

export function authHeaders(): HeadersInit {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}
