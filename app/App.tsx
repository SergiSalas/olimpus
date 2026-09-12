import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { fetchMe, type Me, type StartedSession } from './src/api';
import { HomeScreen } from './src/screens/HomeScreen';
import { LoginScreen } from './src/screens/LoginScreen';
import { clearToken, readToken, saveToken } from './src/session';
import { colors } from './src/theme';

type State = { kind: 'comprobando' } | { kind: 'fuera' } | { kind: 'dentro'; me: Me };

export default function App() {
  const [state, setState] = useState<State>({ kind: 'comprobando' });

  /**
   * Al abrir la app se mira si hay una llave guardada y si el servidor la sigue
   * aceptando: puede haber caducado o haber sido anulada desde el backend.
   */
  const comprobarSesion = useCallback(async () => {
    setState({ kind: 'comprobando' });
    const token = await readToken();
    if (!token) {
      setState({ kind: 'fuera' });
      return;
    }
    try {
      setState({ kind: 'dentro', me: await fetchMe(token) });
    } catch (error) {
      // La llave ya no vale: se borra y se vuelve a empezar.
      await clearToken();
      setState({ kind: 'fuera' });
    }
  }, []);

  useEffect(() => {
    comprobarSesion();
  }, [comprobarSesion]);

  async function entrar(sesion: StartedSession) {
    await saveToken(sesion.token);
    setState({
      kind: 'dentro',
      me: { accountId: sesion.accountId, email: sesion.email, createdAt: new Date().toISOString() },
    });
  }

  async function salir() {
    await clearToken();
    setState({ kind: 'fuera' });
  }

  if (state.kind === 'comprobando') {
    return (
      <View style={styles.centrado}>
        <ActivityIndicator />
        <StatusBar style="auto" />
      </View>
    );
  }

  return (
    <>
      {state.kind === 'dentro' ? (
        <HomeScreen me={state.me} onSalir={salir} />
      ) : (
        <LoginScreen onEntrar={entrar} />
      )}
      <StatusBar style="auto" />
    </>
  );
}

const styles = StyleSheet.create({
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
});
