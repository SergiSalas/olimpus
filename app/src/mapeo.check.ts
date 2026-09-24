/**
 * Comprobación de la traducción, sin marco de pruebas:
 *
 *   node --experimental-strip-types src/mapeo.check.ts
 *
 * Lo que hay que garantizar es la ida y vuelta: quien edita su registro tiene
 * que salir con los mismos números con los que entró, o el algoritmo vería
 * cambiar a alguien que no ha tocado nada.
 */
import assert from 'node:assert/strict';
import {
  CONVERSACIONES,
  RASGOS,
  edadDe,
  fechaISO,
  fechaTecleada,
  profundidadDe,
  rasgosDe,
  sociabilidadDe,
  tipoDeProfundidad,
  type Rasgos,
} from './mapeo.ts';

// Las veintisiete combinaciones de los tres rasgos, y la vuelta de cada una.
const salen = new Set<number>();

for (const largo of RASGOS[0].opciones) {
  for (const ritmo of RASGOS[1].opciones) {
    for (const rol of RASGOS[2].opciones) {
      const rasgos = { largo, ritmo, rol } as Rasgos;
      const sociabilidad = sociabilidadDe(rasgos);
      salen.add(sociabilidad);

      assert.ok(sociabilidad >= 1 && sociabilidad <= 5, `fuera de escala: ${sociabilidad}`);
      assert.equal(
        sociabilidadDe(rasgosDe(sociabilidad)),
        sociabilidad,
        `la vuelta cambia el número para ${JSON.stringify(rasgos)}`,
      );
    }
  }
}

// La razón de ser de la tercera opción: sin ella la escala nunca daba un 3.
assert.deepEqual([...salen].sort(), [1, 2, 3, 4, 5], 'la escala tiene agujeros');

for (const conversacion of CONVERSACIONES) {
  assert.equal(tipoDeProfundidad(profundidadDe(conversacion.valor)), conversacion.valor);
}

assert.equal(fechaISO('20031995'), '1995-03-20');
assert.equal(fechaTecleada('1995-03-20'), '20031995');
assert.equal(fechaTecleada(fechaISO('01012000')), '01012000');

// La edad sale de la fecha, y de ella depende el filtro de mayores de 18.
const hoy = new Date();
const dia = String(hoy.getDate()).padStart(2, '0');
const mes = String(hoy.getMonth() + 1).padStart(2, '0');

assert.equal(edadDe(`${dia}${mes}${hoy.getFullYear() - 30}`), 30, 'cumple hoy');
assert.equal(edadDe(`${dia}${mes}${hoy.getFullYear() - 17}`), 17, 'todavía menor');
assert.equal(edadDe('2003'), null, 'incompleta');
assert.equal(edadDe('31021995'), -1, 'el 31 de febrero no existe');
assert.equal(edadDe('00011995'), -1, 'no hay día cero');
assert.equal(edadDe('01131995'), -1, 'no hay mes trece');

console.log('mapeo: ida y vuelta correctas');
