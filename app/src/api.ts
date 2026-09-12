import Constants from 'expo-constants';

/**
 * Durante el desarrollo, el movil carga la app desde este mismo ordenador.
 * Expo sabe su direccion (hostUri, algo como "192.168.1.40:8081"), asi que
 * el backend esta en esa misma direccion pero en el puerto 8080. Asi no hay
 * que escribir la IP a mano ni cambiarla cada vez que cambia el wifi.
 */
export function backendUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_BACKEND_URL;
  if (fromEnv) return fromEnv;

  const hostUri = Constants.expoConfig?.hostUri ?? '';
  const host = hostUri.split(':')[0];
  if (!host) {
    throw new Error('No se sabe la direccion del backend. Define EXPO_PUBLIC_BACKEND_URL.');
  }
  return `http://${host}:8080`;
}

/** El backend contesta {"error": "..."} con un mensaje ya escrito para mostrar. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(`${backendUrl()}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 204) return undefined as T;

  const body = await response.text();
  const data = body ? JSON.parse(body) : null;

  if (!response.ok) {
    throw new ApiError(data?.error ?? `El servidor respondió ${response.status}`, response.status);
  }
  return data as T;
}

export type Health = {
  status: string;
  schemaVersion: string;
  /** Instante en UTC (ISO-8601). Se muestra en la hora local de quien mira. */
  databaseTime: string;
};

export type StartedSession = {
  token: string;
  expiresAt: string;
  accountId: string;
  email: string;
  isNewAccount: boolean;
};

export type Me = {
  accountId: string;
  email: string;
  createdAt: string;
};

export const fetchHealth = () => request<Health>('/api/health');

/** Pide el codigo de seis cifras. Responde igual exista la cuenta o no. */
export const requestLoginCode = (email: string) =>
  request<void>('/api/auth/code', { method: 'POST', body: JSON.stringify({ email }) });

export const verifyLoginCode = (email: string, code: string) =>
  request<StartedSession>('/api/auth/verify', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
  });

export const fetchMe = (token: string) => request<Me>('/api/me', {}, token);

// ---------- registro ----------

export type Gender = 'MUJER' | 'HOMBRE' | 'NO_BINARIO' | 'OTRO';
export type Intent = 'AMISTAD' | 'CITAS' | 'PAREJA' | 'CASUAL';
export type LanguageLevel = 'BASICO' | 'MEDIO' | 'NATIVO';

export type LanguageSkill = { code: string; level: LanguageLevel };

export type Interest = { name: string; label: string };

export type ProfileData = {
  nickname: string;
  bio: string;
  /** "1995-03-20" */
  birthDate: string;
  gender: Gender;
  seeking: Gender[];
  ageMin: number;
  ageMax: number;
  maxDistanceKm: number;
  latitude: number;
  longitude: number;
  languages: LanguageSkill[];
  sociability: number;
  conversationDepth: number;
  intent: Intent;
  interests: string[];
};

export type Profile = ProfileData & { age: number };

export const fetchInterests = () => request<Interest[]>('/api/interests');

/** Devuelve null si esa cuenta todavía no ha hecho el registro. */
export async function fetchProfile(token: string): Promise<Profile | null> {
  try {
    return await request<Profile>('/api/profile', {}, token);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export const saveProfile = (token: string, data: ProfileData) =>
  request<Profile>('/api/profile', { method: 'PUT', body: JSON.stringify(data) }, token);
