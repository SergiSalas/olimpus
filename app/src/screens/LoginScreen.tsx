import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { ApiError, requestLoginCode, verifyLoginCode, type StartedSession } from '../api';
import { colors } from '../theme';

type Paso = 'email' | 'codigo';

export function LoginScreen({ onEntrar }: { onEntrar: (sesion: StartedSession) => void }) {
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
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Text style={styles.titulo}>Olimpus</Text>
      <Text style={styles.lema}>Hablar primero, ver después</Text>

      {paso === 'email' ? (
        <View style={styles.tarjeta}>
          <Text style={styles.etiqueta}>Tu email</Text>
          <TextInput
            style={styles.input}
            value={email}
            onChangeText={setEmail}
            placeholder="tu@email.com"
            placeholderTextColor={colors.ink3}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            autoComplete="email"
            editable={!ocupado}
            onSubmitEditing={pedirCodigo}
            returnKeyType="send"
          />
          <Text style={styles.ayuda}>
            Te enviamos un código de 6 cifras. No hay contraseñas que recordar.
          </Text>
          <Boton texto="Enviarme el código" onPress={pedirCodigo} ocupado={ocupado} />
        </View>
      ) : (
        <View style={styles.tarjeta}>
          <Text style={styles.etiqueta}>El código que te hemos enviado</Text>
          <TextInput
            style={[styles.input, styles.inputCodigo]}
            value={codigo}
            onChangeText={(texto) => setCodigo(texto.replace(/\D/g, '').slice(0, 6))}
            placeholder="000000"
            placeholderTextColor={colors.ink3}
            keyboardType="number-pad"
            autoComplete="one-time-code"
            maxLength={6}
            editable={!ocupado}
            autoFocus
            onSubmitEditing={entrar}
          />
          <Text style={styles.ayuda}>Enviado a {email}. Caduca en 10 minutos.</Text>
          <Boton
            texto="Entrar"
            onPress={entrar}
            ocupado={ocupado}
            deshabilitado={codigo.length < 6}
          />
          <Pressable
            onPress={() => {
              setPaso('email');
              setCodigo('');
              setError(null);
            }}>
            <Text style={styles.enlace}>Cambiar de email o pedir otro código</Text>
          </Pressable>
        </View>
      )}

      {error && <Text style={styles.error}>{error}</Text>}
    </KeyboardAvoidingView>
  );
}

function Boton({
  texto,
  onPress,
  ocupado,
  deshabilitado,
}: {
  texto: string;
  onPress: () => void;
  ocupado: boolean;
  deshabilitado?: boolean;
}) {
  const apagado = ocupado || deshabilitado;
  return (
    <Pressable
      style={[styles.boton, apagado && styles.botonApagado]}
      onPress={onPress}
      disabled={apagado}>
      {ocupado ? (
        <ActivityIndicator color={colors.surface} />
      ) : (
        <Text style={styles.botonTexto}>{texto}</Text>
      )}
    </Pressable>
  );
}

function mensaje(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  return 'No se pudo conectar con el servidor.';
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
    justifyContent: 'center',
    padding: 24,
    gap: 12,
  },
  titulo: {
    fontSize: 34,
    fontWeight: '700',
    color: colors.ink,
    textAlign: 'center',
    letterSpacing: 1,
  },
  lema: { fontSize: 16, color: colors.ink2, textAlign: 'center', marginBottom: 16 },
  tarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 20,
    gap: 12,
  },
  etiqueta: { fontSize: 13, color: colors.ink3, fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 17,
    color: colors.ink,
    backgroundColor: colors.bg,
  },
  inputCodigo: { fontSize: 26, letterSpacing: 8, textAlign: 'center' },
  ayuda: { fontSize: 13, color: colors.ink3 },
  boton: {
    backgroundColor: colors.accent,
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
  },
  botonApagado: { opacity: 0.45 },
  botonTexto: { color: colors.surface, fontWeight: '600', fontSize: 16 },
  enlace: { color: colors.accent, fontSize: 13, textAlign: 'center' },
  error: { color: colors.error, textAlign: 'center', fontSize: 14 },
});
