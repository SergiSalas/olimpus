import { useEffect, useRef, useState, type ReactNode } from 'react';
import {
  Animated,
  Dimensions,
  Easing,
  LayoutAnimation,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';
import { Boton, BotonPlano, Flotar } from '../components';
import { colors, fonts } from '../theme';

const PANELES = 4;

/**
 * Emojis del fondo. Cada uno pertenece a un panel y va a su propia
 * profundidad: al deslizar, los lejanos se mueven menos que los cercanos.
 */
const FONDO = [
  { emoji: '💬', panel: 0, x: 0.78, y: 90, tam: 34, prof: 0.5 },
  { emoji: '✨', panel: 0, x: 0.08, y: 190, tam: 24, prof: 0.25 },
  { emoji: '☀️', panel: 1, x: 0.8, y: 80, tam: 36, prof: 0.55 },
  { emoji: '🌙', panel: 1, x: 0.1, y: 170, tam: 26, prof: 0.3 },
  { emoji: '🔓', panel: 2, x: 0.8, y: 95, tam: 32, prof: 0.5 },
  { emoji: '🌈', panel: 2, x: 0.08, y: 200, tam: 26, prof: 0.25 },
  { emoji: '💖', panel: 3, x: 0.78, y: 90, tam: 34, prof: 0.55 },
  { emoji: '🎉', panel: 3, x: 0.1, y: 180, tam: 28, prof: 0.3 },
];

/**
 * Los cuatro paneles de bienvenida. Explican el producto antes de pedir nada,
 * y cada uno con una pequeña animación que se ve en vez de leerse: hablar
 * antes de ver, una persona al día, la escalera de niveles y la decisión final.
 */
export function IntroScreen({ onEmpezar }: { onEmpezar: () => void }) {
  const [panel, setPanel] = useState(0);
  const scroll = useRef<ScrollView>(null);
  const desplazamiento = useRef(new Animated.Value(0)).current;
  const ancho = Dimensions.get('window').width;
  const ultimo = panel === PANELES - 1;

  function irA(indice: number) {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.spring);
    setPanel(indice);
    scroll.current?.scrollTo({ x: indice * ancho, animated: true });
  }

  function alDeslizar(evento: NativeSyntheticEvent<NativeScrollEvent>) {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.spring);
    setPanel(Math.round(evento.nativeEvent.contentOffset.x / ancho));
  }

  return (
    <View style={estilos.pantalla}>
      {FONDO.map((f) => (
        <Animated.View
          key={f.emoji}
          pointerEvents="none"
          style={[
            estilos.fondo,
            {
              top: f.y,
              left: f.panel * ancho * f.prof + f.x * ancho,
              transform: [{ translateX: Animated.multiply(desplazamiento, -f.prof) }],
            },
          ]}>
          <Flotar distancia={8} duracion={2400 + f.tam * 30}>
            <Text style={{ fontSize: f.tam }}>{f.emoji}</Text>
          </Flotar>
        </Animated.View>
      ))}

      <View style={estilos.arriba}>
        <View style={estilos.segmentos}>
          {Array.from({ length: PANELES }, (_, i) => (
            <Pressable
              key={i}
              style={[estilos.segmento, i <= panel && estilos.segmentoHecho]}
              onPress={() => irA(i)}
              hitSlop={8}
            />
          ))}
        </View>
        {!ultimo && (
          <Pressable onPress={() => irA(PANELES - 1)} hitSlop={10}>
            <Text style={estilos.saltar}>Saltar</Text>
          </Pressable>
        )}
      </View>

      <Animated.ScrollView
        ref={scroll}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={alDeslizar}
        scrollEventThrottle={16}
        onScroll={Animated.event([{ nativeEvent: { contentOffset: { x: desplazamiento } } }], {
          useNativeDriver: true,
        })}
        style={{ flex: 1 }}>
        <Panel
          ancho={ancho}
          titulo="Habla primero. Ver viene después."
          frase="La foto no es lo primero: se va aclarando conforme la conversación va bien.">
          <HablaPrimero activo={panel === 0} />
        </Panel>
        <Panel
          ancho={ancho}
          titulo="Una persona al día."
          frase="Cada mañana te toca alguien, y tenéis hasta las 22:00 para hablar.">
          <UnaAlDia activo={panel === 1} />
        </Panel>
        <Panel
          ancho={ancho}
          titulo="Se desbloquea hablando."
          frase="Cuanto más habláis, más os conocéis. Y la foto, solo si los dos queréis.">
          <Escalera activo={panel === 2} />
        </Panel>
        <Panel
          ancho={ancho}
          titulo="Al final, decidís los dos."
          frase="Si los dos decís que sí, es una conexión, y ya no caduca.">
          <Decision activo={panel === 3} />
        </Panel>
      </Animated.ScrollView>

      <View style={estilos.pie}>
        <Boton
          texto={ultimo ? 'Crear mi cuenta' : 'Siguiente'}
          onPress={() => (ultimo ? onEmpezar() : irA(panel + 1))}
        />
        <BotonPlano texto="Ya tengo cuenta" onPress={onEmpezar} />
      </View>
    </View>
  );
}

