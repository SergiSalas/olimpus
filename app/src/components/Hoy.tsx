import { useEffect, useRef } from 'react';
import { Animated, Easing, ScrollView, StyleSheet, Text, View } from 'react-native';
import type { Connection } from '../api';
import { Entrada, Rebote } from '../components';
import { colors, fonts } from '../theme';
import { horaCorta as hora, t } from '../i18n';

/** El reparto es a las 4:00 y el cierre a las 22:00: dieciocho horas de día. */
const DURACION_DIA_MS = 18 * 60 * 60 * 1000;
/** La pregunta final llega media hora antes del cierre. */
const ANTES_DE_DECIDIR_MS = 30 * 60 * 1000;

/**
 * El día en una línea: del reparto al cierre, con un punto que marca dónde
 * estás. Cuenta sin párrafos cómo funciona la conversación diaria.
 */
export function TuDia({ closesAt }: { closesAt: string }) {
  const cierre = Date.parse(closesAt);
  const reparto = cierre - DURACION_DIA_MS;
  const decision = cierre - ANTES_DE_DECIDIR_MS;
  const ahora = Date.now();
  const avance = Math.min(1, Math.max(0, (ahora - reparto) / DURACION_DIA_MS));
  const decidiendo = ahora >= decision && ahora < cierre;

  const latido = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const bucle = Animated.loop(
      Animated.timing(latido, {
        toValue: 1,
        duration: 1600,
        easing: Easing.out(Easing.quad),
        useNativeDriver: true,
      }),
    );
    bucle.start();
    return () => bucle.stop();
  }, [latido]);

  return (
    <View style={estilos.tarjeta}>
      <Text style={estilos.titulo}>{t('dia.titulo')}</Text>

      <View style={estilos.pista}>
        <View style={[estilos.relleno, { width: `${avance * 100}%` }]} />
        <View style={[estilos.hito, { left: '97.2%', backgroundColor: colors.sol }]} />
        <View style={[estilos.marcador, { left: `${avance * 100}%` }]}>
          <Animated.View
            style={[
              estilos.onda,
              {
                opacity: latido.interpolate({ inputRange: [0, 1], outputRange: [0.6, 0] }),
                transform: [
                  { scale: latido.interpolate({ inputRange: [0, 1], outputRange: [1, 2.4] }) },
                ],
              },
            ]}
          />
          <View style={estilos.punto} />
        </View>
      </View>

      <View style={estilos.etiquetas}>
        <Text style={estilos.etiqueta}>
          {hora(reparto)}
          {'\n'}
          {t('dia.reparto')}
        </Text>
        <Text style={[estilos.etiqueta, { textAlign: 'center' }]}>
          {hora(decision)}
          {'\n'}
          {t('dia.decision')}
        </Text>
        <Text style={[estilos.etiqueta, { textAlign: 'right' }]}>
          {hora(cierre)}
          {'\n'}
          {t('dia.cierre')}
        </Text>
      </View>

      <Text style={estilos.frase}>
        {ahora >= cierre
          ? t('dia.cerrado')
          : decidiendo
            ? t('dia.decidiendo')
            : t('dia.preguntaremos', { hora: hora(decision) })}
      </Text>
    </View>
  );
}

const COLORES = [colors.accent, colors.uva, colors.cielo, colors.menta, colors.sol];

/**
 * Tus conexiones en fila, como círculos. Sin ninguna, la sección se queda con
 * huecos discontinuos y la explicación: así se entiende qué va ahí.
 */
