import { useRef, useState } from 'react';
import { Animated, PanResponder, StyleSheet, View } from 'react-native';
import { colors } from '../theme';

const POMO = 32;

/**
 * Un deslizador con uno o dos tiradores. Con dos es un rango (desde / hasta) y
 * el tramo elegido se colorea entre ellos; con uno se colorea desde el inicio.
 *
 * Se arrastra el tirador, no la pista: cada uno recuerda su valor al agarrarlo y
 * suma lo que se mueve el dedo, así no hace falta saber dónde está la pista en
 * la pantalla (que además llega animada).
 */
export function Rango({
  min,
  max,
  paso,
  valores,
  onChange,
  color = colors.accent,
}: {
  min: number;
  max: number;
  paso: number;
  /** Uno o dos valores. Con dos, el primero nunca pasa del segundo. */
  valores: number[];
  onChange: (valores: number[]) => void;
  color?: string;
}) {
  const [ancho, setAncho] = useState(0);
  const inicio = useRef(0);

  const x = (v: number) => (ancho * (v - min)) / (max - min);

  function mover(i: number, dx: number) {
    if (!ancho) return;
    let v = Math.round((inicio.current + (dx / ancho) * (max - min)) / paso) * paso;
    v = Math.min(max, Math.max(min, v));
    if (valores.length === 2) v = i === 0 ? Math.min(v, valores[1]) : Math.max(v, valores[0]);
    if (v === valores[i]) return;
    const nuevos = [...valores];
    nuevos[i] = v;
    onChange(nuevos);
  }

  const desde = valores.length === 2 ? x(valores[0]) : 0;
  const hasta = x(valores[valores.length - 1]);

  return (
    <View style={estilos.caja}>
      <View style={estilos.pista} onLayout={(e) => setAncho(e.nativeEvent.layout.width)}>
        <View
          style={[estilos.relleno, { left: desde, width: hasta - desde, backgroundColor: color }]}
        />
        {ancho > 0 &&
          valores.map((v, i) => (
            <Pomo
              key={i}
              x={x(v)}
              color={color}
              onInicio={() => (inicio.current = valores[i])}
              onMover={(dx) => mover(i, dx)}
            />
          ))}
      </View>
    </View>
  );
}

function Pomo({
  x,
  color,
  onInicio,
  onMover,
}: {
  x: number;
  color: string;
  onInicio: () => void;
  onMover: (dx: number) => void;
}) {
  const escala = useRef(new Animated.Value(1)).current;
  // El PanResponder se crea una vez; lo que cambia en cada render se lee de aquí.
  const ultimos = useRef({ onInicio, onMover });
  ultimos.current = { onInicio, onMover };

  const soltar = () =>
    Animated.spring(escala, { toValue: 1, useNativeDriver: true, bounciness: 14 }).start();

  const gesto = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      // Mientras se arrastra, la pantalla no se lo quita para hacer scroll.
      onPanResponderTerminationRequest: () => false,
      onPanResponderGrant: () => {
        ultimos.current.onInicio();
        Animated.spring(escala, { toValue: 1.3, useNativeDriver: true, bounciness: 14 }).start();
      },
      onPanResponderMove: (_, g) => ultimos.current.onMover(g.dx),
      onPanResponderRelease: soltar,
      onPanResponderTerminate: soltar,
    }),
  ).current;

  return (
    <Animated.View
      {...gesto.panHandlers}
      hitSlop={{ top: 16, bottom: 16, left: 12, right: 12 }}
      style={[
        estilos.pomo,
        { left: x - POMO / 2, borderColor: color, transform: [{ scale: escala }] },
      ]}
    />
  );
}

const estilos = StyleSheet.create({
  // Margen a los lados para que los tiradores quepan enteros en los extremos.
  caja: { paddingHorizontal: POMO / 2, paddingVertical: 14 },
  pista: {
    height: 10,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    justifyContent: 'center',
  },
  relleno: { position: 'absolute', top: 0, bottom: 0, borderRadius: 999 },
  pomo: {
    position: 'absolute',
    width: POMO,
    height: POMO,
    borderRadius: 999,
    backgroundColor: '#FFFFFF',
    borderWidth: 5,
    shadowColor: colors.uva,
    shadowOpacity: 0.25,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    elevation: 4,
  },
});