function Panel({
  ancho,
  titulo,
  frase,
  children,
}: {
  ancho: number;
  titulo: string;
  frase: string;
  children: ReactNode;
}) {
  return (
    <View style={[estilos.panel, { width: ancho }]}>
      <View style={estilos.escena}>{children}</View>
      <Text style={estilos.titulo}>{titulo}</Text>
      <Text style={estilos.frase}>{frase}</Text>
    </View>
  );
}

/** Arranca la animación de un panel cada vez que se llega a él, desde cero. */
function useAlLlegar(activo: boolean, animacion: () => Animated.CompositeAnimation) {
  useEffect(() => {
    if (!activo) return;
    const a = animacion();
    a.start();
    return () => a.stop();
    // La animación se construye de nuevo en cada llegada; solo importa activo.
  }, [activo]);
}

const valor = () => new Animated.Value(0);
const muelle = (v: Animated.Value) =>
  Animated.spring(v, { toValue: 1, useNativeDriver: true, speed: 12, bounciness: 10 });
const hasta = (v: Animated.Value, toValue: number, duration: number) =>
  Animated.timing(v, { toValue, duration, useNativeDriver: true });

/** Aparece desde abajo según `v` vaya de 0 a 1. */
const aparece = (v: Animated.Value) => ({
  opacity: v,
  transform: [{ translateY: v.interpolate({ inputRange: [0, 1], outputRange: [16, 0] }) }],
});

// ---------- 1. Habla primero ----------

function HablaPrimero({ activo }: { activo: boolean }) {
  const [b1, b2, b3, d2, d3] = useRef([valor(), valor(), valor(), valor(), valor()]).current;

  useAlLlegar(activo, () => {
    [b1, b2, b3, d2, d3].forEach((v) => v.setValue(0));
    return Animated.sequence([
      Animated.delay(250),
      muelle(b1),
      hasta(d2, 1, 200),
      Animated.delay(800),
      hasta(d2, 0, 150),
      muelle(b2),
      hasta(d3, 1, 200),
      Animated.delay(800),
      hasta(d3, 0, 150),
      muelle(b3),
    ]);
  });

  // Cada mensaje aclara un poco la niebla que tapa la foto.
  const niebla = Animated.add(Animated.add(b1, b2), b3).interpolate({
    inputRange: [0, 3],
    outputRange: [0.94, 0],
  });

  return (
    <View style={{ gap: 12 }}>
      <View style={estilos.fotoMarco}>
        <Text style={estilos.fotoCara}>😊</Text>
        <Animated.View style={[estilos.niebla, { opacity: niebla }]}>
          <Text style={estilos.nieblaTexto}>?</Text>
        </Animated.View>
      </View>

      <Animated.View style={[estilos.burbujaSuya, aparece(b1)]}>
        <Text style={estilos.textoSuyo}>¿Montaña o rocódromo?</Text>
      </Animated.View>
      <View>
        <Animated.View style={[estilos.burbujaMia, aparece(b2)]}>
          <Text style={estilos.textoMio}>Montaña, siempre 🏔️</Text>
        </Animated.View>
        <Animated.View style={[estilos.escribiendo, estilos.escribiendoMio, { opacity: d2 }]}>
          <Puntos />
        </Animated.View>
      </View>
      <View>
        <Animated.View style={[estilos.burbujaSuya, aparece(b3)]}>
          <Text style={estilos.textoSuyo}>¡Por fin alguien con criterio!</Text>
        </Animated.View>
        <Animated.View style={[estilos.escribiendo, { opacity: d3 }]}>
          <Puntos />
        </Animated.View>
      </View>
    </View>
  );
}

