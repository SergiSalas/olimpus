import * as Haptics from 'expo-haptics';
import { useEffect, useRef } from 'react';
import { Animated, Easing, Pressable, StyleSheet, Text, View } from 'react-native';
import { COLOR_NIVEL, NIVELES } from './Escalera';
import { t } from '../i18n';
import { colors, fonts } from '../theme';

const CONFETI = ['✨', '🎉', '💫', '⭐', '🔓', '🌟', '💖', '🎈'];

/** Lo que dura en pantalla si nadie la toca. */
const DURA_MS = 3500;

/**
 * Subir de nivel es el momento de la app: por eso tiene el suyo. Una tarjeta del
 * color del nivel salta al centro con confeti y un toque de vibración, cuenta lo
 * que se acaba de destapar y deja ir directo a verlo.
 */
export function SubidaNivel({
  nivel,
  onVerPerfil,
  onFin,
}: {
  nivel: number;
  onVerPerfil: () => void;
  onFin: () => void;
}) {
  const entrada = useRef(new Animated.Value(0)).current;
  const estallido = useRef(new Animated.Value(0)).current;
  const color = COLOR_NIVEL[nivel] ?? colors.accent;
  const claro = nivel === 1 || nivel === 2;

  useEffect(() => {
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {});
    Animated.parallel([
      Animated.spring(entrada, { toValue: 1, useNativeDriver: true, speed: 9, bounciness: 14 }),
      Animated.timing(estallido, {
        toValue: 1,
        duration: 1100,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }),
    ]).start();
    const reloj = setTimeout(onFin, DURA_MS);
    return () => clearTimeout(reloj);
    // Una vez por subida: el componente se monta de nuevo en la siguiente.
  }, []);

  return (
    <Animated.View
      style={[
        estilos.velo,
        {
          opacity: entrada.interpolate({
            inputRange: [0, 0.4],
            outputRange: [0, 1],
            extrapolate: 'clamp',
          }),
        },
      ]}>
      <Pressable style={StyleSheet.absoluteFill} onPress={onFin} />

      <View style={estilos.centro} pointerEvents="box-none">
        {CONFETI.map((emoji, i) => {
          const angulo = (i / CONFETI.length) * Math.PI * 2;
          return (
            <Animated.Text
              key={emoji}
              pointerEvents="none"
              style={[
                estilos.confeti,
                {
                  opacity: estallido.interpolate({
                    inputRange: [0, 0.1, 0.7, 1],
                    outputRange: [0, 1, 1, 0],
                  }),
                  transform: [
                    {
                      translateX: estallido.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0, Math.cos(angulo) * 170],
                      }),
                    },
                    {
                      translateY: estallido.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0, Math.sin(angulo) * 170],
                      }),
                    },
                  ],
                },
              ]}>
              {emoji}
            </Animated.Text>
          );
        })}

        <Animated.View
          style={[
            estilos.tarjeta,
            { backgroundColor: color },
            {
              transform: [
                { scale: entrada.interpolate({ inputRange: [0, 1], outputRange: [0.5, 1] }) },
                {
                  rotate: entrada.interpolate({
                    inputRange: [0, 1],
                    outputRange: ['-8deg', '0deg'],
                  }),
                },
              ],
            },
          ]}>
          <Text style={estilos.candado}>🔓</Text>
          <Text style={[estilos.titulo, claro && { color: colors.ink }]}>
            {t('subida.titulo', { nivel })}
          </Text>
          <Text style={[estilos.nombre, claro && { color: colors.ink }]}>
            {NIVELES[nivel]?.titulo}
          </Text>
          <Text style={[estilos.desc, claro && { color: colors.ink2 }]}>
            {t('subida.desbloqueado', { desc: NIVELES[nivel]?.desc.toLowerCase() ?? '' })}
          </Text>
          <Pressable
            style={estilos.boton}
            onPress={() => {
              onFin();
              onVerPerfil();
            }}>
            <Text style={estilos.botonTexto}>{t('chat.verPerfil')}</Text>
          </Pressable>
        </Animated.View>
      </View>
    </Animated.View>
  );
}

const estilos = StyleSheet.create({
  velo: {
    ...StyleSheet.absoluteFill,
    zIndex: 9,
    elevation: 19,
    backgroundColor: colors.velo,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centro: { alignItems: 'center', justifyContent: 'center' },
  confeti: { position: 'absolute', fontSize: 30 },
  tarjeta: {
    width: 280,
    borderRadius: 32,
    borderWidth: 5,
    borderColor: '#FFFFFF',
    paddingVertical: 26,
    paddingHorizontal: 22,
    alignItems: 'center',
    gap: 4,
  },
  candado: { fontSize: 48, marginBottom: 4 },
  titulo: { fontFamily: fonts.displayFuerte, fontSize: 34, color: '#FFFFFF' },
  nombre: { fontFamily: fonts.display, fontSize: 20, color: '#FFFFFF' },
  desc: {
    fontFamily: fonts.sansMedia,
    fontSize: 14.5,
    lineHeight: 21,
    color: 'rgba(255,255,255,0.9)',
    textAlign: 'center',
    marginTop: 6,
  },
  boton: {
    marginTop: 16,
    backgroundColor: '#FFFFFF',
    borderRadius: 999,
    paddingHorizontal: 20,
    paddingVertical: 11,
  },
  botonTexto: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.ink },
});
