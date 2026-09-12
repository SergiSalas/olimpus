import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { fetchMe, fetchProfile, type Me, type Profile, type StartedSession } from './src/api';
import { HomeScreen } from './src/screens/HomeScreen';
import { LoginScreen } from './src/screens/LoginScreen';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { TodayScreen } from './src/screens/TodayScreen';
import { clearToken, readToken, saveToken } from './src/session';
import { colors } from './src/theme';

type State =
  | { kind: 'comprobando' }
  | { kind: 'fuera' }
  /** Con sesión pero sin registro hecho: el único camino es terminarlo. */
  | { kind: 'registro'; token: string; me: Me }
  | { kind: 'hoy'; token: string; me: Me; perfil: Profile }
  | { kind: 'miRegistro'; token: string; me: Me; perfil: Profile };

export default function App() {
  const [state, setState] = useState<State>({ kind: 'comprobando' });

  /**
   * Al abrir la app: ¿hay llave guardada?, ¿la sigue aceptando el servidor?,
   * ¿tiene ya registro? De esas tres respuestas sale la pantalla.
   */
  const arrancar = useCallback(async () => {
    setState({ kind: 'comprobando' });
    const token = await readToken();
    if (!token) {
      setState({ kind: 'fuera' });
      return;
    }
    try {
      const me = await fetchMe(token);
      const perfil = await fetchProfile(token);
      setState(perfil ? { kind: 'hoy', token, me, perfil } : { kind: 'registro', token, me });
    } catch {
      // La llave ya no vale (caducada o anulada): se borra y se empieza de nuevo.
      await clearToken();
      setState({ kind: 'fuera' });
    }
  }, []);

  useEffect(() => {
    arrancar();
  }, [arrancar]);

  async function entrar(sesion: StartedSession) {
    await saveToken(sesion.token);
    await arrancar();
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
      {state.kind === 'fuera' && <LoginScreen onEntrar={entrar} />}

      {state.kind === 'registro' && (
        <OnboardingScreen
          token={state.token}
          onTerminado={(perfil) =>
            setState({ kind: 'hoy', token: state.token, me: state.me, perfil })
          }
        />
      )}

      {state.kind === 'hoy' && (
        <TodayScreen
          token={state.token}
          perfil={state.perfil}
          onPerfil={() => setState({ ...state, kind: 'miRegistro' })}
          onSalir={salir}
        />
      )}

      {state.kind === 'miRegistro' && (
        <HomeScreen
          me={state.me}
          perfil={state.perfil}
          onVolver={() => setState({ ...state, kind: 'hoy' })}
          onEditar={() => setState({ kind: 'registro', token: state.token, me: state.me })}
          onSalir={salir}
        />
      )}

      <StatusBar style="auto" />
    </>
  );
}

const styles = StyleSheet.create({
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
});
