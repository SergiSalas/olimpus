/**
 * Ayudas para escribir el email: validarlo, pillar erratas típicas del dominio
 * ("gmial.com") y completarlo de un toque. Sin nada de Expo, para poder
 * comprobarlo con node (email.check.ts).
 */

/** Los que casi todo el mundo usa: contra estos se buscan las erratas. */
const CONOCIDOS = [
  'gmail.com',
  'hotmail.com',
  'hotmail.es',
  'outlook.com',
  'outlook.es',
  'icloud.com',
  'me.com',
  'yahoo.com',
  'yahoo.es',
  'live.com',
  'protonmail.com',
];

/** Los que se ofrecen como botones para completar. */
export const DOMINIOS_RAPIDOS = ['gmail.com', 'icloud.com', 'hotmail.com', 'outlook.com'];

export function emailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim());
}

/** "sergi" o "sergi@gm" + "gmail.com" → "sergi@gmail.com". */
export function conDominio(email: string, dominio: string): string {
  const local = email.trim().split('@')[0];
  return `${local}@${dominio}`;
}

/**
 * Si el dominio se parece mucho a uno conocido sin serlo, el email corregido;
 * si no, null. "sergi@gmial.com" → "sergi@gmail.com".
 */
export function sugerirEmail(email: string): string | null {
  const limpio = email.trim().toLowerCase();
  const arroba = limpio.lastIndexOf('@');
  if (arroba < 1) return null;
  const local = limpio.slice(0, arroba);
  const dominio = limpio.slice(arroba + 1);
  if (!dominio || CONOCIDOS.includes(dominio)) return null;

  // "gmail" a secas: le falta el final.
  const completo = CONOCIDOS.find((c) => c.startsWith(`${dominio}.`));
  if (completo) return `${local}@${completo}`;

  let mejor: string | null = null;
  let menor = 3;
  for (const conocido of CONOCIDOS) {
    const d = distancia(dominio, conocido);
    if (d < menor) {
      menor = d;
      mejor = conocido;
    }
  }
  return mejor ? `${local}@${mejor}` : null;
}

/** Cuántas letras hay que cambiar, poner o quitar para ir de a a b. */
function distancia(a: string, b: string): number {
  let anterior = Array.from({ length: b.length + 1 }, (_, j) => j);
  for (let i = 1; i <= a.length; i++) {
    const actual = [i];
    for (let j = 1; j <= b.length; j++) {
      actual[j] = Math.min(
        anterior[j] + 1,
        actual[j - 1] + 1,
        anterior[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1),
      );
    }
    anterior = actual;
  }
  return anterior[b.length];
}
