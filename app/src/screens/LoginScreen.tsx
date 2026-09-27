import { useEffect, useRef, useState } from 'react';
import {
  Animated,
  KeyboardAvoidingView,
  Linking,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {
  ApiError,
  requestLoginCode,
  ultimoCodigoDePruebas,
  verifyLoginCode,
  type StartedSession,
} from '../api';
import { Boton, Entrada, Etiqueta, Flotar, Rebote } from '../components';
import { conDominio, DOMINIOS_RAPIDOS, emailValido, sugerirEmail } from '../email';
import { t } from '../i18n';
import { colors, fonts, text } from '../theme';

type Paso = 'email' | 'codigo';

const CIFRAS = 6;
/** Lo que hay que esperar para pedir otro código: diez seguidos no ayudan a nadie. */
const ESPERA_REENVIO_S = 30;

/**
 * Entrar y crear cuenta son lo mismo: un email y un código de seis cifras, sin
 * contraseñas. Quien no tenía cuenta la tiene al entrar; el servidor contesta
 * igual en los dos casos, así nadie puede usar la app para saber quién está.
 */
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
  const [aviso, setAviso] = useState<string | null>(null);
  const [espera, setEspera] = useState(0);
  const campoCodigo = useRef<TextInput>(null);
  const sacudida = useRef(new Animated.Value(0)).current;

  const limpio = email.trim();
  const valido = emailValido(limpio);
  const sugerencia = valido ? sugerirEmail(limpio) : null;
  const tecleado = limpio.includes('@') ? limpio.split('@')[1] : '';
  const rapidos =
    limpio && !valido ? DOMINIOS_RAPIDOS.filter((d) => d.startsWith(tecleado.toLowerCase())) : [];

  // La cuenta atrás para poder pedir otro código.
  useEffect(() => {
    if (espera <= 0) return;
    const reloj = setTimeout(() => setEspera((s) => s - 1), 1000);
    return () => clearTimeout(reloj);
  }, [espera]);

  async function pedirCodigo() {
    if (!valido || ocupado) return;
    setOcupado(true);
    setError(null);
    try {
      await requestLoginCode(limpio);
      setPaso('codigo');
      setCodigo('');
      setEspera(ESPERA_REENVIO_S);
    } catch (e) {
      setError(mensaje(e));
    } finally {
      setOcupado(false);
    }
  }

  async function reenviar() {
    setError(null);
    try {
      await requestLoginCode(limpio);
      setEspera(ESPERA_REENVIO_S);
      setAviso(t('login.reenviado'));
    } catch (e) {
      setError(mensaje(e));
    }
  }

  async function entrar(completo: string) {
    setOcupado(true);
    setError(null);
    try {
      onEntrar(await verifyLoginCode(limpio, completo));
    } catch (e) {
      setError(mensaje(e));
      setCodigo('');
      // Las casillas tiemblan y se vacían: se vuelve a escribir sin tocar nada más.
      Animated.sequence(
        [12, -12, 8, -8, 4, 0].map((x) =>
          Animated.timing(sacudida, { toValue: x, duration: 55, useNativeDriver: true }),
        ),
      ).start();
      campoCodigo.current?.focus();
    } finally {
      setOcupado(false);
    }
  }

  function teclearCodigo(texto: string) {
    const cifras = texto.replace(/\D/g, '').slice(0, CIFRAS);
    setCodigo(cifras);
    setAviso(null);
    // Con las seis, se entra solo.
    if (cifras.length === CIFRAS) entrar(cifras);
  }

  async function rellenarDePruebas() {
    try {
      const { code } = await ultimoCodigoDePruebas(limpio);
      teclearCodigo(code);
    } catch (e) {
      setError(mensaje(e));
    }
  }

  return (
    <KeyboardAvoidingView
      style={estilos.pantalla}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={estilos.arriba}>
        <Rebote
          style={estilos.redondo}
          onPress={() => (paso === 'codigo' ? setPaso('email') : onAtras?.())}>
          <Text style={estilos.flecha}>←</Text>
        </Rebote>
        <View style={estilos.puntos}>
          <View style={[estilos.punto, estilos.puntoHecho]} />
          <View style={[estilos.punto, paso === 'codigo' && estilos.puntoHecho]} />
        </View>
      </View>

      {paso === 'email' ? (
        <Entrada key="email" style={estilos.cuerpo}>
          <Flotar distancia={8} giro={6} style={estilos.emojiCaja}>
            <Text style={estilos.emoji}>✉️</Text>
          </Flotar>
          <Text style={estilos.titulo}>{t('login.email.titulo')}</Text>
          <Text style={[text.ayuda, { marginBottom: 20 }]}>{t('login.email.ayuda')}</Text>

          <View style={[estilos.campoEmail, valido && estilos.campoValido]}>
            <Text style={estilos.campoIcono}>✉️</Text>
            <TextInput
              style={estilos.campoTexto}
              value={email}
              onChangeText={(v) => {
                setEmail(v);
                setError(null);
              }}
              placeholder={t('login.emailEjemplo')}
              placeholderTextColor={colors.ink5}
              keyboardType="email-address"
              // Así el teclado del móvil propone el email de la persona.
              textContentType="emailAddress"
              autoComplete="email"
              autoCapitalize="none"
              autoCorrect={false}
              autoFocus
              returnKeyType="go"
              onSubmitEditing={pedirCodigo}
            />
            {valido && <Text style={estilos.check}>✓</Text>}
          </View>

          {sugerencia && (
            <Entrada style={estilos.sugerencia}>
              <Text style={estilos.sugerenciaTexto}>
                {t('login.quisiste', { email: sugerencia })}
              </Text>
              <Rebote style={estilos.sugerenciaSi} onPress={() => setEmail(sugerencia)}>
                <Text style={estilos.sugerenciaSiTexto}>{t('login.si')}</Text>
              </Rebote>
            </Entrada>
          )}

          {rapidos.length > 0 && (
            <View style={estilos.rapidos}>
              {rapidos.map((dominio) => (
                <Rebote
                  key={dominio}
                  style={estilos.rapido}
                  onPress={() => setEmail(conDominio(limpio, dominio))}>
                  <Text style={estilos.rapidoTexto}>@{dominio}</Text>
                </Rebote>
              ))}
            </View>
          )}

          <View style={{ height: 18 }} />
          <Boton
            texto={t('login.enviarCodigo')}
            onPress={pedirCodigo}
            ocupado={ocupado}
            deshabilitado={!valido}
          />
        </Entrada>
      ) : (
        <Entrada key="codigo" style={estilos.cuerpo}>
          <Flotar distancia={8} giro={6} style={estilos.emojiCaja}>
            <Text style={estilos.emoji}>📬</Text>
          </Flotar>
          <Text style={estilos.titulo}>{t('login.codigo.titulo')}</Text>
          <Text style={[text.ayuda, { marginBottom: 20 }]}>
            {t('login.codigo.enviado', { email: limpio })}{' '}
            <Text style={estilos.enlaceEnLinea} onPress={() => setPaso('email')}>
              {t('login.cambiar')}
            </Text>
          </Text>

          {/*
            Las casillas solo enseñan; el campo de verdad es invisible y va
            encima. Es la fila la que recoge el toque: en iPhone una vista con
            opacidad 0 no lo recibe.
          */}
          <Pressable onPress={() => campoCodigo.current?.focus()}>
            <Animated.View style={[estilos.casillas, { transform: [{ translateX: sacudida }] }]}>
              <TextInput
                ref={campoCodigo}
                pointerEvents="none"
                value={codigo}
                onChangeText={teclearCodigo}
                keyboardType="number-pad"
                // iOS saca el código del correo y lo propone encima del teclado.
                textContentType="oneTimeCode"
                autoComplete="one-time-code"
                maxLength={CIFRAS}
                autoFocus
                caretHidden
                style={estilos.campoInvisible}
              />
              {Array.from({ length: CIFRAS }, (_, i) => (
                <View
                  key={i}
                  pointerEvents="none"
                  style={[
                    estilos.casilla,
                    i === 3 && { marginLeft: 10 },
                    codigo.length === i && estilos.casillaActiva,
                    codigo[i] !== undefined && estilos.casillaLlena,
                  ]}>
                  <Text style={estilos.casillaTexto}>{codigo[i] ?? ''}</Text>
                </View>
              ))}
            </Animated.View>
          </Pressable>

          <Text style={estilos.caduca}>{t('login.caduca')}</Text>

          {Platform.OS === 'ios' && (
            <Rebote style={estilos.abrirCorreo} onPress={() => abrirCorreo(limpio)}>
              <Text style={estilos.abrirCorreoTexto}>{t('login.abrirCorreo')}</Text>
            </Rebote>
          )}

          {espera > 0 ? (
            <Text style={estilos.reenviarEspera}>
              {t('login.reenviarEn', { tiempo: `0:${String(espera).padStart(2, '0')}` })}
            </Text>
          ) : (
            <Pressable style={{ paddingVertical: 10 }} onPress={reenviar}>
              <Text style={estilos.reenviar}>{t('login.reenviar')}</Text>
            </Pressable>
          )}
          {aviso && <Text style={estilos.aviso}>{aviso}</Text>}

          {__DEV__ && (
            <Pressable style={{ paddingVertical: 8 }} onPress={rellenarDePruebas}>
              <Text style={estilos.pruebas}>{t('login.codigoPruebas')}</Text>
            </Pressable>
          )}
        </Entrada>
      )}

      {error && (
        <Entrada key={error}>
          <Text style={estilos.error}>{error}</Text>
        </Entrada>
      )}

      <View style={{ flex: 1 }} />
      <View style={estilos.pie}>
        <Etiqueta>{t('login.lema')}</Etiqueta>
      </View>
    </KeyboardAvoidingView>
  );
}

