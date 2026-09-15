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

export type Gender = 'WOMAN' | 'MAN' | 'NON_BINARY' | 'OTHER';
export type Intent = 'FRIENDSHIP' | 'DATING' | 'RELATIONSHIP' | 'CASUAL';
export type LanguageLevel = 'BASIC' | 'INTERMEDIATE' | 'NATIVE';

export type LanguageSkill = { code: string; level: LanguageLevel };

/** `name` es el id que se guarda ("ice-climbing"); `label`, lo que se enseña ("Escalada en hielo"). */
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

// ---------- la foto ----------

export type PhotoState = {
  uploaded: boolean;
  moderation: 'PENDING' | 'APPROVED' | 'REJECTED';
  uploadedAt: string;
};

/**
 * La foto viaja como formulario, no como JSON: así no hay que convertirla a
 * texto y crecer un tercio por el camino.
 */
export async function uploadPhoto(token: string, uri: string): Promise<PhotoState> {
  const formulario = new FormData();
  formulario.append('file', {
    uri,
    name: 'foto.jpg',
    type: 'image/jpeg',
  } as unknown as Blob);

  const response = await fetch(`${backendUrl()}/api/profile/photo`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formulario,
  });

  const body = await response.text();
  const data = body ? JSON.parse(body) : null;
  if (!response.ok) {
    throw new ApiError(data?.error ?? `El servidor respondió ${response.status}`, response.status);
  }
  return data as PhotoState;
}

/**
 * La dirección de tu propia foto. No es un enlace público: el servidor
 * comprueba la llave en cada petición, así que hay que pasarle la cabecera.
 */
export function ownPhotoSource(token: string) {
  return {
    uri: `${backendUrl()}/api/profile/photo`,
    headers: { Authorization: `Bearer ${token}` },
  };
}

// ---------- la conversación del día ----------

/**
 * Lo que se ve del otro, según lo que la conversación haya desbloqueado.
 *
 * Un campo en null no es que falte: es que todavía no se ha ganado. El servidor
 * ni siquiera lo manda, así que la app no podría enseñarlo por error.
 */
export type Partner = {
  /** 0 match · 1 primer mensaje · 2 conversación · 3 buena conexión · 4 confianza */
  level: number;
  age: number;
  interests: string[];
  approxDistanceKm: number;
  /** Desde el nivel 1. */
  nickname: string | null;
  /** Desde el nivel 2. */
  bio: string | null;
  /** Desde el nivel 3. */
  languages: string[];
  intent: string | null;
  /** Nivel 3: la foto ya se puede pedir. */
  photoAvailable: boolean;
};

export type Today = {
  hasConversation: boolean;
  conversationId: string | null;
  /** Instante de cierre en UTC; siempre las 22:00 de la comunidad. */
  closesAt: string | null;
  partner: Partner | null;
  sharedInterests: string[];
  /** Cuándo llega la siguiente oportunidad, si hoy no hay nadie. */
  nextRoundAt: string | null;
  message: string | null;
};

export const fetchToday = (token: string) => request<Today>('/api/today', {}, token);

// ---------- el chat ----------

export type ChatMessage = {
  id: string;
  /** true si lo escribiste tú: el móvil no necesita comparar identificadores. */
  mine: boolean;
  text: string;
  sentAt: string;
};

export type Chat = {
  conversationId: string;
  state: 'OPEN' | 'CANCELLED' | 'CLOSED';
  closesAt: string;
  /** La pregunta con la que arranca, sacada de un interés que compartís. */
  icebreaker: string;
  partner: Partner;
  sharedInterests: string[];
  bothHaveWritten: boolean;
  /** Si el botón de "quiero verte" debe estar ya en pantalla. */
  canAskForPhoto: boolean;
  /** Si tú ya lo pediste. Lo que haya hecho el otro no se cuenta. */
  alreadyAskedForPhoto: boolean;
  messages: ChatMessage[];
};

export const fetchChat = (token: string, conversationId: string) =>
  request<Chat>(`/api/conversations/${conversationId}`, {}, token);

/** "Quiero verte". Solo dice si habéis aceptado los dos, nunca qué hizo el otro. */
export const askToSeePhoto = (token: string, conversationId: string) =>
  request<{ bothAccepted: boolean; level: number }>(
    `/api/conversations/${conversationId}/see-photo`,
    { method: 'POST' },
    token,
  );

/** La foto del otro. No es un enlace público: se comprueba en cada lectura. */
export function partnerPhotoSource(token: string, conversationId: string) {
  return {
    uri: `${backendUrl()}/api/conversations/${conversationId}/partner-photo`,
    headers: { Authorization: `Bearer ${token}` },
  };
}

export const sendMessage = (token: string, conversationId: string, text: string) =>
  request<ChatMessage>(
    `/api/conversations/${conversationId}/messages`,
    { method: 'POST', body: JSON.stringify({ text }) },
    token,
  );

/**
 * Conexión permanente para recibir lo que escribe el otro al instante.
 * Solo baja mensajes: enviar se hace por HTTP, donde ya están las reglas.
 */
export function openChatSocket(token: string, onMessage: (m: ChatMessage & { conversationId: string }) => void) {
  const url = `${backendUrl().replace(/^http/, 'ws')}/ws/chat?token=${encodeURIComponent(token)}`;
  const socket = new WebSocket(url);

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(String(event.data));
      if (data.type === 'message') onMessage(data);
    } catch {
      // Un mensaje que no se entiende no puede tumbar el chat.
    }
  };

  return socket;
}
