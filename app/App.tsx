import {
  Figtree_400Regular,
  Figtree_500Medium,
  Figtree_600SemiBold,
  Figtree_700Bold,
} from '@expo-google-fonts/figtree';
import {
  InstrumentSerif_400Regular,
  InstrumentSerif_400Regular_Italic,
} from '@expo-google-fonts/instrument-serif';
import { useFonts } from 'expo-font';
import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { fetchMe, fetchProfile, type Me, type Profile, type StartedSession } from './src/api';
import { ChatScreen } from './src/screens/ChatScreen';
import { HomeScreen } from './src/screens/HomeScreen';
import { IntroScreen } from './src/screens/IntroScreen';
import { LoginScreen } from './src/screens/LoginScreen';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { TodayScreen } from './src/screens/TodayScreen';
import { clearToken, readToken, saveToken } from './src/session';
import { colors } from './src/theme';

type State =
  | { kind: 'comprobando' }
  /** Los tres paneles de bienvenida: explican la app antes de pedir nada. */
  | { kind: 'intro' }
  | { kind: 'fuera' }
  /** Con sesión pero sin registro hecho: el único camino es terminarlo. */
  | { kind: 'registro'; token: string; me: Me }
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
    InstrumentSerif_400Regular,
    InstrumentSerif_400Regular_Italic,
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
    <>
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
          onSalir={salir}
        />
      )}

      {state.kind === 'chat' && (
        <ChatScreen
          token={state.token}
          conversationId={state.conversationId}
          onVolver={() =>
            setState({ kind: 'hoy', token: state.token, me: state.me, perfil: state.perfil })
          }
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

      <StatusBar style="dark" />
    </>
  );
}

const styles = StyleSheet.create({
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
});
