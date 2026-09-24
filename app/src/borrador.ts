import {
  saveProfile,
  uploadPhoto,
  type Gender,
  type Intent,
  type LanguageSkill,
  type Profile,
  type ProfileData,
  type PromptAnswer,
} from './api';
import {
  edadDe,
  fechaISO,
  fechaTecleada,
  profundidadDe,
  rasgosDe,
  sociabilidadDe,
  tipoDeProfundidad,
  type Rasgos,
  type TipoConversacion,
} from './mapeo';

/**
 * Lo que se lleva respondido, en los términos en que se pregunta.
 *
 * Vive aquí, fuera de las pantallas, porque son dos las que lo usan: el
 * registro de la primera vez, que va por bloques, y la pantalla de editar, que
 * lo enseña todo de una vez. Las dos rellenan el mismo borrador y lo mandan por
 * la misma puerta.
 */
export type Borrador = {
  apodo: string;
  /** DDMMAAAA, tal y como se teclea. */
  fecha: string;
  genero: Gender | null;
  generoEtiqueta: string;
  busco: Gender[];
  edadMin: number;
  edadMax: number;
  /**
   * Solo para la pantalla: si nadie ha tocado el rango, se recalcula a partir
   * de la edad en vez de dejar puesto un 27-38 que nadie eligió. No se manda.
   */
  rangoTocado: boolean;
  distancia: number;
  ubicacion: { latitude: number; longitude: number } | null;
  idiomas: LanguageSkill[];
  rasgos: Rasgos;
  conversacion: TipoConversacion;
  intencion: Intent | null;
  intereses: string[];
  /** Hasta tres, y hacen falta las tres para terminar. */
  preguntas: PromptAnswer[];
  trabajo: string;
  sitio: string;
  /** Ruta de la foto ya reducida en el móvil, lista para enviar. */
  foto: string | null;
};

export const PREGUNTAS = 3;
export const RESPUESTA_MAX = 200;
export const INTERESES_MIN = 5;
export const INTERESES_MAX = 8;
export const IDIOMAS_MAX = 5;
export const APODO_MIN = 2;
export const APODO_MAX = 20;
export const GENERO_ETIQUETA_MAX = 40;
export const TRABAJO_MAX = 60;
export const SITIO_MAX = 60;
export const EDAD_MIN = 18;
export const EDAD_MAX = 70;

export const VACIO: Borrador = {
  apodo: '',
  fecha: '',
  genero: null,
  generoEtiqueta: '',
  busco: [],
  edadMin: 27,
  edadMax: 38,
  rangoTocado: false,
  distancia: 30,
  ubicacion: null,
  idiomas: [{ code: 'es', level: 'NATIVE' }],
  rasgos: { largo: 'Mensajes largos', ritmo: 'Cuando puedo', rol: 'Pregunto mucho' },
  conversacion: 'A fondo',
  intencion: null,
  intereses: [],
  preguntas: [],
  trabajo: '',
  sitio: '',
  foto: null,
};

/**
 * De lo guardado al borrador, para editar sin volver a teclearlo todo.
 *
 * La foto se queda en null porque en el móvil no hay copia: la que vale está en
 * el servidor, y solo se sube otra si se elige otra.
 */
export function borradorDe(perfil: Profile): Borrador {
  return {
    apodo: perfil.nickname,
    fecha: fechaTecleada(perfil.birthDate),
    genero: perfil.gender,
    generoEtiqueta: perfil.genderLabel,
    busco: perfil.seeking,
    edadMin: perfil.ageMin,
    edadMax: perfil.ageMax,
    rangoTocado: true,
    distancia: perfil.maxDistanceKm,
    ubicacion: { latitude: perfil.latitude, longitude: perfil.longitude },
    idiomas: perfil.languages,
    rasgos: rasgosDe(perfil.sociability),
    conversacion: tipoDeProfundidad(perfil.conversationDepth),
    intencion: perfil.intent,
    intereses: perfil.interests,
    preguntas: perfil.prompts,
    trabajo: perfil.occupation,
    sitio: perfil.fromPlace,
    foto: null,
  };
}

/** Y del borrador a lo que viaja. Solo se llama con un borrador completo. */
export function datosDe(b: Borrador): ProfileData {
  return {
    nickname: b.apodo.trim(),
    birthDate: fechaISO(b.fecha),
    gender: b.genero!,
    genderLabel: b.generoEtiqueta.trim(),
    seeking: b.busco,
    ageMin: b.edadMin,
    ageMax: b.edadMax,
    maxDistanceKm: b.distancia,
    latitude: b.ubicacion!.latitude,
    longitude: b.ubicacion!.longitude,
    languages: b.idiomas,
    sociability: sociabilidadDe(b.rasgos),
    conversationDepth: profundidadDe(b.conversacion),
    intent: b.intencion!,
    interests: b.intereses,
    prompts: b.preguntas.map((p) => ({ ...p, answer: p.answer.trim() })),
    occupation: b.trabajo.trim(),
    fromPlace: b.sitio.trim(),
  };
}

/**
 * Qué le falta a cada bloque, en el mismo orden en que se preguntan. Un bloque
 * listo es uno que el backend aceptaría; el resto son decisiones con valor por
 * defecto y no bloquean.
 */
export const BLOQUES_LISTOS: ((b: Borrador) => boolean)[] = [
  (b) => {
    const edad = edadDe(b.fecha);
    return b.apodo.trim().length >= APODO_MIN && edad !== null && edad >= EDAD_MIN && b.genero !== null;
  },
  (b) => b.busco.length > 0,
  (b) => b.ubicacion !== null && b.idiomas.length > 0,
  (b) => b.intencion !== null,
  (b) =>
    b.intereses.length >= INTERESES_MIN &&
    b.preguntas.length === PREGUNTAS &&
    b.preguntas.every((p) => p.answer.trim().length > 0),
];

/** Todo menos la foto, que se trata aparte porque al editar ya hay una. */
export function completo(b: Borrador): boolean {
  return BLOQUES_LISTOS.every((listo) => listo(b));
}

/**
 * El rango de edad por defecto, a partir de la edad de quien se registra: un
 * 27-38 fijo es un número que nadie ha elegido y que acaba en el filtro como si
 * lo hubiera elegido.
 */
export function rangoSugerido(edad: number): { edadMin: number; edadMax: number } {
  return {
    edadMin: Math.max(EDAD_MIN, edad - 6),
    edadMax: Math.min(EDAD_MAX, edad + 6),
  };
}

/**
 * La foto no subió, pero el registro sí quedó guardado.
 *
 * Existe para que la pantalla no mienta: decir «no se pudo guardar el registro»
 * cuando el registro está guardado hace que alguien lo rehaga entero para nada,
 * y encima esconde el motivo real, que es lo único que ayuda a arreglarlo.
 */
export class FotoFallida extends Error {
  constructor(
    readonly perfil: Profile,
    causa: unknown,
  ) {
    super(causa instanceof Error ? causa.message : 'No se pudo subir la foto.');
  }
}

/**
 * Primero el registro y después la foto, en ese orden: si la foto fallara, el
 * registro ya está guardado y solo hay que reintentar la foto.
 */
export async function guardar(token: string, b: Borrador): Promise<Profile> {
  const perfil = await saveProfile(token, datosDe(b));

  if (b.foto) {
    try {
      await uploadPhoto(token, b.foto);
    } catch (causa) {
      throw new FotoFallida(perfil, causa);
    }
  }

  return perfil;
}
