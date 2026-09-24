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

/**
 * Si ya se vio la presentación de una conversación: la animación de entrada
 * solo sale la primera vez. Si el llavero falla se da por vista, que saltarse
 * una animación no rompe nada y repetirla cada vez sí cansa.
 */
export async function presentacionVista(conversationId: string): Promise<boolean> {
  try {
    return (await SecureStore.getItemAsync(`olimpus.visto.${conversationId}`)) !== null;
  } catch {
    return true;
  }
}

export async function marcarPresentacionVista(conversationId: string): Promise<void> {
  await SecureStore.setItemAsync(`olimpus.visto.${conversationId}`, '1').catch(() => {});
}