/** Los tres puntitos de "escribiendo…", botando uno detrás de otro. */
function Puntos() {
  const v = useRef(valor()).current;
  useEffect(() => {
    const bucle = Animated.loop(
      Animated.timing(v, {
        toValue: 1,
        duration: 900,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );
    bucle.start();
    return () => bucle.stop();
  }, [v]);
  return (
    <View style={estilos.puntos}>
      {[0, 1, 2].map((i) => (
        <Animated.View
          key={i}
          style={[
            estilos.punto,
            {
              transform: [
                {
                  translateY: v.interpolate({
                    inputRange: [0, 0.15 + i * 0.2, 0.3 + i * 0.2, 1],
                    outputRange: [0, -5, 0, 0],
                    extrapolate: 'clamp',
                  }),
                },
              ],
            },
          ]}
        />
      ))}
    </View>
  );
}

// ---------- 2. Una persona al día ----------

const MOMENTOS = [
  { hora: '04:00', emoji: '☀️', texto: 'Llega tu persona del día' },
  { hora: 'Durante el día', emoji: '💬', texto: 'Habláis a vuestro ritmo' },
  { hora: '22:00', emoji: '🌙', texto: 'La conversación se cierra' },
];

function UnaAlDia({ activo }: { activo: boolean }) {
  const filas = useRef([valor(), valor(), valor()]).current;
  const tarjeta = useRef(valor()).current;
  // La barra cambia de alto: eso no lo puede animar el hilo nativo.
  const barra = useRef(valor()).current;

  useAlLlegar(activo, () => {
    [...filas, tarjeta, barra].forEach((v) => v.setValue(0));
    return Animated.parallel([
      Animated.timing(barra, { toValue: 1, duration: 3200, useNativeDriver: false }),
      Animated.sequence([
        Animated.delay(200),
        muelle(filas[0]),
        Animated.spring(tarjeta, { toValue: 1, useNativeDriver: true, speed: 8, bounciness: 12 }),
        Animated.delay(500),
        muelle(filas[1]),
        Animated.delay(900),
        muelle(filas[2]),
      ]),
    ]);
  });

  return (
    <View style={estilos.dia}>
      <View style={estilos.carril}>
        <Animated.View
          style={[
            estilos.carrilLleno,
            { height: barra.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }) },
          ]}
        />
      </View>
      <View style={{ flex: 1, gap: 22 }}>
        {MOMENTOS.map((m, i) => (
          <Animated.View key={m.hora} style={[estilos.momento, aparece(filas[i])]}>
            <Text style={estilos.momentoEmoji}>{m.emoji}</Text>
            <View style={{ flex: 1 }}>
              <Text style={estilos.momentoHora}>{m.hora}</Text>
              <Text style={estilos.momentoTexto}>{m.texto}</Text>
              {i === 0 && (
                <Animated.View
                  style={[
                    estilos.miniTarjeta,
                    {
                      opacity: tarjeta,
                      transform: [
                        { perspective: 600 },
                        {
                          rotateY: tarjeta.interpolate({
                            inputRange: [0, 1],
                            outputRange: ['90deg', '0deg'],
                          }),
                        },
                      ],
                    },
                  ]}>
                  <View style={estilos.miniBola}>
                    <Text style={estilos.miniInterrogante}>?</Text>
                  </View>
                  <Text style={estilos.miniTexto}>27 años · a 4 km</Text>
                </Animated.View>
              )}
            </View>
          </Animated.View>
        ))}
      </View>
    </View>
  );
}

// ---------- 3. La escalera ----------

const PELDANOS = [
  { emoji: '🎂', texto: 'Edad y dos intereses', color: colors.accent },
  { emoji: '🏷️', texto: 'Su apodo, al escribiros', color: colors.sol },
  { emoji: '💬', texto: 'Sus tres preguntas', color: colors.menta },
  { emoji: '📸', texto: 'Su foto, si los dos queréis', color: colors.cielo },
  { emoji: '💖', texto: 'Lo que queráis compartir', color: colors.uva },
];

