import {
  Figtree_400Regular,
  Figtree_500Medium,
  Figtree_600SemiBold,
  Figtree_700Bold,
} from '@expo-google-fonts/figtree';
import { Fredoka_600SemiBold, Fredoka_700Bold } from '@expo-google-fonts/fredoka';
import { useFonts } from 'expo-font';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { fetchMe, fetchProfile, type Me, type Profile, type StartedSession } from './src/api';
import { ChatScreen } from './src/screens/ChatScreen';
import { EditarScreen } from './src/screens/EditarScreen';
import { HomeScreen } from './src/screens/HomeScreen';
import { IntroScreen } from './src/screens/IntroScreen';
import { LoginScreen } from './src/screens/LoginScreen';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { TodayScreen } from './src/screens/TodayScreen';
import { activarAvisos } from './src/avisos';
import { clearToken, readToken, saveToken } from './src/session';
import { Entrada } from './src/components';
import { colors } from './src/theme';

type State =
  | { kind: 'comprobando' }
  /** Los tres paneles de bienvenida: explican la app antes de pedir nada. */
  | { kind: 'intro' }
  | { kind: 'fuera' }
  /** Con sesión pero sin registro hecho: el único camino es terminarlo. */
  | { kind: 'registro'; token: string; me: Me }
  /** Cambiar lo ya respondido, en una sola pantalla y sin rehacer nada. */
  | { kind: 'editar'; token: string; me: Me; perfil: Profile }
  | { kind: 'hoy'; token: string; me: Me; perfil: Profile }
  | { kind: 'chat'; token: string; me: Me; perfil: Profile; conversationId: string }
  | { kind: 'miRegistro'; token: string; me: Me; perfil: Profile };

export default function App() {
  const [state, setState] = useState<State>({ kind: 'comprobando' });

  const [fuentesListas] = useFonts({
    Figtree_400Regular,
    Figtree_500Medium,
    Figtree_600SemiBold,
    Figtree_700Bold,
    Fredoka_600SemiBold,
    Fredoka_700Bold,
  });

  /**
   * Al abrir la app: ¿hay llave guardada?, ¿la sigue aceptando el servidor?,
   * ¿tiene ya registro? De esas tres respuestas sale la pantalla. Quien no
   * tiene sesión ve primero la bienvenida.
   */
  const arrancar = useCallback(async () => {
    setState({ kind: 'comprobando' });
    const token = await readToken();
    if (!token) {
      setState({ kind: 'intro' });
      return;
    }
    try {
      const me = await fetchMe(token);
      const perfil = await fetchProfile(token);
      setState(perfil ? { kind: 'hoy', token, me, perfil } : { kind: 'registro', token, me });
    } catch {
      // La llave ya no vale (caducada o anulada): se borra y se empieza de nuevo.
      await clearToken();
      setState({ kind: 'intro' });
    }
  }, []);

  useEffect(() => {
    arrancar();
  }, [arrancar]);

  /**
   * Los avisos se piden una vez, cuando ya hay sesión y registro: pedir permiso
   * nada más abrir, antes de que se entienda para qué sirve, es la mejor forma
   * de que digan que no.
   */
  useEffect(() => {
    if (state.kind !== 'hoy') return;
    activarAvisos(state.token).catch(() => {
      // Sin avisos la app funciona igual; no hay nada que contarle a nadie.
    });
  }, [state]);

  async function entrar(sesion: StartedSession) {
    await saveToken(sesion.token);
    await arrancar();
  }

  async function salir() {
    await clearToken();
    setState({ kind: 'intro' });
  }

  if (!fuentesListas || state.kind === 'comprobando') {
    return (
      <View style={styles.centrado}>
        <ActivityIndicator color={colors.accent} />
        <StatusBar style="dark" />
      </View>
    );
  }

  return (
    // Cada cambio de pantalla entra con un pequeño muelle.
    <Entrada key={state.kind} style={styles.pantalla}>
      {state.kind === 'intro' && <IntroScreen onEmpezar={() => setState({ kind: 'fuera' })} />}

      {state.kind === 'fuera' && (
        <LoginScreen onEntrar={entrar} onAtras={() => setState({ kind: 'intro' })} />
      )}

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
          onAbrirChat={(conversationId) => setState({ ...state, kind: 'chat', conversationId })}
          onPerfil={() => setState({ ...state, kind: 'miRegistro' })}
        />
      )}

      {state.kind === 'chat' && (
        <ChatScreen
          token={state.token}
          conversationId={state.conversationId}
          yo={state.perfil.nickname}
          onVolver={() =>
            setState({ kind: 'hoy', token: state.token, me: state.me, perfil: state.perfil })
          }
        />
      )}

      {state.kind === 'editar' && (
        <EditarScreen
          token={state.token}
          perfil={state.perfil}
          onVolver={() => setState({ ...state, kind: 'miRegistro' })}
          onGuardado={(perfil) =>
            setState({ kind: 'miRegistro', token: state.token, me: state.me, perfil })
          }
        />
      )}

      {state.kind === 'miRegistro' && (
        <HomeScreen
          me={state.me}
          perfil={state.perfil}
          token={state.token}
          onVolver={() => setState({ ...state, kind: 'hoy' })}
          onEditar={() => setState({ ...state, kind: 'editar' })}
          onSalir={salir}
        />
      )}

      <StatusBar style="dark" />
    </Entrada>
  );
}

const styles = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
});
