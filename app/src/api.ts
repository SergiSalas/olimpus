import Constants from 'expo-constants';
import { fetch as subirFetch } from 'expo/fetch';
import { File } from 'expo-file-system';
import { locale, t } from './i18n';

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
      // Así los errores, los intereses y las preguntas llegan en el idioma de la app.
      'Accept-Language': locale(),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 204) return undefined as T;

  const body = await response.text();
  const data = body ? JSON.parse(body) : null;

  if (!response.ok) {
    throw new ApiError(
      data?.error ?? t('comun.servidorRespondio', { estado: response.status }),
      response.status,
    );
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

/**
 * Borrar la cuenta. Se va todo: registro, foto, conversaciones y mensajes.
 * No hay periodo de gracia ni "la guardamos 30 días por si cambias de idea".
 */
export const deleteAccount = (token: string) =>
  request<void>('/api/me', { method: 'DELETE' }, token);

// ---------- registro ----------

export type Gender = 'WOMAN' | 'MAN' | 'NON_BINARY' | 'OTHER';
export type Intent = 'FRIENDSHIP' | 'DATING' | 'RELATIONSHIP' | 'CASUAL';
export type LanguageLevel = 'BASIC' | 'INTERMEDIATE' | 'NATIVE';

export type LanguageSkill = { code: string; level: LanguageLevel };

/** `name` es el id que se guarda ("ice-climbing"); `label`, lo que se enseña ("Escalada en hielo"). */
export type Interest = { name: string; label: string };

/** La pregunta elegida del catálogo y lo que se contestó. */
export type PromptAnswer = { question: string; answer: string };

/** `name` es el id que se guarda ("always-ask"); `label`, la pregunta escrita. */
export type PromptQuestion = { name: string; label: string };

export type ProfileData = {
  nickname: string;
  /** "1995-03-20" */
  birthDate: string;
  gender: Gender;
  /** Cómo lo dice la persona. Se enseña en el nivel 3, no se usa para emparejar. */
  genderLabel: string;
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
  /** Las tres preguntas contestadas. Es lo que se abre en el nivel 2. */
  prompts: PromptAnswer[];
  occupation: string;
  fromPlace: string;
};

export type Profile = ProfileData & { age: number };

export const fetchInterests = () => request<Interest[]>('/api/interests');

export const fetchPrompts = () => request<PromptQuestion[]>('/api/prompts');

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
 *
 * El fichero se manda con el `File` de expo-file-system, que implementa Blob.
 * El apaño de toda la vida —pasarle a FormData un objeto `{uri, name, type}`—
 * ya no vale: el FormData de React Native pasó a seguir el estándar y lo
 * rechaza con «Unsupported FormDataPart implementation», que es un error que
 * solo se ve al subir de verdad, nunca al compilar. Y el fetch es el de
 * `expo/fetch`, que es el que la documentación de la v57 usa para subir.
 */
export async function uploadPhoto(token: string, uri: string): Promise<PhotoState> {
  const formulario = new FormData();
  formulario.append('file', new File(uri));

  const response = await subirFetch(`${backendUrl()}/api/profile/photo`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Accept-Language': locale() },
    body: formulario,
  });

  const body = await response.text();
  const data = body ? JSON.parse(body) : null;
  if (!response.ok) {
    throw new ApiError(
      data?.error ?? t('comun.servidorRespondio', { estado: response.status }),
      response.status,
    );
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
  /** Desde el nivel 2: las tres preguntas contestadas, con la pregunta ya escrita. */
  prompts: { question: string; label: string; answer: string }[];
  /** Desde el nivel 3. */
  languages: string[];
  intent: string | null;
  genderLabel: string | null;
  occupation: string | null;
  fromPlace: string | null;
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
  /** Si lleva el corazón de quien lo recibió. Los que llegan en directo aún no lo tienen. */
  liked?: boolean;
};

/** Un aviso de desbloqueo, para pintarlo justo después del mensaje que lo abrió. */
export type Unlock = {
  level: number;
  /** null cuando lo abrió el tiempo, no un mensaje. */
  afterMessageId: string | null;
  at: string;
  text: string;
};

export type Chat = {
  conversationId: string;
  state: 'OPEN' | 'CANCELLED' | 'CLOSED' | 'BLOCKED' | 'CONNECTED';
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
  /** Si estamos en la última media hora: toca responder. */
  decisionTime: boolean;
  /** Lo que respondiste tú. Lo del otro no viaja nunca. */
  yourDecision: 'YES' | 'NO' | null;
  /** Si acabó en conexión: el chat ya no cierra. */
  connected: boolean;
  unlocks: Unlock[];
  messages: ChatMessage[];
};

export type Connection = {
  conversationId: string;
  level: number;
  nickname: string | null;
  age: number;
  interests: string[];
  photoAvailable: boolean;
  connectedOn: string;
  lastMessage: string | null;
  lastMessageAt: string | null;
};

export type ReportReason =
  'DISRESPECT' | 'UNWANTED_SEXUAL' | 'SPAM' | 'FAKE_PROFILE' | 'LOOKS_UNDERAGE' | 'OTHER';

/** Un toque: corta la conversación al momento y no os vuelve a emparejar. */
export const reportar = (token: string, conversationId: string, reason: ReportReason | null) =>
  request<void>(
    `/api/conversations/${conversationId}/report`,
    { method: 'POST', body: JSON.stringify({ reason }) },
    token,
  );

/** Dónde encontrar este móvil, para los cinco avisos. */
export const registerPushToken = (token: string, pushToken: string) =>
  request<void>(
    '/api/push-token',
    { method: 'POST', body: JSON.stringify({ token: pushToken }) },
    token,
  );

export const fetchConnections = (token: string) =>
  request<Connection[]>('/api/connections', {}, token);

/**
 * La respuesta del final del día. No devuelve nada: el resultado se monta a las
 * 22:00, así que ni el momento de esta llamada puede filtrarlo.
 */
export const decide = (token: string, conversationId: string, answer: 'YES' | 'NO') =>
  request<void>(
    `/api/conversations/${conversationId}/decision`,
    { method: 'POST', body: JSON.stringify({ answer }) },
    token,
  );

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

/** Pone o quita el corazón en un mensaje de la otra persona. */
export const likeMessage = (
  token: string,
  conversationId: string,
  messageId: string,
  liked: boolean,
) =>
  request<{ messageId: string; liked: boolean }>(
    `/api/conversations/${conversationId}/messages/${messageId}/like`,
    { method: 'POST', body: JSON.stringify({ liked }) },
    token,
  );

/**
 * Conexión permanente para recibir lo que escribe el otro al instante, y los
 * corazones que se ponen o se quitan. Enviar se hace por HTTP, donde están las reglas.
 */
export function openChatSocket(
  token: string,
  onMessage: (m: ChatMessage & { conversationId: string; level: number }) => void,
  onLike?: (like: { conversationId: string; messageId: string; liked: boolean }) => void,
) {
  const url = `${backendUrl().replace(/^http/, 'ws')}/ws/chat?token=${encodeURIComponent(token)}`;
  const socket = new WebSocket(url);

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(String(event.data));
      if (data.type === 'message') onMessage(data);
      if (data.type === 'like') onLike?.(data);
    } catch {
      // Un mensaje que no se entiende no puede tumbar el chat.
    }
  };

  return socket;
}

// ---------- modo de pruebas (solo en desarrollo) ----------

/** Qué paso del día adelantar en la conversación de prueba. */
export type PasoDemo = 'HOUR' | 'PHOTO' | 'DECISION' | 'CLOSE';

/** Crea una persona de prueba que encaja contigo y te da la conversación de hoy con ella. */
export const startDemo = (token: string) =>
  request<{ conversationId: string; nickname: string }>('/api/dev/demo', { method: 'POST' }, token);

/** Mueve el reloj de la conversación de prueba: las reglas no cambian, solo las horas. */
export const advanceDemo = (token: string, step: PasoDemo) =>
  request<unknown>(`/api/dev/demo/advance?step=${step}`, { method: 'POST' }, token);