/**
 * Abre la app de correo donde está el código: Gmail primero si es un gmail, si
 * no Mail. Si la primera no está instalada, se prueba la otra.
 */
async function abrirCorreo(email: string) {
  const gmail = /@gmail\.com$/i.test(email);
  const intentos = gmail ? ['googlegmail://', 'message://'] : ['message://', 'googlegmail://'];
  for (const url of intentos) {
    try {
      await Linking.openURL(url);
      return;
    } catch {
      // No está: la siguiente.
    }
  }
}

function mensaje(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return t('comun.sinConexion');
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg, paddingHorizontal: 26, paddingTop: 58 },
  arriba: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  redondo: {
    width: 40,
    height: 40,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 18, color: colors.uva },
  puntos: { flexDirection: 'row', gap: 6 },
  punto: { width: 22, height: 8, borderRadius: 999, backgroundColor: colors.lineFuerte },
  puntoHecho: { backgroundColor: colors.accent },

  cuerpo: { paddingTop: 26 },
  emojiCaja: { alignSelf: 'flex-start', marginBottom: 10 },
  emoji: { fontSize: 52 },
  titulo: { ...text.titulo, fontSize: 32, lineHeight: 38, marginBottom: 8 },

  campoEmail: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 2,
    borderBottomWidth: 4,
    borderColor: colors.line,
    paddingHorizontal: 16,
  },
  campoValido: { borderColor: colors.menta },
  campoIcono: { fontSize: 18 },
  campoTexto: {
    flex: 1,
    paddingVertical: 16,
    fontFamily: fonts.sansMedia,
    fontSize: 17,
    color: colors.ink,
  },
  check: { fontFamily: fonts.displayFuerte, fontSize: 20, color: colors.menta },

  sugerencia: {
    marginTop: 10,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: '#FFF6D9',
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: colors.sol,
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  sugerenciaTexto: { flex: 1, fontFamily: fonts.sansMedia, fontSize: 14, color: colors.ink },
  sugerenciaSi: {
    backgroundColor: colors.sol,
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 7,
  },
  sugerenciaSiTexto: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.ink },

  rapidos: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  rapido: {
    backgroundColor: colors.surface2,
    borderRadius: 999,
    paddingHorizontal: 13,
    paddingVertical: 8,
  },
  rapidoTexto: { fontFamily: fonts.sansNegrita, fontSize: 13.5, color: colors.uva },

  enlaceEnLinea: { fontFamily: fonts.sansNegrita, color: colors.uva },
  casillas: { flexDirection: 'row', gap: 7 },
  casilla: {
    flex: 1,
    height: 64,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderBottomWidth: 5,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  casillaActiva: { borderColor: colors.accent, backgroundColor: colors.accentWash },
  casillaLlena: { borderColor: colors.uva },
  casillaTexto: { fontFamily: fonts.displayFuerte, fontSize: 26, color: colors.ink },
  campoInvisible: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    opacity: 0,
    zIndex: 1,
  },
  caduca: { ...text.ayuda, fontSize: 13, textAlign: 'center', marginTop: 12 },
  abrirCorreo: {
    marginTop: 20,
    alignSelf: 'center',
    backgroundColor: colors.ink,
    borderRadius: 999,
    borderBottomWidth: 3,
    borderColor: '#150A28',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  abrirCorreoTexto: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.onInk },
  reenviarEspera: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.ink4,
    textAlign: 'center',
    marginTop: 16,
  },
  reenviar: {
    fontFamily: fonts.sansNegrita,
    fontSize: 14,
    color: colors.accent,
    textAlign: 'center',
    marginTop: 6,
  },
  aviso: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.ok, textAlign: 'center' },
  pruebas: {
    fontFamily: fonts.sansNegrita,
    fontSize: 13.5,
    color: colors.uva,
    textAlign: 'center',
  },
  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 14,
    color: colors.error,
    textAlign: 'center',
    marginTop: 14,
  },
  pie: { paddingBottom: 38, alignItems: 'center' },
});
