/**
 * De lo que se pregunta a lo que se guarda.
 *
 * El diseño pregunta con palabras («contesto al momento», «a fondo») porque a
 * una persona se le pregunta así. El algoritmo trabaja con dos números del 1 al
 * 5. Esta es la única traducción entre los dos mundos, y está aquí sola para
 * que se vea y se pueda discutir.
 *
 * Ojo: la traducción pierde matiz. Dos combinaciones distintas pueden acabar en
 * el mismo número. Si algún día el matiz importa, lo que hay que hacer es
 * guardar las respuestas tal cual y cambiar el algoritmo, no retocar esto.
 */

/** El término medio de los tres rasgos, que se escribe igual en los tres. */
export const DEPENDE = 'Depende';

export type Rasgos = {
  /** Al escribir. */
  largo: 'Mensajes largos' | typeof DEPENDE | 'Cortos y seguidos';
  /** Mi ritmo. */
  ritmo: 'Cuando puedo' | typeof DEPENDE | 'Contesto al momento';
  /** En una charla. */
  rol: 'Escucho más' | typeof DEPENDE | 'Pregunto mucho';
};

export type TipoConversacion = 'A fondo' | 'Ligera y con humor' | 'Curiosa' | 'Práctica';

/**
 * Las opciones van de menos a más empuje, y la de en medio existe porque sin
 * ella la escala 1-5 tenía agujeros: con tres binarias nunca salía un 3.
 */
export const RASGOS = [
  {
    clave: 'largo' as const,
    titulo: 'Al escribir',
    opciones: ['Mensajes largos', DEPENDE, 'Cortos y seguidos'],
  },
  {
    clave: 'ritmo' as const,
    titulo: 'Mi ritmo',
    opciones: ['Cuando puedo', DEPENDE, 'Contesto al momento'],
  },
  {
    clave: 'rol' as const,
    titulo: 'En una charla',
    opciones: ['Escucho más', DEPENDE, 'Pregunto mucho'],
  },
];

/** Lo que empuja la conversación hacia delante, en cada rasgo. */
const EMPUJA = {
  largo: 'Cortos y seguidos',
  ritmo: 'Contesto al momento',
  rol: 'Pregunto mucho',
} as const;

export const CONVERSACIONES: { valor: TipoConversacion; pista: string }[] = [
  { valor: 'A fondo', pista: 'Pocos temas, bien tratados' },
  { valor: 'Ligera y con humor', pista: 'Sin ponerse intenso' },
  { valor: 'Curiosa', pista: 'Preguntas raras, hipótesis' },
  { valor: 'Práctica', pista: 'Planes, ciudad, qué hacer' },
];

/**
 * Cuánto empujan las tres respuestas, de 0 a 3. El término medio cuenta medio
 * punto, que es lo que permite que salgan los cinco valores de la escala.
 */
export function sociabilidadDe(rasgos: Rasgos): number {
  const empuje = (valor: string, alto: string) =>
    valor === alto ? 1 : valor === DEPENDE ? 0.5 : 0;

  const empujan =
    empuje(rasgos.largo, EMPUJA.largo) +
    empuje(rasgos.ritmo, EMPUJA.ritmo) +
    empuje(rasgos.rol, EMPUJA.rol);

  return Math.round(1 + (4 * empujan) / 3);
}

/** De la clase de conversación que gusta al 1-5 de profundidad. */
export function profundidadDe(tipo: TipoConversacion): number {
  switch (tipo) {
    case 'A fondo':
      return 5;
    case 'Curiosa':
      return 4;
    case 'Práctica':
      return 3;
    case 'Ligera y con humor':
      return 1;
  }
}

/** Y de vuelta, para poder enseñar en el perfil lo que se respondió. */
export function tipoDeProfundidad(valor: number): TipoConversacion {
  if (valor >= 5) return 'A fondo';
  if (valor === 4) return 'Curiosa';
  if (valor === 3) return 'Práctica';
  return 'Ligera y con humor';
}

/** Cuánto empuje hace falta para cada valor de la escala. */
const EMPUJE_DE = [0, 0, 1, 1.5, 2, 3];

/**
 * La vuelta de {@link sociabilidadDe}, para poder repasar y cambiar lo que se
 * respondió sin volver a empezar de cero.
 *
 * No es una inversa de verdad, porque la ida no lo permite: tres respuestas de
 * tres opciones son veintisiete combinaciones y la sociabilidad solo tiene
 * cinco valores. De todas las que dan el mismo número se elige una, repartiendo
 * el empuje en el orden en que se preguntan. Quien edite su registro puede ver
 * un rasgo distinto del que marcó, aunque el número que usa el algoritmo sea el
 * mismo.
 */
export function rasgosDe(sociabilidad: number): Rasgos {
  const dentro = Math.min(5, Math.max(1, Math.round(sociabilidad)));
  let queda = EMPUJE_DE[dentro];

  function reparte<T extends string>(alto: T, bajo: T): T | typeof DEPENDE {
    if (queda >= 1) {
      queda -= 1;
      return alto;
    }
    if (queda >= 0.5) {
      queda -= 0.5;
      return DEPENDE;
    }
    return bajo;
  }

  return {
    largo: reparte('Cortos y seguidos', 'Mensajes largos'),
    ritmo: reparte('Contesto al momento', 'Cuando puedo'),
    rol: reparte('Pregunto mucho', 'Escucho más'),
  };
}

/**
 * La fecha se teclea DDMMAAAA y se guarda AAAA-MM-DD.
 *
 * Las dos direcciones viven juntas porque son inversas. Si una cambiara sin la
 * otra, editar el registro estropearía la fecha de nacimiento, y de ahí salen
 * la edad y el filtro de mayores de 18.
 */
export function fechaISO(tecleada: string): string {
  return `${tecleada.slice(4)}-${tecleada.slice(2, 4)}-${tecleada.slice(0, 2)}`;
}

export function fechaTecleada(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}${mes}${anio}`;
}

/** Del DDMMAAAA tecleado a la edad de hoy, o null si aún no está completa. */
export function edadDe(fecha: string): number | null {
  if (fecha.length < 8) return null;
  const dia = Number(fecha.slice(0, 2));
  const mes = Number(fecha.slice(2, 4));
  const anio = Number(fecha.slice(4));
  if (dia < 1 || dia > 31 || mes < 1 || mes > 12 || anio < 1920) return -1;

  const nacimiento = new Date(anio, mes - 1, dia);
  if (nacimiento.getDate() !== dia || nacimiento.getMonth() !== mes - 1) return -1;

  const hoy = new Date();
  let edad = hoy.getFullYear() - anio;
  const cumpleEsteAno = new Date(hoy.getFullYear(), mes - 1, dia);
  if (hoy < cumpleEsteAno) edad--;
  return edad;
}
