import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { backendUrl, fetchHealth, type Health } from './src/api';

type State =
  | { kind: 'cargando' }
  | { kind: 'ok'; health: Health }
  | { kind: 'error'; mensaje: string };

export default function App() {
  const [state, setState] = useState<State>({ kind: 'cargando' });

  const comprobar = useCallback(async () => {
    setState({ kind: 'cargando' });
    try {
      setState({ kind: 'ok', health: await fetchHealth() });
    } catch (error) {
      setState({ kind: 'error', mensaje: error instanceof Error ? error.message : String(error) });
    }
  }, []);

  useEffect(() => {
    comprobar();
  }, [comprobar]);

  return (
    <View style={styles.container}>
      <Text style={styles.titulo}>Hablar primero, ver después</Text>
      <Text style={styles.subtitulo}>Paso 1 · el móvil habla con el servidor</Text>

      <View style={styles.tarjeta}>
        {state.kind === 'cargando' && <ActivityIndicator />}

        {state.kind === 'ok' && (
          <>
            <Text style={styles.ok}>Conectado</Text>
            <Fila etiqueta="Esquema" valor={state.health.schemaVersion} />
            <Fila etiqueta="Hora de la base de datos" valor={horaLocal(state.health.databaseTime)} />
            <Fila etiqueta="Servidor" valor={backendUrl()} />
          </>
        )}

        {state.kind === 'error' && (
          <>
            <Text style={styles.error}>Sin conexión con el servidor</Text>
            <Text style={styles.detalle}>{state.mensaje}</Text>
          </>
        )}
      </View>

      <Pressable style={styles.boton} onPress={comprobar}>
        <Text style={styles.botonTexto}>Volver a comprobar</Text>
      </Pressable>

      <StatusBar style="auto" />
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

/** El servidor manda siempre UTC; aquí se pasa a la hora del móvil. */
function horaLocal(iso: string): string {
  return new Date(iso).toLocaleString();
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#E9ECF1',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 16,
  },
  titulo: { fontSize: 24, fontWeight: '700', color: '#10151C', textAlign: 'center' },
  subtitulo: { fontSize: 14, color: '#6E7885', marginBottom: 8 },
  tarjeta: {
    width: '100%',
    backgroundColor: '#FBFCFD',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#C8D0DA',
    padding: 20,
    gap: 10,
    minHeight: 120,
    justifyContent: 'center',
  },
  ok: { fontSize: 18, fontWeight: '600', color: '#2C6A4F' },
  error: { fontSize: 18, fontWeight: '600', color: '#B23A2F' },
  detalle: { fontSize: 13, color: '#48525F' },
  fila: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  etiqueta: { fontSize: 13, color: '#6E7885' },
  valor: { fontSize: 13, color: '#10151C', fontWeight: '600', flexShrink: 1, textAlign: 'right' },
  boton: {
    backgroundColor: '#1E5F79',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
  },
  botonTexto: { color: '#FBFCFD', fontWeight: '600' },
});
