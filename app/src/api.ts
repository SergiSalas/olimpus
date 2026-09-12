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

export type Health = {
  status: string;
  schemaVersion: string;
  /** Instante en UTC (ISO-8601). Se muestra en la hora local de quien mira. */
  databaseTime: string;
};

export async function fetchHealth(): Promise<Health> {
  const response = await fetch(`${backendUrl()}/api/health`);
  if (!response.ok) {
    throw new Error(`El servidor respondio ${response.status}`);
  }
  return response.json();
}