export function Conexiones({
  conexiones,
  onAbrir,
}: {
  conexiones: Connection[];
  onAbrir: (conversationId: string) => void;
}) {
  return (
    <View style={{ gap: 10 }}>
      <Text style={estilos.titulo}>{t('conexiones.titulo')}</Text>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={estilos.fila}>
        {conexiones.map((c, i) => (
          <Entrada key={c.conversationId} retraso={200 + i * 70}>
            <Rebote style={estilos.conexion} onPress={() => onAbrir(c.conversationId)}>
              <View style={[estilos.circulo, { backgroundColor: COLORES[i % COLORES.length] }]}>
                <Text style={estilos.inicial}>{(c.nickname ?? '?').charAt(0).toUpperCase()}</Text>
              </View>
              <Text style={estilos.nombre} numberOfLines={1}>
                {c.nickname}
              </Text>
            </Rebote>
          </Entrada>
        ))}
        {Array.from({ length: Math.max(0, 3 - conexiones.length) }, (_, i) => (
          <View key={`hueco${i}`} style={estilos.conexion}>
            <View style={[estilos.circulo, estilos.circuloVacio]}>
              <Text style={estilos.mas}>＋</Text>
            </View>
            <Text style={estilos.nombre}> </Text>
          </View>
        ))}
      </ScrollView>
      <Text style={estilos.nota}>
        {conexiones.length === 0 ? t('conexiones.vacio') : t('conexiones.nota')}
      </Text>
    </View>
  );
}

const CONSEJOS = [
  'consejo.0',
  'consejo.1',
  'consejo.2',
  'consejo.3',
  'consejo.4',
  'consejo.5',
  'consejo.6',
  'consejo.7',
  'consejo.8',
  'consejo.9',
] as const;

/** Un consejo distinto cada día, igual para todo el día. */
export function Consejo() {
  const dia = Math.floor(Date.now() / (24 * 60 * 60 * 1000));
  return (
    <View style={estilos.consejo}>
      <Text style={estilos.consejoEmoji}>💡</Text>
      <Text style={estilos.consejoTexto}>{t(CONSEJOS[dia % CONSEJOS.length])}</Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  tarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 26,
    borderWidth: 1.5,
    borderBottomWidth: 5,
    borderColor: colors.line,
    padding: 18,
    gap: 12,
  },
  titulo: { fontFamily: fonts.display, fontSize: 22, color: colors.ink },
  pista: {
    height: 10,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    marginVertical: 8,
    marginHorizontal: 6,
    justifyContent: 'center',
  },
  relleno: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    borderRadius: 999,
    backgroundColor: colors.menta,
  },
  hito: {
    position: 'absolute',
    width: 14,
    height: 14,
    marginLeft: -7,
    borderRadius: 999,
    borderWidth: 3,
    borderColor: '#FFFFFF',
  },
  marcador: {
    position: 'absolute',
    width: 22,
    height: 22,
    marginLeft: -11,
    alignItems: 'center',
    justifyContent: 'center',
  },
  onda: {
    position: 'absolute',
    width: 22,
    height: 22,
    borderRadius: 999,
    backgroundColor: colors.accent,
  },
  punto: {
    width: 22,
    height: 22,
    borderRadius: 999,
    backgroundColor: colors.accent,
    borderWidth: 4,
    borderColor: '#FFFFFF',
  },
  etiquetas: { flexDirection: 'row', justifyContent: 'space-between' },
  etiqueta: { fontFamily: fonts.sansMedia, fontSize: 12, lineHeight: 17, color: colors.ink3 },
  frase: { fontFamily: fonts.sansMedia, fontSize: 13.5, lineHeight: 20, color: colors.ink2 },

  fila: { gap: 14, paddingVertical: 4 },
  conexion: { alignItems: 'center', gap: 6, width: 70 },
  circulo: {
    width: 64,
    height: 64,
    borderRadius: 999,
    borderWidth: 4,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: colors.uva,
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 4 },
    elevation: 3,
  },
  circuloVacio: {
    backgroundColor: 'transparent',
    borderWidth: 2.5,
    borderStyle: 'dashed',
    borderColor: colors.trazo,
    shadowOpacity: 0,
    elevation: 0,
  },
  inicial: { fontFamily: fonts.displayFuerte, fontSize: 26, color: '#FFFFFF' },
  mas: { fontFamily: fonts.displayFuerte, fontSize: 22, color: colors.trazo },
  nombre: { fontFamily: fonts.sansNegrita, fontSize: 13, color: colors.ink },
  nota: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },

  consejo: {
    flexDirection: 'row',
    gap: 12,
    backgroundColor: '#FFF6D9',
    borderRadius: 22,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.sol,
    padding: 16,
  },
  consejoEmoji: { fontSize: 24 },
  consejoTexto: {
    flex: 1,
    fontFamily: fonts.sansMedia,
    fontSize: 14.5,
    lineHeight: 21,
    color: colors.ink,
  },
});
