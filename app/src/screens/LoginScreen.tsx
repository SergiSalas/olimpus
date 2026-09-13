import { useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, requestLoginCode, verifyLoginCode, type StartedSession } from '../api';
import { Boton, Campo, Etiqueta } from '../components';
import { colors, fonts, text } from '../theme';

type Paso = 'email' | 'codigo';

export function LoginScreen({
  onEntrar,
  onAtras,
}: {
  onEntrar: (sesion: StartedSession) => void;
  onAtras?: () => void;
}) {
  const [paso, setPaso] = useState<Paso>('email');
  const [email, setEmail] = useState('');
  const [codigo, setCodigo] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function pedirCodigo() {
    setOcupado(true);
    setError(null);
    try {
      await requestLoginCode(email);
      setPaso('codigo');
    } catch (e) {
      setError(mensaje(e));
    } finally {
      setOcupado(false);
    }
  }

  async function entrar() {
    setOcupado(true);
    setError(null);
    try {
      onEntrar(await verifyLoginCode(email, codigo));
    } catch (e) {
      setError(mensaje(e));
      setCodigo('');
    } finally {
      setOcupado(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={estilos.pantalla}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      {onAtras && (
        <Pressable style={estilos.redondo} onPress={onAtras}>
          <Text style={estilos.flecha}>←</Text>
        </Pressable>
      )}

      <View style={estilos.centro}>
        <Text style={estilos.marca}>Olimpus</Text>

        {paso === 'email' ? (
          <>
            <Text style={[text.titulo, { marginBottom: 8 }]}>Tu email</Text>
            <Text style={[text.ayuda, { marginBottom: 18 }]}>
              Te mandamos un código de seis cifras. No hay contraseñas que recordar ni que perder.
            </Text>
            <Campo
              valor={email}
              onChange={setEmail}
              placeholder="tu@email.com"
              keyboardType="email-address"
              onSubmit={pedirCodigo}
            />
            <View style={{ height: 16 }} />
            <Boton
              texto="Enviarme el código"
              onPress={pedirCodigo}
              ocupado={ocupado}
              deshabilitado={!email.includes('@')}
            />
          </>
        ) : (
          <>
            <Text style={[text.titulo, { marginBottom: 8 }]}>El código que te hemos enviado</Text>
            <Text style={[text.ayuda, { marginBottom: 18 }]}>
              A {email}. Caduca en diez minutos.
            </Text>
            <View style={estilos.casillas}>
              {[0, 1, 2, 3, 4, 5].map((i) => (
                <View
                  key={i}
                  style={[estilos.casilla, codigo.length === i && estilos.casillaActiva]}>
                  <Text style={estilos.casillaTexto}>{codigo[i] ?? ''}</Text>
                </View>
              ))}
            </View>
            <View style={estilos.campoInvisible}>
              <Campo
                valor={codigo}
                onChange={(v) => setCodigo(v.replace(/\D/g, '').slice(0, 6))}
                keyboardType="number-pad"
                maxLength={6}
                autoFocus
                onSubmit={entrar}
              />
            </View>
            <View style={{ height: 16 }} />
            <Boton
              texto="Entrar"
              onPress={entrar}
              ocupado={ocupado}
              deshabilitado={codigo.length < 6}
            />
            <Pressable
              style={{ paddingVertical: 14 }}
              onPress={() => {
                setPaso('email');
                setCodigo('');
                setError(null);
              }}>
              <Text style={estilos.enlace}>Cambiar de email o pedir otro código</Text>
            </Pressable>
          </>
        )}

        {error && <Text style={estilos.error}>{error}</Text>}
      </View>

      <View style={estilos.pie}>
        <Etiqueta>Hablar primero, ver después</Etiqueta>
      </View>
    </KeyboardAvoidingView>
  );
}

function mensaje(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return 'No se pudo conectar con el servidor.';
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 26, paddingTop: 58 },
  redondo: {
    width: 36,
    height: 36,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 17, color: colors.ink2 },
  centro: { flex: 1, justifyContent: 'center' },
  marca: {
    fontFamily: fonts.serif,
    fontSize: 26,
    color: colors.accent,
    marginBottom: 18,
  },
  casillas: { flexDirection: 'row', gap: 8 },
  casilla: {
    flex: 1,
    height: 64,
    borderRadius: 13,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  casillaActiva: { borderWidth: 1.5, borderColor: colors.accent },
  casillaTexto: { fontFamily: fonts.sansNegrita, fontSize: 22, color: colors.ink },
  /** El campo real: invisible, pero es quien abre el teclado y recibe las cifras. */
  campoInvisible: { height: 0, opacity: 0, overflow: 'hidden' },
  enlace: {
    fontFamily: fonts.sansMedia,
    fontSize: 14,
    color: colors.accent,
    textAlign: 'center',
  },
  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.error,
    textAlign: 'center',
    marginTop: 14,
  },
  pie: { paddingBottom: 38, alignItems: 'center' },
});
