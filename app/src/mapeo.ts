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

export type Rasgos = {
  /** Al escribir. */
  largo: 'Mensajes largos' | 'Cortos y seguidos';
  /** Mi ritmo. */
  ritmo: 'Contesto al momento' | 'Cuando puedo';
  /** En una charla. */
  rol: 'Pregunto mucho' | 'Escucho más';
};

export type TipoConversacion = 'A fondo' | 'Ligera y con humor' | 'Curiosa' | 'Práctica';

export const RASGOS = [
  { clave: 'largo' as const, titulo: 'Al escribir', opciones: ['Mensajes largos', 'Cortos y seguidos'] },
  { clave: 'ritmo' as const, titulo: 'Mi ritmo', opciones: ['Contesto al momento', 'Cuando puedo'] },
  { clave: 'rol' as const, titulo: 'En una charla', opciones: ['Pregunto mucho', 'Escucho más'] },
];

export const CONVERSACIONES: { valor: TipoConversacion; pista: string }[] = [
  { valor: 'A fondo', pista: 'Pocos temas, bien tratados' },
  { valor: 'Ligera y con humor', pista: 'Sin ponerse intenso' },
  { valor: 'Curiosa', pista: 'Preguntas raras, hipótesis' },
  { valor: 'Práctica', pista: 'Planes, ciudad, qué hacer' },
];

/**
 * Cuántas de las tres respuestas empujan la conversación hacia delante.
 * Cero da 1 (le cuesta arrancar); las tres dan 5 (habla con cualquiera).
 */
export function sociabilidadDe(rasgos: Rasgos): number {
  let empujan = 0;
  if (rasgos.largo === 'Cortos y seguidos') empujan++; // ida y vuelta más viva
  if (rasgos.ritmo === 'Contesto al momento') empujan++;
  if (rasgos.rol === 'Pregunto mucho') empujan++;

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
