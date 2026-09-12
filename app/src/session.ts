import * as SecureStore from 'expo-secure-store';

/**
 * La llave de sesion se guarda en el llavero del sistema (Keychain en iPhone),
 * no en el almacenamiento normal: ahi va cifrada y no la ve otra app.
 */
const KEY = 'olimpus.session.token';

export async function saveToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(KEY, token);
}

export async function readToken(): Promise<string | null> {
  try {
    return await SecureStore.getItemAsync(KEY);
  } catch {
    // Si el llavero falla, se empieza sin sesion en vez de romper la app.
    return null;
  }
}

export async function clearToken(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY);
}
