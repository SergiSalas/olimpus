/**
 * El sistema visual: colores de caramelo, titulares redondos y todo con rebote.
 *
 * Un solo sitio para colores, tipografías y formas: si el diseño cambia, se
 * cambia aquí y no en veinte pantallas.
 */

export const colors = {
  /** Fondo de la app: crema melocotón. */
  bg: '#FFF5EA',
  /** Fondo de las pantallas de bienvenida y de match. */
  bgCalido: '#FFE4F0',
  /** Tarjetas y campos. */
  surface: '#FFFFFF',
  /** Fondo de piezas secundarias (botón atrás, huecos de foto). */
  surface2: '#F1E8FF',
  line: '#EBDDF6',
  lineSuave: '#F4EBFB',
  lineFuerte: '#E0CFF2',
  /** Bordes de círculos, casillas y cajas punteadas. */
  trazo: '#CDB8EA',

  /** Texto principal, uva oscura. También el fondo de los botones oscuros. */
  ink: '#2A1747',
  ink2: '#57467A',
  ink3: '#7F70A0',
  ink4: '#968AB4',
  ink5: '#ADA3C9',

  /** El color de la marca: chicle. */
  accent: '#E6296B',
  accentOscuro: '#B81A52',
  /** Fondo suave del acento, para lo elegido. */
  accentWash: '#FFE9F1',
  accentBorde: '#FFC2D7',
  /** Acento sobre fondo oscuro. */
  accentClaro: '#FFD0E1',

  /** Colores de juego, para adornos y celebraciones. */
  sol: '#FFC53D',
  menta: '#2ED3A0',
  cielo: '#4DB5FF',
  uva: '#8B5CF6',

  /** Texto sobre fondo oscuro. */
  onInk: '#FFF5EA',
  onInk2: '#CBBFE3',
  /** Velo sobre la pantalla detrás de un diálogo. */
  velo: 'rgba(42,23,71,0.5)',

  error: '#E03A45',
  ok: '#14A06B',
};

export const fonts = {
  /** Titulares, redondos y alegres. */
  display: 'Fredoka_600SemiBold',
  displayFuerte: 'Fredoka_700Bold',
  /** Todo lo demás. */
  sans: 'Figtree_400Regular',
  sansMedia: 'Figtree_500Medium',
  sansNegrita: 'Figtree_600SemiBold',
  sansMuyNegrita: 'Figtree_700Bold',
};

/** Textos que se repiten en todas las pantallas. */
export const text = {
  /** Titular de pantalla. */
  titulo: {
    fontFamily: fonts.display,
    fontSize: 30,
    lineHeight: 36,
    color: colors.ink,
  },
  /** Titular grande, de bienvenida. */
  tituloGrande: {
    fontFamily: fonts.display,
    fontSize: 38,
    lineHeight: 44,
    color: colors.ink,
  },
  /** Frase bajo el titular. */
  ayuda: {
    fontFamily: fonts.sans,
    fontSize: 14.5,
    lineHeight: 22,
    color: colors.ink3,
  },
  cuerpo: {
    fontFamily: fonts.sans,
    fontSize: 15.5,
    lineHeight: 25,
    color: colors.ink2,
  },
  /** Etiqueta pequeña en mayúsculas, para encabezar bloques. */
  etiqueta: {
    fontFamily: fonts.sansMedia,
    fontSize: 11,
    letterSpacing: 1.5,
    textTransform: 'uppercase' as const,
    color: colors.ink4,
  },
  boton: {
    fontFamily: fonts.sansNegrita,
    fontSize: 16.5,
    color: '#FFFFFF',
  },
};

export const radios = {
  boton: 20,
  tarjeta: 26,
  campo: 18,
  fila: 18,
  pastilla: 999,
};

export const espacios = {
  pantalla: 26,
  entre: 10,
};
