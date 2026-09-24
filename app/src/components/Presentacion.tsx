import { useEffect, useRef } from 'react';
import { Animated, Easing, Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts } from '../theme';
import { t } from '../i18n';

const CONFETI = ['✨', '💬', '💖', '⭐', '🎉', '🌈', '💫', '🔥', '🎈', '😄', '🌟', '💌'];

/**
 * La primera vez que se abre la conversación del día: tu círculo y el de la
 * otra persona chocan en el centro, salta confeti y se aparta para dejar ver el
 * chat. Tocar en cualquier sitio la salta.
 */
export function Presentacion({
  inicial,
  detalle,
  onFin,
}: {
  /** Tu inicial, para tu círculo. */
  inicial: string;
  /** Lo poco que se sabe del otro: "25 años · a 5 km". */
  detalle: string;
  onFin: () => void;
}) {
  const llegada = useRef(new Animated.Value(0)).current;
  const choque = useRef(new Animated.Value(0)).current;
  const explosion = useRef(new Animated.Value(0)).current;
  const salida = useRef(new Animated.Value(0)).current;
  const terminada = useRef(false);

  function salir() {
    if (terminada.current) return;
    terminada.current = true;
    Animated.timing(salida, {
      toValue: 1,
      duration: 450,
      easing: Easing.in(Easing.quad),
      useNativeDriver: true,
    }).start(onFin);
  }

  useEffect(() => {
    Animated.sequence([
      Animated.spring(llegada, { toValue: 1, useNativeDriver: true, speed: 5, bounciness: 6 }),
      Animated.parallel([
        Animated.sequence([
          Animated.timing(choque, { toValue: 1, duration: 110, useNativeDriver: true }),
          Animated.spring(choque, { toValue: 0, useNativeDriver: true, speed: 14, bounciness: 16 }),
        ]),
        Animated.timing(explosion, {
          toValue: 1,
          duration: 1200,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }),
      ]),
    ]).start();
    const reloj = setTimeout(salir, 3200);
    return () => clearTimeout(reloj);
    // Solo al montar: la animación va una vez y luego se va sola.
  }, []);

  const circulo = (lado: 1 | -1) => ({
    transform: [
      {
        translateX: llegada.interpolate({
          inputRange: [0, 1],
          outputRange: [lado * 240, lado * 46],
        }),
      },
      {
        rotate: llegada.interpolate({
          inputRange: [0, 1],
          outputRange: [`${lado * 40}deg`, '0deg'],
        }),
      },
      { scale: choque.interpolate({ inputRange: [0, 1], outputRange: [1, 1.18] }) },
    ],
  });

  return (
    <Animated.View
      style={[
        estilos.fondo,
        {
          opacity: salida.interpolate({ inputRange: [0, 1], outputRange: [1, 0] }),
          transform: [
            { scale: salida.interpolate({ inputRange: [0, 1], outputRange: [1, 1.15] }) },
          ],
        },
      ]}>
      <Pressable style={StyleSheet.absoluteFill} onPress={salir} />

      <View style={estilos.centro} pointerEvents="none">
        {CONFETI.map((emoji, i) => {
          const angulo = (i / CONFETI.length) * Math.PI * 2;
          const lejos = 120 + (i % 3) * 45;
          return (
            <Animated.Text
              key={i}
              style={[
                estilos.confeti,
                {
                  opacity: explosion.interpolate({
                    inputRange: [0, 0.1, 0.7, 1],
                    outputRange: [0, 1, 1, 0],
                  }),
                  transform: [
                    {
                      translateX: explosion.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0, Math.cos(angulo) * lejos],
                      }),
                    },
                    {
                      translateY: explosion.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0, Math.sin(angulo) * lejos],
                      }),
                    },
                    {
                      scale: explosion.interpolate({
                        inputRange: [0, 0.3, 1],
                        outputRange: [0.3, 1.3, 1],
                      }),
                    },
                  ],
                },
              ]}>
              {emoji}
            </Animated.Text>
          );
        })}

        <Animated.View style={[estilos.circulo, estilos.tu, circulo(-1)]}>
          <Text style={estilos.letra}>{inicial}</Text>
        </Animated.View>
        <Animated.View style={[estilos.circulo, estilos.otro, circulo(1)]}>
          <Text style={[estilos.letra, { color: colors.ink }]}>?</Text>
        </Animated.View>
      </View>

      <Animated.View
        pointerEvents="none"
        style={[
          estilos.textos,
          {
            opacity: explosion.interpolate({ inputRange: [0, 0.25, 1], outputRange: [0, 1, 1] }),
            transform: [
              {
                translateY: explosion.interpolate({
                  inputRange: [0, 0.4],
                  outputRange: [30, 0],
                  extrapolate: 'clamp',
                }),
              },
            ],
          },
        ]}>
        <Text style={estilos.titulo}>{t('presentacion.titulo')}</Text>
        <Text style={estilos.detalle}>{t('presentacion.detalle', { detalle })}</Text>
      </Animated.View>

      <Text style={estilos.saltar}>{t('presentacion.toca')}</Text>
    </Animated.View>
  );
}

const estilos = StyleSheet.create({
  fondo: {
    ...StyleSheet.absoluteFill,
    zIndex: 10,
    // En Android lo que tiene sombra se pinta encima aunque el zIndex diga otra cosa.
    elevation: 20,
    backgroundColor: colors.uva,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centro: { height: 200, width: '100%', alignItems: 'center', justifyContent: 'center' },
  confeti: { position: 'absolute', fontSize: 30 },
  circulo: {
    position: 'absolute',
    width: 96,
    height: 96,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 5,
    borderColor: '#FFFFFF',
  },
  tu: { backgroundColor: colors.accent },
  otro: { backgroundColor: colors.sol },
  letra: { fontFamily: fonts.displayFuerte, fontSize: 40, color: '#FFFFFF' },
  textos: { alignItems: 'center', gap: 10, paddingHorizontal: 32, marginTop: 30 },
  titulo: {
    fontFamily: fonts.displayFuerte,
    fontSize: 32,
    lineHeight: 38,
    color: '#FFFFFF',
    textAlign: 'center',
  },
  detalle: {
    fontFamily: fonts.sansMedia,
    fontSize: 15,
    lineHeight: 22,
    color: colors.onInk2,
    textAlign: 'center',
  },
  saltar: {
    position: 'absolute',
    bottom: 50,
    fontFamily: fonts.sansMedia,
    fontSize: 13,
    color: colors.onInk2,
  },
});