function Escalera({ activo }: { activo: boolean }) {
  const subida = useRef(valor()).current;

  useAlLlegar(activo, () => {
    subida.setValue(0);
    return Animated.sequence([
      Animated.delay(250),
      ...PELDANOS.map((_, i) =>
        Animated.sequence([hasta(subida, i + 1, 380), Animated.delay(220)]),
      ),
    ]);
  });

  return (
    <View style={{ gap: 10 }}>
      {PELDANOS.map((p, i) => (
        <Animated.View
          key={p.emoji}
          style={[
            estilos.peldano,
            {
              marginLeft: i * 12,
              opacity: subida.interpolate({
                inputRange: [i, i + 1],
                outputRange: [0.25, 1],
                extrapolate: 'clamp',
              }),
            },
          ]}>
          <Animated.View
            style={[
              estilos.peldanoBola,
              {
                backgroundColor: p.color,
                transform: [
                  {
                    scale: subida.interpolate({
                      inputRange: [i, i + 0.5, i + 1],
                      outputRange: [1, 1.35, 1],
                      extrapolate: 'clamp',
                    }),
                  },
                ],
              },
            ]}>
            <Text style={estilos.peldanoNumero}>{i}</Text>
          </Animated.View>
          <Text style={estilos.peldanoTexto}>
            {p.emoji} {p.texto}
          </Text>
        </Animated.View>
      ))}
    </View>
  );
}

// ---------- 4. La decisión ----------

const CONFETI = ['✨', '💖', '⭐', '🎉', '💫', '🌟', '💌', '🎈'];

