/**
 * El sistema visual, sacado del diseño "Flujo de presentación".
 *
 * Un solo sitio para colores, tipografías y formas: si el diseño cambia, se
 * cambia aquí y no en veinte pantallas.
 */

export const colors = {
  /** Fondo de la app. */
  bg: '#F7F2EA',
  /** Fondo de las pantallas de bienvenida y de match, con degradado. */
  bgCalido: '#F2E3D5',
  /** Tarjetas y campos. */
  surface: '#FFFDF9',
  /** Fondo de piezas secundarias (botón atrás, huecos de foto). */
  surface2: '#EDE5D8',
  line: '#E8DECD',
  lineSuave: '#EFE6D8',
  lineFuerte: '#E4DACA',

  /** Texto principal, un negro cálido. También el fondo de los botones oscuros. */
  ink: '#211D1A',
  ink2: '#5E564E',
  ink3: '#8A8177',
  ink4: '#9A9188',
  ink5: '#A79E93',

  /** El color de la marca. */
  accent: '#C8553D',
  accentOscuro: '#A8402C',
  /** Fondo suave del acento, para lo elegido. */
  accentWash: '#FBEFE9',
  accentBorde: '#EBD3C9',
  /** Acento sobre fondo oscuro. */
  accentClaro: '#C8A99B',

  /** Texto sobre fondo oscuro. */
  onInk: '#F7F2EA',
  onInk2: '#BFB4AA',

  error: '#B23A2F',
  ok: '#2C6A4F',
};

export const fonts = {
  /** Titulares. */
  serif: 'InstrumentSerif_400Regular',
  serifItalic: 'InstrumentSerif_400Regular_Italic',
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
    fontFamily: fonts.serif,
    fontSize: 32,
    lineHeight: 35,
    color: colors.ink,
  },
  /** Titular grande, de bienvenida. */
  tituloGrande: {
    fontFamily: fonts.serif,
    fontSize: 40,
    lineHeight: 42,
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
  boton: 16,
  tarjeta: 22,
  campo: 14,
  fila: 14,
  pastilla: 999,
};

export const espacios = {
  pantalla: 26,
  entre: 10,
};
