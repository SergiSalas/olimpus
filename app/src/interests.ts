import { useEffect, useState } from 'react';
import { fetchInterests } from './api';
import { idioma, type Idioma } from './i18n';

/**
 * El backend guarda y devuelve los intereses por su id ("ice-climbing"); el
 * nombre que se enseña ("Escalada en hielo") viene del catálogo de
 * /api/interests, ya traducido. Se pide una sola vez por idioma para toda la app.
 */
const catalogos = new Map<Idioma, Promise<Record<string, string>>>();

function cargarNombres(): Promise<Record<string, string>> {
  const deEste = idioma();
  let catalogo = catalogos.get(deEste);
  if (!catalogo) {
    catalogo = fetchInterests()
      .then((lista) => Object.fromEntries(lista.map((i) => [i.name, i.label])))
      .catch((error) => {
        catalogos.delete(deEste); // que el siguiente intento vuelva a pedirlo
        throw error;
      });
    catalogos.set(deEste, catalogo);
  }
  return catalogo;
}

/** Un emoji por interés del catálogo. Uno nuevo sin emoji sale con ✨ hasta que se le ponga. */
const EMOJIS: Record<string, string> = {
  travel: '✈️',
  music: '🎵',
  movies: '🎬',
  cooking: '🍳',
  'tv-series': '📺',
  reading: '📚',
  sports: '⚽',
  tapas: '🍤',
  photography: '📷',
  hiking: '🥾',
  concerts: '🎤',
  'video-games': '🎮',
  art: '🎨',
  running: '🏃',
  gym: '🏋️',
  animals: '🐾',
  theatre: '🎭',
  gardening: '🌱',
  podcasts: '🎧',
  dancing: '💃',
  guitar: '🎸',
  surfing: '🏄',
  cycling: '🚴',
  history: '🏛️',
  astronomy: '🔭',
  climbing: '🧗',
  chess: '♟️',
  ceramics: '🏺',
  diving: '🤿',
  'board-games': '🎲',
  philosophy: '🤔',
  wine: '🍷',
  improv: '🤹',
  beekeeping: '🐝',
  'ice-climbing': '🧊',
  'instrument-making': '🪕',
  birdwatching: '🐦',
  kendo: '🥋',
};

/** "♟️ Ajedrez": el nombre de un interés con su emoji delante. */
export const conEmoji = (id: string, nombre: string) => `${EMOJIS[id] ?? '✨'} ${nombre}`;

/** Devuelve una función id -> nombre con emoji. Mientras carga, enseña el id sin guiones. */
export function useNombreInteres(): (id: string) => string {
  const [nombres, setNombres] = useState<Record<string, string>>({});

  useEffect(() => {
    let vivo = true;
    cargarNombres()
      .then((n) => vivo && setNombres(n))
      .catch(() => {});
    return () => {
      vivo = false;
    };
  }, []);

  return (id) => conEmoji(id, nombres[id] ?? id.replace(/-/g, ' '));
}
