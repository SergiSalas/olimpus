import * as Localization from 'expo-localization';
import * as SecureStore from 'expo-secure-store';
import { useSyncExternalStore } from 'react';
import { en } from './idiomas/en';
import { es, type Clave } from './idiomas/es';

/**
 * El idioma de la app.
 *
 * Los textos viven en un diccionario por idioma (src/idiomas). El castellano es
 * la referencia: los demás están tipados contra él, así que una clave que falte
 * en otro idioma no compila. Sin librería: con buscar la clave y sustituir las
 * variables basta.
 *
 * El idioma elegido también viaja al servidor en cada petición, para que los
 * errores, los intereses y las preguntas lleguen en el mismo idioma.
 */
export type Idioma = 'es' | 'en';

export const IDIOMAS_APP: { codigo: Idioma; bandera: string; nombre: string }[] = [
  { codigo: 'es', bandera: '🇪🇸', nombre: 'Castellano' },
  { codigo: 'en', bandera: '🇬🇧', nombre: 'English' },
];

const DICCIONARIOS: Record<Idioma, Record<Clave, string>> = { es, en };

/** Para fechas, horas y la cabecera Accept-Language. */
const LOCALES: Record<Idioma, string> = { es: 'es-ES', en: 'en-GB' };

const LLAVE = 'olimpus.idioma';

let actual: Idioma = 'es';
const oyentes = new Set<() => void>();

const esIdioma = (valor: string | null | undefined): valor is Idioma =>
  IDIOMAS_APP.some((i) => i.codigo === valor);

/** "Quedan {tiempo}" con { tiempo: '2 h' } → "Quedan 2 h". */
export function t(clave: Clave, variables?: Record<string, string | number>): string {
  let texto = DICCIONARIOS[actual][clave] ?? es[clave];
  if (variables) {
    for (const [nombre, valor] of Object.entries(variables)) {
      texto = texto.split(`{${nombre}}`).join(String(valor));
    }
  }
  return texto;
}

export const idioma = () => actual;
export const locale = () => LOCALES[actual];

/**
 * Al abrir: el que se eligió, si se eligió alguno; si no, el del móvil si lo
 * tenemos; y si tampoco, castellano.
 */
export async function cargarIdioma(): Promise<void> {
  let guardado: string | null = null;
  try {
    guardado = await SecureStore.getItemAsync(LLAVE);
  } catch {
    // Sin llavero se sigue con el del móvil.
  }
  const delMovil = Localization.getLocales()[0]?.languageCode;
  actual = esIdioma(guardado) ? guardado : esIdioma(delMovil) ? delMovil : 'es';
}

export async function cambiarIdioma(nuevo: Idioma): Promise<void> {
  actual = nuevo;
  oyentes.forEach((avisar) => avisar());
  await SecureStore.setItemAsync(LLAVE, nuevo).catch(() => {});
}

/** El idioma actual, y un repintado cuando cambia. */
export function useIdioma(): Idioma {
  return useSyncExternalStore(
    (avisar) => {
      oyentes.add(avisar);
      return () => oyentes.delete(avisar);
    },
    () => actual,
  );
}

/** "21:30" en el formato del idioma elegido. */
export const horaCorta = (fecha: Date | number | string) =>
  new Date(fecha).toLocaleTimeString(locale(), { hour: '2-digit', minute: '2-digit' });