function Decision({ activo }: { activo: boolean }) {
  const [acercar, corazon, estallido] = useRef([valor(), valor(), valor()]).current;

  useAlLlegar(activo, () => {
    [acercar, corazon, estallido].forEach((v) => v.setValue(0));
    return Animated.sequence([
      Animated.delay(300),
      Animated.timing(acercar, {
        toValue: 1,
        duration: 900,
        easing: Easing.inOut(Easing.quad),
        useNativeDriver: true,
      }),
      Animated.parallel([
        Animated.spring(corazon, { toValue: 1, useNativeDriver: true, speed: 10, bounciness: 18 }),
        Animated.timing(estallido, {
          toValue: 1,
          duration: 1000,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }),
      ]),
    ]);
  });

  const burbuja = (lado: 1 | -1) => ({
    opacity: corazon.interpolate({
      inputRange: [0, 0.3],
      outputRange: [1, 0],
      extrapolate: 'clamp',
    }),
    transform: [
      {
        translateX: acercar.interpolate({
          inputRange: [0, 1],
          outputRange: [lado * 90, lado * 34],
        }),
      },
    ],
  });

  return (
    <View style={estilos.decision}>
      {CONFETI.map((emoji, i) => {
        const angulo = (i / CONFETI.length) * Math.PI * 2;
        return (
          <Animated.Text
            key={emoji}
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
                      outputRange: [0, Math.cos(angulo) * 110],
                    }),
                  },
                  {
                    translateY: estallido.interpolate({
                      inputRange: [0, 1],
                      outputRange: [0, Math.sin(angulo) * 110],
                    }),
                  },
                ],
              },
            ]}>
            {emoji}
          </Animated.Text>
        );
      })}

      <Animated.View style={[estilos.si, estilos.siTu, burbuja(-1)]}>
        <Text style={estilos.siTexto}>¿Sí?</Text>
      </Animated.View>
      <Animated.View style={[estilos.si, estilos.siOtro, burbuja(1)]}>
        <Text style={[estilos.siTexto, { color: colors.ink }]}>¿Sí?</Text>
      </Animated.View>

      <Animated.Text
        style={[
          estilos.corazon,
          {
            opacity: corazon,
            transform: [
              { scale: corazon.interpolate({ inputRange: [0, 1], outputRange: [0.2, 1] }) },
            ],
          },
        ]}>
        ❤️
      </Animated.Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bgCalido },
  fondo: { position: 'absolute' },

  arriba: {
    paddingTop: 58,
    paddingHorizontal: 26,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  segmentos: { flex: 1, flexDirection: 'row', gap: 6 },
  segmento: { flex: 1, height: 6, borderRadius: 999, backgroundColor: 'rgba(42,23,71,0.12)' },
  segmentoHecho: { backgroundColor: colors.accent },
  saltar: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.uva },

  panel: { paddingHorizontal: 26, paddingTop: 20 },
  escena: { flex: 1, justifyContent: 'center' },
  titulo: {
    fontFamily: fonts.displayFuerte,
    fontSize: 34,
    lineHeight: 40,
    color: colors.ink,
    marginBottom: 10,
  },
  frase: { fontFamily: fonts.sansMedia, fontSize: 16, lineHeight: 24, color: colors.ink2 },

  // 1
  fotoMarco: {
    alignSelf: 'center',
    width: 104,
    height: 104,
    borderRadius: 999,
    backgroundColor: colors.sol,
    borderWidth: 5,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    marginBottom: 6,
  },
  fotoCara: { fontSize: 56 },
  niebla: {
    ...StyleSheet.absoluteFill,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nieblaTexto: { fontFamily: fonts.displayFuerte, fontSize: 42, color: colors.trazo },
  burbujaSuya: {
    alignSelf: 'flex-start',
    maxWidth: '80%',
    backgroundColor: colors.surface,
    borderRadius: 22,
    borderBottomLeftRadius: 6,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    paddingHorizontal: 18,
    paddingVertical: 13,
  },
  burbujaMia: {
    alignSelf: 'flex-end',
    maxWidth: '80%',
    backgroundColor: colors.accent,
    borderRadius: 22,
    borderBottomRightRadius: 6,
    borderBottomWidth: 4,
    borderColor: colors.accentOscuro,
    paddingHorizontal: 18,
    paddingVertical: 13,
  },
  textoSuyo: { fontFamily: fonts.sans, fontSize: 15.5, color: colors.ink },
  textoMio: { fontFamily: fonts.sansMedia, fontSize: 15.5, color: '#FFFFFF' },
  escribiendo: {
    position: 'absolute',
    left: 0,
    top: 0,
    backgroundColor: colors.surface,
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 14,
  },
  escribiendoMio: { left: undefined, right: 0, backgroundColor: colors.accentWash },
  puntos: { flexDirection: 'row', gap: 5 },
  punto: { width: 8, height: 8, borderRadius: 999, backgroundColor: colors.ink4 },

  // 2
  dia: { flexDirection: 'row', gap: 18 },
  carril: {
    width: 6,
    borderRadius: 999,
    backgroundColor: 'rgba(42,23,71,0.1)',
    overflow: 'hidden',
  },
  carrilLleno: { width: 6, borderRadius: 999, backgroundColor: colors.uva },
  momento: { flexDirection: 'row', gap: 12, alignItems: 'flex-start' },
  momentoEmoji: { fontSize: 30 },
  momentoHora: { fontFamily: fonts.sansNegrita, fontSize: 13, color: colors.uva },
  momentoTexto: { fontFamily: fonts.display, fontSize: 19, color: colors.ink },
  miniTarjeta: {
    marginTop: 10,
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: colors.uva,
    borderRadius: 18,
    borderBottomWidth: 4,
    borderColor: '#6A3FD1',
    paddingVertical: 9,
    paddingHorizontal: 12,
  },
  miniBola: {
    width: 32,
    height: 32,
    borderRadius: 999,
    backgroundColor: colors.sol,
    alignItems: 'center',
    justifyContent: 'center',
  },
  miniInterrogante: { fontFamily: fonts.displayFuerte, fontSize: 16, color: colors.ink },
  miniTexto: { fontFamily: fonts.sansNegrita, fontSize: 13.5, color: '#FFFFFF' },

  // 3
  peldano: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: 18,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    paddingVertical: 9,
    paddingHorizontal: 12,
  },
  peldanoBola: {
    width: 30,
    height: 30,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
  },
  peldanoNumero: { fontFamily: fonts.displayFuerte, fontSize: 15, color: '#FFFFFF' },
  peldanoTexto: { fontFamily: fonts.sansNegrita, fontSize: 14.5, color: colors.ink, flexShrink: 1 },

  // 4
  decision: { height: 240, alignItems: 'center', justifyContent: 'center' },
  confeti: { position: 'absolute', fontSize: 26 },
  si: {
    position: 'absolute',
    width: 92,
    height: 92,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 5,
    borderColor: '#FFFFFF',
  },
  siTu: { backgroundColor: colors.accent },
  siOtro: { backgroundColor: colors.sol },
  siTexto: { fontFamily: fonts.displayFuerte, fontSize: 24, color: '#FFFFFF' },
  corazon: { fontSize: 110 },

  pie: { paddingHorizontal: 26, paddingTop: 16, paddingBottom: 36 },
});
