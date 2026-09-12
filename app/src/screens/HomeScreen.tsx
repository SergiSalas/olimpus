import { Pressable, StyleSheet, Text, View } from 'react-native';
import { type Me } from '../api';
import { colors } from '../theme';

/**
 * Pantalla provisional de "ya estas dentro". En el paso 2b la sustituye el
 * registro de las diez preguntas.
 */
export function HomeScreen({ me, onSalir }: { me: Me; onSalir: () => void }) {
  return (
    <View style={styles.container}>
      <Text style={styles.titulo}>Olimpus</Text>
      <View style={styles.tarjeta}>
        <Text style={styles.dentro}>Ya estás dentro</Text>
        <Fila etiqueta="Email" valor={me.email} />
        <Fila etiqueta="Cuenta creada" valor={new Date(me.createdAt).toLocaleString()} />
        <Text style={styles.ayuda}>
          Lo siguiente será el registro: diez preguntas y una foto que nadie verá hasta que una
          conversación llegue al nivel 3.
        </Text>
      </View>
      <Pressable style={styles.boton} onPress={onSalir}>
        <Text style={styles.botonTexto}>Cerrar sesión</Text>
      </Pressable>
    </View>
  );
}

function Fila({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <View style={styles.fila}>
      <Text style={styles.etiqueta}>{etiqueta}</Text>
      <Text style={styles.valor}>{valor}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
    justifyContent: 'center',
    padding: 24,
    gap: 16,
  },
  titulo: {
    fontSize: 30,
    fontWeight: '700',
    color: colors.ink,
    textAlign: 'center',
    letterSpacing: 1,
  },
  tarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 20,
    gap: 10,
  },
  dentro: { fontSize: 18, fontWeight: '600', color: colors.ok },
  fila: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  etiqueta: { fontSize: 13, color: colors.ink3 },
  valor: { fontSize: 13, color: colors.ink, fontWeight: '600', flexShrink: 1, textAlign: 'right' },
  ayuda: { fontSize: 13, color: colors.ink3, marginTop: 4 },
  boton: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  botonTexto: { color: colors.ink2, fontWeight: '600' },
});
