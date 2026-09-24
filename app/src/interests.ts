import { useEffect, useState } from 'react';
import { fetchInterests } from './api';

/**
 * El backend guarda y devuelve los intereses por su id ("ice-climbing"); el
 * nombre que se enseña ("Escalada en hielo") viene del catálogo de
 * /api/interests. Se pide una sola vez para toda la app.
 */
let catalogo: Promise<Record<string, string>> | null = null;

function cargarNombres(): Promise<Record<string, string>> {
  if (!catalogo) {
    catalogo = fetchInterests()
      .then((lista) => Object.fromEntries(lista.map((i) => [i.name, i.label])))
      .catch((error) => {
        catalogo = null; // que el siguiente intento vuelva a pedirlo
        throw error;
      });
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
