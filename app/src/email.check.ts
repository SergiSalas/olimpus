/**
 * Comprobación de las ayudas del email, sin marco de pruebas:
 *
 *   node --experimental-strip-types src/email.check.ts
 */
import assert from 'node:assert/strict';
import { conDominio, emailValido, sugerirEmail } from './email.ts';

// Erratas típicas: se corrigen.
assert.equal(sugerirEmail('sergi@gmial.com'), 'sergi@gmail.com');
assert.equal(sugerirEmail('sergi@gmail.con'), 'sergi@gmail.com');
assert.equal(sugerirEmail('sergi@hotmial.com'), 'sergi@hotmail.com');
assert.equal(sugerirEmail('sergi@gmail'), 'sergi@gmail.com');
assert.equal(sugerirEmail('Sergi@GMIAL.COM'), 'sergi@gmail.com');

// Lo que está bien, o es de un dominio propio, se deja en paz.
assert.equal(sugerirEmail('sergi@gmail.com'), null);
assert.equal(sugerirEmail('sergi@universidad.edu'), null);
assert.equal(sugerirEmail('sergi@empresa.cat'), null);
assert.equal(sugerirEmail('sergi'), null);
assert.equal(sugerirEmail('sergi@'), null);

assert.equal(emailValido('sergi@gmail.com'), true);
assert.equal(emailValido(' sergi@gmail.com '), true);
assert.equal(emailValido('sergi@gmail'), false);
assert.equal(emailValido('sergi gmail.com'), false);

assert.equal(conDominio('sergi', 'gmail.com'), 'sergi@gmail.com');
assert.equal(conDominio('sergi@gm', 'gmail.com'), 'sergi@gmail.com');

console.log('email: sugerencias correctas');
