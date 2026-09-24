import { useEffect, useRef, type ReactNode } from 'react';
import {
  ActivityIndicator,
  Animated,
  Easing,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  type PressableProps,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import { colors, fonts, radios, text } from './theme';
import { t } from './i18n';

const muelle = { useNativeDriver: true, speed: 30, bounciness: 14 };

/**
 * Pressable que se encoge al tocarlo y rebota al soltarlo. Con `pop`, además
 * da un saltito cada vez que pasa a verdadero (al elegir una opción).
 *
 * `style` es lo que se ve y se encoge; `fuera`, el hueco que ocupa en su
 * padre (un `flex: 1` en una fila, por ejemplo).
 */
export function Rebote({
  style,
  fuera,
  pop,
  children,
  ...props
}: Omit<PressableProps, 'style' | 'children'> & {
  style?: StyleProp<ViewStyle>;
  fuera?: StyleProp<ViewStyle>;
  pop?: boolean;
  children?: ReactNode;
}) {
  const escala = useRef(new Animated.Value(1)).current;
  const primera = useRef(true);

  useEffect(() => {
    if (primera.current) {
      primera.current = false;
      return;
    }
    if (!pop) return;
    escala.setValue(1.12);
    Animated.spring(escala, { toValue: 1, ...muelle, bounciness: 18 }).start();
  }, [pop, escala]);

  return (
    <Pressable
      {...props}
      style={fuera}
      onPressIn={(e) => {
        Animated.spring(escala, { toValue: 0.93, ...muelle }).start();
        props.onPressIn?.(e);
      }}
      onPressOut={(e) => {
        Animated.spring(escala, { toValue: 1, ...muelle }).start();
        props.onPressOut?.(e);
      }}>
      <Animated.View style={[style, { transform: [{ scale: escala }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

/**
 * Aparece desde abajo con un pequeño muelle. `retraso` en ms, para escalonar;
 * `desdeX` para que llegue de lado (positivo, desde la derecha).
 */
export function Entrada({
  children,
  retraso = 0,
  desdeX = 0,
  style,
}: {
  children: ReactNode;
  retraso?: number;
  desdeX?: number;
  style?: StyleProp<ViewStyle>;
}) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.spring(v, {
      toValue: 1,
      delay: retraso,
      useNativeDriver: true,
      speed: 12,
      bounciness: 9,
    }).start();
  }, [v, retraso]);
  return (
    <Animated.View
      style={[
        style,
        {
          opacity: v.interpolate({ inputRange: [0, 0.6, 1], outputRange: [0, 1, 1] }),
          transform: [
            { translateX: v.interpolate({ inputRange: [0, 1], outputRange: [desdeX, 0] }) },
            {
              translateY: v.interpolate({ inputRange: [0, 1], outputRange: [desdeX ? 0 : 28, 0] }),
            },
            { scale: v.interpolate({ inputRange: [0, 1], outputRange: [0.94, 1] }) },
          ],
        },
      ]}>
      {children}
    </Animated.View>
  );
}

/** Flota arriba y abajo sin parar, balanceándose `giro` grados. Para adornos. */
export function Flotar({
  children,
  duracion = 2600,
  distancia = 10,
  giro = 6,
  style,
}: {
  children: ReactNode;
  duracion?: number;
  distancia?: number;
  giro?: number;
  style?: StyleProp<ViewStyle>;
}) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const ida = { duration: duracion / 2, easing: Easing.inOut(Easing.sin), useNativeDriver: true };
    const bucle = Animated.loop(
      Animated.sequence([
        Animated.timing(v, { toValue: 1, ...ida }),
        Animated.timing(v, { toValue: 0, ...ida }),
      ]),
    );
    bucle.start();
    return () => bucle.stop();
  }, [v, duracion]);
  return (
    <Animated.View
      pointerEvents="none"
      style={[
        style,
        {
          transform: [
            { translateY: v.interpolate({ inputRange: [0, 1], outputRange: [0, -distancia] }) },
            {
              rotate: v.interpolate({
                inputRange: [0, 1],
                outputRange: [`-${giro}deg`, `${giro}deg`],
              }),
            },
          ],
        },
      ]}>
      {children}
    </Animated.View>
  );
}

/** Botón principal, con relieve. Chicle el de avanzar, uva oscura el de rematar. */
export function Boton({
  texto,
  onPress,
  ocupado,
  deshabilitado,
  tono = 'accent',
}: {
  texto: string;
  onPress: () => void;
  ocupado?: boolean;
  deshabilitado?: boolean;
  tono?: 'accent' | 'oscuro';
}) {
  const apagado = ocupado || deshabilitado;
  return (
    <Rebote
      pop={!apagado}
      style={[
        estilos.boton,
        tono === 'oscuro' ? estilos.botonOscuro : estilos.botonAccent,
        apagado && estilos.botonApagado,
      ]}
      onPress={onPress}
      disabled={apagado}>
      {ocupado ? (
        <ActivityIndicator color="#FFFFFF" />
      ) : (
        <Text
          style={[
            text.boton,
            apagado && !ocupado ? { color: colors.ink5 } : null,
            tono === 'oscuro' ? { color: colors.onInk } : null,
          ]}>
          {texto}
        </Text>
      )}
    </Rebote>
  );
}

/** Botón de texto, sin fondo. */
export function BotonPlano({ texto, onPress }: { texto: string; onPress: () => void }) {
  return (
    <Rebote style={estilos.botonPlano} onPress={onPress}>
      <Text style={estilos.botonPlanoTexto}>{texto}</Text>
    </Rebote>
  );
}

/** Fila de opción, con círculo (elegir una) o cuadro (elegir varias). */
export function FilaOpcion({
  etiqueta,
  detalle,
  elegida,
  varias,
  onPress,
}: {
  etiqueta: string;
  detalle?: string;
  elegida: boolean;
  varias?: boolean;
  onPress: () => void;
}) {
  return (
    <Rebote
      pop={elegida}
      style={[estilos.fila, elegida ? estilos.filaElegida : estilos.filaNormal]}
      onPress={onPress}>
      <View style={{ flexShrink: 1, gap: 3 }}>
        <Text style={[estilos.filaTexto, elegida && { fontFamily: fonts.sansNegrita }]}>
          {etiqueta}
        </Text>
        {detalle && <Text style={estilos.filaDetalle}>{detalle}</Text>}
      </View>
      <View
        style={[
          varias ? estilos.cuadro : estilos.circulo,
          elegida && (varias ? estilos.cuadroElegido : estilos.circuloElegido),
        ]}
      />
    </Rebote>
  );
}

/** Pastilla: idiomas, intereses, intereses en común. */
export function Pastilla({
  texto: etiqueta,
  elegida,
  onPress,
  deshabilitada,
  tono = 'oscuro',
}: {
  texto: string;
  elegida: boolean;
  onPress?: () => void;
  deshabilitada?: boolean;
  tono?: 'oscuro' | 'suave';
}) {
  return (
    <Rebote
      pop={elegida}
      style={[
        estilos.pastilla,
        elegida
          ? tono === 'oscuro'
            ? estilos.pastillaOscura
            : estilos.pastillaSuave
          : estilos.pastillaNormal,
        deshabilitada && !elegida && { opacity: 0.45 },
      ]}
      onPress={onPress}
      disabled={!onPress || (deshabilitada && !elegida)}>
      <Text
        style={[
          estilos.pastillaTexto,
          elegida && tono === 'oscuro' && { color: '#FFFFFF', fontFamily: fonts.sansNegrita },
          elegida && tono === 'suave' && { color: colors.ink, fontFamily: fonts.sansMedia },
        ]}>
        {etiqueta}
      </Text>
    </Rebote>
  );
}

/** Opciones lado a lado: los pares de "cómo te relacionas", los tres niveles de idioma. */
export function Opciones({
  opciones,
  elegida,
  onElegir,
  etiqueta = (v) => v,
}: {
  opciones: string[];
  elegida: string;
  onElegir: (v: string) => void;
  /** Lo que se enseña de cada opción, si no es la opción tal cual (una traducción). */
  etiqueta?: (v: string) => string;
}) {
  return (
    <View style={{ flexDirection: 'row', gap: 9 }}>
      {opciones.map((opcion) => (
        <Rebote
          key={opcion}
          fuera={{ flex: 1 }}
          pop={opcion === elegida}
          style={[estilos.segmento, opcion === elegida && estilos.segmentoElegido]}
          onPress={() => onElegir(opcion)}>
          <Text
            style={[
              estilos.segmentoTexto,
              opcion === elegida && { color: colors.ink, fontFamily: fonts.sansNegrita },
            ]}>
            {etiqueta(opcion)}
          </Text>
        </Rebote>
      ))}
    </View>
  );
}

export function Tarjeta({ children, oscura }: { children: ReactNode; oscura?: boolean }) {
  return <View style={[estilos.tarjeta, oscura && estilos.tarjetaOscura]}>{children}</View>;
}

export function Etiqueta({ children, tono }: { children: string; tono?: 'accent' | 'claro' }) {
  return (
    <Text
      style={[
        text.etiqueta,
        tono === 'accent' && { color: colors.accent },
        tono === 'claro' && { color: colors.accentClaro },
      ]}>
      {children}
    </Text>
  );
}

export function Campo({
  valor,
  onChange,
  placeholder,
  maxLength,
  multiline,
  keyboardType,
  autoFocus,
  onSubmit,
  teclaIntro = 'go',
}: {
  valor: string;
  onChange: (v: string) => void;
  placeholder?: string;
  maxLength?: number;
  multiline?: boolean;
  keyboardType?: 'default' | 'number-pad' | 'email-address';
  autoFocus?: boolean;
  onSubmit?: () => void;
  /** Qué dice la tecla Intro: "Ir" para enviar, "Siguiente" para pasar de campo. */
  teclaIntro?: 'go' | 'next';
}) {
  return (
    <TextInput
      style={[estilos.campo, multiline && estilos.campoLargo]}
      value={valor}
      onChangeText={onChange}
      placeholder={placeholder}
      placeholderTextColor={colors.ink5}
      maxLength={maxLength}
      multiline={multiline}
      keyboardType={keyboardType ?? 'default'}
      autoCapitalize={keyboardType === 'email-address' ? 'none' : 'sentences'}
      autoCorrect={keyboardType !== 'email-address'}
      autoFocus={autoFocus}
      onSubmitEditing={onSubmit}
      returnKeyType={onSubmit ? teclaIntro : 'default'}
    />
  );
}

/** Cabecera del registro: flecha atrás, barra de avance con muelle y "3 de 11". */
export function CabeceraPaso({
  paso,
  total,
  onAtras,
}: {
  paso: number;
  total: number;
  onAtras: () => void;
}) {
  const avance = useRef(new Animated.Value(paso / total)).current;
  useEffect(() => {
    Animated.spring(avance, {
      toValue: paso / total,
      useNativeDriver: false,
      speed: 10,
      bounciness: 10,
    }).start();
  }, [avance, paso, total]);

  return (
    <View style={estilos.cabecera}>
      <Rebote style={estilos.redondo} onPress={onAtras}>
        <Text style={estilos.flecha}>←</Text>
      </Rebote>
      <View style={estilos.barra}>
        <Animated.View
          style={[
            estilos.barraLlena,
            { width: avance.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }) },
          ]}
        />
      </View>
      <Text style={estilos.pasoTexto}>{t('comun.pasoDe', { paso, total })}</Text>
    </View>
  );
}

