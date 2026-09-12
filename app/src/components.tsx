import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { colors } from './theme';

/** Botón grande de acción, el que hace avanzar. */
export function Boton({
  texto,
  onPress,
  ocupado,
  deshabilitado,
}: {
  texto: string;
  onPress: () => void;
  ocupado?: boolean;
  deshabilitado?: boolean;
}) {
  const apagado = ocupado || deshabilitado;
  return (
    <Pressable
      style={[styles.boton, apagado && styles.apagado]}
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

/** Opción en forma de pastilla: se usa para género, intención, intereses e idiomas. */
export function Pastilla({
  texto,
  elegida,
  onPress,
  deshabilitada,
}: {
  texto: string;
  elegida: boolean;
  onPress: () => void;
  deshabilitada?: boolean;
}) {
  return (
    <Pressable
      style={[
        styles.pastilla,
        elegida && styles.pastillaElegida,
        deshabilitada && !elegida && styles.apagado,
      ]}
      onPress={onPress}
      disabled={deshabilitada && !elegida}>
      <Text style={[styles.pastillaTexto, elegida && styles.pastillaTextoElegido]}>{texto}</Text>
    </Pressable>
  );
}

/** Escala de 1 a 5 con una frase en cada extremo. */
export function Escala({
  valor,
  onChange,
  izquierda,
  derecha,
}: {
  valor: number;
  onChange: (v: number) => void;
  izquierda: string;
  derecha: string;
}) {
  return (
    <View style={{ gap: 10 }}>
      <View style={styles.fila}>
        {[1, 2, 3, 4, 5].map((n) => (
          <Pressable
            key={n}
            style={[styles.punto, valor === n && styles.puntoElegido]}
            onPress={() => onChange(n)}>
            <Text style={[styles.puntoTexto, valor === n && styles.puntoTextoElegido]}>{n}</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.extremos}>
        <Text style={styles.extremo}>{izquierda}</Text>
        <Text style={[styles.extremo, { textAlign: 'right' }]}>{derecha}</Text>
      </View>
    </View>
  );
}

export function Campo({
  valor,
  onChange,
  placeholder,
  maxLength,
  multiline,
  keyboardType,
  ancho,
}: {
  valor: string;
  onChange: (v: string) => void;
  placeholder?: string;
  maxLength?: number;
  multiline?: boolean;
  keyboardType?: 'default' | 'number-pad';
  ancho?: number;
}) {
  return (
    <TextInput
      style={[styles.campo, multiline && styles.campoLargo, ancho ? { width: ancho } : null]}
      value={valor}
      onChangeText={onChange}
      placeholder={placeholder}
      placeholderTextColor={colors.ink3}
      maxLength={maxLength}
      multiline={multiline}
      keyboardType={keyboardType ?? 'default'}
      autoCapitalize={multiline ? 'sentences' : 'words'}
    />
  );
}

/** Barra de avance del registro: cuántas preguntas quedan. */
export function Progreso({ paso, total }: { paso: number; total: number }) {
  return (
    <View style={styles.progresoFondo}>
      <View style={[styles.progresoBarra, { width: `${(paso / total) * 100}%` }]} />
    </View>
  );
}

export const styles = StyleSheet.create({
  boton: {
    backgroundColor: colors.accent,
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
  },
  botonTexto: { color: colors.surface, fontWeight: '600', fontSize: 16 },
  apagado: { opacity: 0.4 },
  pastilla: {
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 9,
  },
  pastillaElegida: { backgroundColor: colors.accent, borderColor: colors.accent },
  pastillaTexto: { color: colors.ink2, fontSize: 14 },
  pastillaTextoElegido: { color: colors.surface, fontWeight: '600' },
  fila: { flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  punto: {
    flex: 1,
    aspectRatio: 1,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  puntoElegido: { backgroundColor: colors.accent, borderColor: colors.accent },
  puntoTexto: { color: colors.ink2, fontSize: 16 },
  puntoTextoElegido: { color: colors.surface, fontWeight: '700' },
  extremos: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  extremo: { fontSize: 12, color: colors.ink3, flex: 1 },
  campo: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 17,
    color: colors.ink,
    backgroundColor: colors.surface,
  },
  campoLargo: { minHeight: 90, textAlignVertical: 'top' },
  progresoFondo: {
    height: 4,
    backgroundColor: colors.line,
    borderRadius: 2,
    overflow: 'hidden',
  },
  progresoBarra: { height: 4, backgroundColor: colors.accent },
});
