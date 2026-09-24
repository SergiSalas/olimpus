import { useEffect, useRef } from 'react';
import { Animated, Easing, Pressable, StyleSheet, Text } from 'react-native';
import type { ChatMessage } from '../api';
import { Entrada } from '../components';
import { colors, fonts } from '../theme';

/** Dos toques más juntos que esto cuentan como doble toque. */
const DOBLE_TOQUE_MS = 300;

/**
 * Un mensaje del chat. Los de la otra persona se pueden marcar con un corazón
 * tocándolos dos veces; el corazón se ve en los dos lados, pegado a la burbuja.
 */
export function Burbuja({
  mensaje,
  hora,
  onCorazon,
}: {
  mensaje: ChatMessage;
  hora: string;
  /** Solo en los mensajes del otro: a uno propio no se le da corazón. */
  onCorazon?: () => void;
}) {
  const ultimoToque = useRef(0);
  const estallido = useRef(new Animated.Value(0)).current;
  const insignia = useRef(new Animated.Value(mensaje.liked ? 1 : 0)).current;

  // La insignia entra con un salto cuando llega el corazón (tuyo o del otro).
  useEffect(() => {
    Animated.spring(insignia, {
      toValue: mensaje.liked ? 1 : 0,
      useNativeDriver: true,
      speed: 14,
      bounciness: 16,
    }).start();
  }, [mensaje.liked, insignia]);

  function tocar() {
    if (!onCorazon) return;
    const ahora = Date.now();
    if (ahora - ultimoToque.current < DOBLE_TOQUE_MS) {
      ultimoToque.current = 0;
      // El corazón grande solo al ponerlo; al quitarlo basta con que se vaya la insignia.
      if (!mensaje.liked) {
        estallido.setValue(0);
        Animated.timing(estallido, {
          toValue: 1,
          duration: 700,
          easing: Easing.out(Easing.quad),
          useNativeDriver: true,
        }).start();
      }
      onCorazon();
    } else {
      ultimoToque.current = ahora;
    }
  }

  const mia = mensaje.mine;

  return (
    <Entrada
      style={[
        estilos.burbuja,
        mia ? estilos.mia : estilos.suya,
        mensaje.liked && { marginBottom: 10 },
      ]}>
      <Pressable onPress={tocar} disabled={!onCorazon}>
        <Text style={mia ? estilos.textoMio : estilos.textoSuyo}>{mensaje.text}</Text>
        <Text style={mia ? estilos.horaMia : estilos.horaSuya}>{hora}</Text>
      </Pressable>

      <Animated.Text
        pointerEvents="none"
        style={[
          estilos.estallido,
          {
            opacity: estallido.interpolate({
              inputRange: [0, 0.15, 0.7, 1],
              outputRange: [0, 1, 1, 0],
            }),
            transform: [
              {
                scale: estallido.interpolate({
                  inputRange: [0, 0.35, 1],
                  outputRange: [0.2, 1.4, 1],
                }),
              },
              { translateY: estallido.interpolate({ inputRange: [0, 1], outputRange: [0, -18] }) },
            ],
          },
        ]}>
        ❤️
      </Animated.Text>

      <Animated.View
        pointerEvents="none"
        style={[
          estilos.insignia,
          mia ? { left: -8 } : { right: -8 },
          { opacity: insignia, transform: [{ scale: insignia }] },
        ]}>
        <Text style={estilos.insigniaTexto}>❤️</Text>
      </Animated.View>
    </Entrada>
  );
}

const estilos = StyleSheet.create({
  burbuja: { maxWidth: '82%', borderRadius: 22, paddingHorizontal: 18, paddingVertical: 14 },
  mia: {
    alignSelf: 'flex-end',
    backgroundColor: colors.accent,
    borderBottomWidth: 4,
    borderColor: colors.accentOscuro,
    borderBottomRightRadius: 6,
  },
  suya: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    borderBottomLeftRadius: 6,
  },
  textoMio: { fontFamily: fonts.sansMedia, fontSize: 16, lineHeight: 23, color: '#FFFFFF' },
  textoSuyo: { fontFamily: fonts.sans, fontSize: 16, lineHeight: 23, color: colors.ink },
  horaMia: {
    fontFamily: fonts.sans,
    fontSize: 10.5,
    color: colors.accentClaro,
    marginTop: 4,
    textAlign: 'right',
  },
  horaSuya: { fontFamily: fonts.sans, fontSize: 10.5, color: colors.ink5, marginTop: 4 },
  estallido: {
    position: 'absolute',
    alignSelf: 'center',
    top: '20%',
    fontSize: 44,
  },
  insignia: {
    position: 'absolute',
    bottom: -12,
    width: 28,
    height: 28,
    borderRadius: 999,
    backgroundColor: '#FFFFFF',
    borderWidth: 2,
    borderColor: colors.accentBorde,
    alignItems: 'center',
    justifyContent: 'center',
  },
  insigniaTexto: { fontSize: 13 },
});