/** Relieve: un borde inferior más grueso hace que todo parezca un botón de juguete. */
const relieve = { borderWidth: 1.5, borderBottomWidth: 4 };

const estilos = StyleSheet.create({
  boton: {
    height: 58,
    borderRadius: radios.boton,
    alignItems: 'center',
    justifyContent: 'center',
    borderBottomWidth: 5,
  },
  botonAccent: {
    backgroundColor: colors.accent,
    borderColor: colors.accentOscuro,
    shadowColor: colors.accent,
    shadowOpacity: 0.35,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 8 },
    elevation: 6,
  },
  botonOscuro: { backgroundColor: colors.ink, borderColor: '#150A28' },
  botonApagado: {
    backgroundColor: colors.lineFuerte,
    borderColor: colors.trazo,
    shadowOpacity: 0,
    elevation: 0,
  },
  botonPlano: { height: 46, alignItems: 'center', justifyContent: 'center' },
  botonPlanoTexto: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.uva },

  fila: {
    minHeight: 60,
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderRadius: radios.fila,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  filaNormal: { backgroundColor: colors.surface, ...relieve, borderColor: colors.line },
  filaElegida: { backgroundColor: colors.accentWash, ...relieve, borderColor: colors.accent },
  filaTexto: { fontFamily: fonts.sansMedia, fontSize: 16, color: colors.ink },
  filaDetalle: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink3 },

  circulo: { width: 24, height: 24, borderRadius: 999, borderWidth: 2, borderColor: colors.trazo },
  circuloElegido: {
    backgroundColor: colors.accent,
    borderColor: colors.accentWash,
    borderWidth: 5,
  },
  cuadro: { width: 24, height: 24, borderRadius: 8, borderWidth: 2, borderColor: colors.trazo },
  cuadroElegido: { backgroundColor: colors.accent, borderColor: colors.accentWash, borderWidth: 5 },

  pastilla: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: radios.pastilla,
    ...relieve,
    borderBottomWidth: 3,
  },
  pastillaNormal: { backgroundColor: colors.surface, borderColor: colors.line },
  pastillaOscura: { backgroundColor: colors.uva, borderColor: '#6A3FD1' },
  pastillaSuave: { backgroundColor: colors.accentWash, borderColor: colors.accentBorde },
  pastillaTexto: { fontFamily: fonts.sansMedia, fontSize: 14.5, color: colors.ink2 },

  segmento: {
    paddingVertical: 15,
    paddingHorizontal: 12,
    borderRadius: radios.campo,
    backgroundColor: colors.surface,
    ...relieve,
    borderColor: colors.line,
    alignItems: 'center',
  },
  segmentoElegido: { backgroundColor: colors.accentWash, borderColor: colors.accent },
  segmentoTexto: {
    fontFamily: fonts.sansMedia,
    fontSize: 14.5,
    color: colors.ink2,
    textAlign: 'center',
  },

  tarjeta: {
    backgroundColor: colors.surface,
    ...relieve,
    borderBottomWidth: 5,
    borderColor: colors.line,
    borderRadius: radios.tarjeta,
    padding: 20,
    gap: 12,
    shadowColor: colors.uva,
    shadowOpacity: 0.1,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
  },
  tarjetaOscura: { backgroundColor: colors.ink, borderColor: '#150A28' },

  campo: {
    borderWidth: 2,
    borderColor: colors.line,
    borderRadius: radios.campo,
    backgroundColor: colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontFamily: fonts.sans,
    fontSize: 17,
    color: colors.ink,
  },
  campoLargo: { minHeight: 96, textAlignVertical: 'top' },

  cabecera: {
    paddingTop: 58,
    paddingHorizontal: 20,
    paddingBottom: 6,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  redondo: {
    width: 40,
    height: 40,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 18, color: colors.uva },
  barra: {
    flex: 1,
    height: 10,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    overflow: 'hidden',
  },
  barraLlena: { height: 10, borderRadius: 999, backgroundColor: colors.menta },
  pasoTexto: { fontFamily: fonts.display, fontSize: 13, color: colors.ink3 },
});

export { estilos as estilosComunes };
