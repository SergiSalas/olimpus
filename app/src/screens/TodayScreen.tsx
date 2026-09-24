import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Easing,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { fetchConnections, fetchToday, type Connection, type Profile, type Today } from '../api';
import { Entrada, Etiqueta, Flotar, Rebote, Tarjeta } from '../components';
import { useNombreInteres } from '../interests';
import { Conexiones, Consejo, TuDia } from '../components/Hoy';
import { PanelPruebas } from '../components/PanelPruebas';
import { presentacionVista } from '../session';
import { colors, fonts, text } from '../theme';

/**
 * La pantalla principal: con quién hablas hoy.
 *
 * Como solo hay una conversación al día, no hay lista ni botón aparte: la
 * tarjeta de la persona ES el botón que abre el chat.
 *
 * De la otra persona solo llega lo del nivel 0 (edad, dos intereses y a qué
 * distancia está). El apodo, la bio y la foto no están ni en la respuesta del
 * servidor, así que esta pantalla no podría mostrarlos ni por error.
 */
export function TodayScreen({
  token,
  perfil,
  onAbrirChat,
  onPerfil,
}: {
  token: string;
  perfil: Profile;
  onAbrirChat: (conversationId: string) => void;
  onPerfil: () => void;
}) {
  const [today, setToday] = useState<Today | null>(null);
  const [conexiones, setConexiones] = useState<Connection[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    setError(null);
    try {
      const [hoy, misConexiones] = await Promise.all([fetchToday(token), fetchConnections(token)]);
      setToday(hoy);
      setConexiones(misConexiones);
    } catch {
      setError('No se pudo hablar con el servidor.');
    } finally {
      setCargando(false);
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <View style={{ flex: 1 }}>
      <ScrollView
        style={estilos.pantalla}
        contentContainerStyle={estilos.contenido}
        refreshControl={<RefreshControl refreshing={cargando} onRefresh={cargar} />}>
        <View style={estilos.cabecera}>
          <Text style={estilos.marca}>Olimpus</Text>
          <Rebote style={estilos.yo} onPress={onPerfil}>
            <Text style={estilos.yoTexto}>Mi perfil</Text>
            <View style={estilos.yoCirculo}>
              <Text style={estilos.yoInicial}>{perfil.nickname.charAt(0).toUpperCase()}</Text>
            </View>
          </Rebote>
        </View>

        {cargando && !today && (
          <ActivityIndicator style={{ marginTop: 40 }} color={colors.accent} />
        )}
        {error && <Text style={estilos.error}>{error}</Text>}

        {today?.hasConversation && today.partner && today.conversationId && (
          <View style={{ gap: 14 }}>
            <Entrada>
              <Text style={estilos.titular}>Hoy hablas con…</Text>
            </Entrada>
            <TarjetaHoy
              today={today}
              onAbrir={() => today.conversationId && onAbrirChat(today.conversationId)}
            />
          </View>
        )}

        {today && !today.hasConversation && <Buscando today={today} />}

        {today?.hasConversation && today.closesAt && (
          <Entrada retraso={250}>
            <TuDia closesAt={today.closesAt} />
          </Entrada>
        )}

        {today && (
          <Entrada retraso={320}>
            <Conexiones conexiones={conexiones} onAbrir={onAbrirChat} />
          </Entrada>
        )}

        {today && (
          <Entrada retraso={400}>
            <Consejo />
          </Entrada>
        )}
      </ScrollView>
      <PanelPruebas token={token} onHecho={cargar} />
    </View>
  );
}

/**
 * La persona de hoy, y a la vez el botón que abre la conversación. Late, flota
 * y cuenta el tiempo que queda: tiene que apetecer tocarla.
 */
function TarjetaHoy({ today, onAbrir }: { today: Today; onAbrir: () => void }) {
  const nombreInteres = useNombreInteres();
  const [empezada, setEmpezada] = useState(false);
  const [, setReloj] = useState(0);
  const partner = today.partner!;

  useEffect(() => {
    if (today.conversationId) presentacionVista(today.conversationId).then(setEmpezada);
  }, [today.conversationId]);

  // La cuenta atrás se refresca cada medio minuto.
  useEffect(() => {
    const t = setInterval(() => setReloj((n) => n + 1), 30_000);
    return () => clearInterval(t);
  }, []);

  const comunes = today.sharedInterests.length;

  return (
    <Entrada retraso={120}>
      <Rebote style={estilos.tarjetaHoy} onPress={onAbrir}>
        <View style={estilos.filaArriba}>
          <Text style={estilos.etiquetaHoy}>NIVEL 0 · MATCH</Text>
          {today.closesAt && (
            <View style={estilos.quedan}>
              <Text style={estilos.quedanTexto}>⏳ quedan {quedanHasta(today.closesAt)}</Text>
            </View>
          )}
        </View>

        <View style={estilos.centroHoy}>
          <Halo />
          <Halo retraso={900} />
          <Flotar distancia={6} giro={4}>
            <View style={estilos.bola}>
              <Text style={estilos.bolaTexto}>?</Text>
            </View>
          </Flotar>
        </View>

        <Text style={estilos.edad}>{partner.age} años</Text>
        <Text style={estilos.distancia}>
          a unos {partner.approxDistanceKm} km · foto y apodo, todavía no
        </Text>

        <View style={estilos.chips}>
          {partner.interests.map((interes) => {
            const comun = today.sharedInterests.includes(interes);
            return (
              <View key={interes} style={[estilos.chip, comun && estilos.chipComun]}>
                <Text style={[estilos.chipTexto, comun && { color: colors.ink }]}>
                  {nombreInteres(interes)}
                </Text>
              </View>
            );
          })}
        </View>
        {comunes > 0 && (
          <Text style={estilos.comunes}>
            {comunes === 1 ? 'Un interés' : `${comunes} intereses`} en común contigo
          </Text>
        )}

        <View style={estilos.boton}>
          <Text style={estilos.botonTexto}>
            {empezada ? 'Seguir hablando' : 'Toca para empezar a hablar'}
          </Text>
          <Flotar distancia={3} giro={0} duracion={1200}>
            <Text style={estilos.botonTexto}>→</Text>
          </Flotar>
        </View>
      </Rebote>
    </Entrada>
  );
}

/** Un aro que sale de la bola y se desvanece, en bucle: la tarjeta "late". */
function Halo({ retraso = 0 }: { retraso?: number }) {
  const v = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const bucle = Animated.loop(
      Animated.timing(v, {
        toValue: 1,
        duration: 1800,
        easing: Easing.out(Easing.quad),
        useNativeDriver: true,
      }),
    );
    const t = setTimeout(() => bucle.start(), retraso);
    return () => {
      clearTimeout(t);
      bucle.stop();
    };
  }, [v, retraso]);
  return (
    <Animated.View
      pointerEvents="none"
      style={[
        estilos.halo,
        {
          opacity: v.interpolate({ inputRange: [0, 1], outputRange: [0.55, 0] }),
          transform: [{ scale: v.interpolate({ inputRange: [0, 1], outputRange: [1, 1.8] }) }],
        },
      ]}
    />
  );
}

function quedanHasta(iso: string): string {
  const minutos = Math.max(0, Math.round((Date.parse(iso) - Date.now()) / 60_000));
  const horas = Math.floor(minutos / 60);
  return horas > 0 ? `${horas} h ${minutos % 60} min` : `${minutos} min`;
}

/**
 * Cuando todavía no hay nadie. En vez de una pantalla vacía, se explica cuándo
 * llega el siguiente reparto: una espera que se entiende molesta mucho menos.
 */
function Buscando({ today }: { today: Today }) {
  const pulso = useRef(new Animated.Value(0)).current;
  const giro = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const animacion = Animated.loop(
      Animated.sequence([
        Animated.timing(pulso, {
          toValue: 1,
          duration: 1300,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulso, {
          toValue: 0,
          duration: 1300,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );
    const vuelta = Animated.loop(
      Animated.timing(giro, {
        toValue: 1,
        duration: 9000,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );
    animacion.start();
    vuelta.start();
    return () => {
      animacion.stop();
      vuelta.stop();
    };
  }, [pulso, giro]);

  const escala = pulso.interpolate({ inputRange: [0, 1], outputRange: [1, 1.07] });
  const opacidad = pulso.interpolate({ inputRange: [0, 1], outputRange: [1, 0.7] });
  const rotacion = giro.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });

  return (
    <View style={estilos.buscando}>
      <Animated.View
        style={[
          estilos.aroGrande,
          { transform: [{ scale: escala }, { rotate: rotacion }], opacity: opacidad },
        ]}>
        <View style={estilos.aroMedio}>
          <View style={estilos.nucleo} />
        </View>
      </Animated.View>

      <Text style={[text.titulo, { textAlign: 'center' }]}>Tu emparejamiento de hoy</Text>
      <Text style={[text.cuerpo, { textAlign: 'center', maxWidth: 300 }]}>
        Se hace solo, una vez al día y a la misma hora. No eliges tú: se mira que le convenga a los
        dos.
      </Text>

      <Tarjeta>
        <Etiqueta>Cuándo</Etiqueta>
        <Text style={estilos.cuando}>
          {today.nextRoundAt ? fechaYHora(today.nextRoundAt) : 'Pronto'}
        </Text>
        <Text style={estilos.pistaPersona}>
          Si llevas días sin encaje, ampliamos la búsqueda poco a poco y te avisamos.
        </Text>
      </Tarjeta>
    </View>
  );
}

function fechaYHora(iso: string): string {
  const fecha = new Date(iso);
  const texto = fecha.toLocaleString([], {
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
  });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  contenido: { paddingHorizontal: 22, paddingTop: 58, paddingBottom: 40, gap: 16, flexGrow: 1 },
  cabecera: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  marca: { fontFamily: fonts.displayFuerte, fontSize: 30, color: colors.accent },
  yo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: colors.surface,
    borderRadius: 999,
    borderWidth: 1.5,
    borderBottomWidth: 3,
    borderColor: colors.line,
    paddingLeft: 14,
    padding: 4,
  },
  yoTexto: { fontFamily: fonts.sansNegrita, fontSize: 13, color: colors.ink2 },
  yoCirculo: {
    width: 34,
    height: 34,
    borderRadius: 999,
    backgroundColor: colors.uva,
    alignItems: 'center',
    justifyContent: 'center',
  },
  yoInicial: { fontFamily: fonts.displayFuerte, fontSize: 16, color: '#FFFFFF' },
  titular: { fontFamily: fonts.display, fontSize: 30, color: colors.ink },

  tarjetaHoy: {
    backgroundColor: colors.uva,
    borderRadius: 32,
    borderBottomWidth: 7,
    borderColor: '#6A3FD1',
    padding: 22,
    alignItems: 'center',
    shadowColor: colors.uva,
    shadowOpacity: 0.35,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 10 },
    elevation: 8,
  },
  filaArriba: {
    alignSelf: 'stretch',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  etiquetaHoy: {
    fontFamily: fonts.sansNegrita,
    fontSize: 11,
    letterSpacing: 1.4,
    color: colors.onInk2,
  },
  quedan: {
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 999,
    paddingHorizontal: 11,
    paddingVertical: 6,
  },
  quedanTexto: { fontFamily: fonts.sansNegrita, fontSize: 12, color: '#FFFFFF' },
  centroHoy: { height: 170, width: 170, alignItems: 'center', justifyContent: 'center' },
  halo: {
    position: 'absolute',
    width: 104,
    height: 104,
    borderRadius: 999,
    borderWidth: 3,
    borderColor: colors.sol,
  },
  bola: {
    width: 104,
    height: 104,
    borderRadius: 999,
    backgroundColor: colors.sol,
    borderWidth: 5,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  bolaTexto: { fontFamily: fonts.displayFuerte, fontSize: 48, color: colors.ink },
  edad: { fontFamily: fonts.displayFuerte, fontSize: 34, color: '#FFFFFF' },
  distancia: {
    fontFamily: fonts.sansMedia,
    fontSize: 14,
    color: colors.onInk2,
    textAlign: 'center',
    marginTop: 2,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 7,
    marginTop: 16,
  },
  chip: {
    backgroundColor: 'rgba(255,255,255,0.16)',
    borderRadius: 999,
    paddingHorizontal: 13,
    paddingVertical: 8,
  },
  chipComun: { backgroundColor: colors.sol },
  chipTexto: { fontFamily: fonts.sansNegrita, fontSize: 13.5, color: '#FFFFFF' },
  comunes: { fontFamily: fonts.sansMedia, fontSize: 12.5, color: colors.onInk2, marginTop: 8 },
  boton: {
    alignSelf: 'stretch',
    marginTop: 20,
    height: 56,
    borderRadius: 20,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 4,
    borderColor: colors.trazo,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  botonTexto: { fontFamily: fonts.sansNegrita, fontSize: 16, color: colors.uva },

  pistaPersona: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },

  buscando: { alignItems: 'center', gap: 22, paddingVertical: 24 },
  aroGrande: {
    width: 150,
    height: 150,
    borderRadius: 999,
    borderWidth: 4,
    borderStyle: 'dashed',
    borderColor: colors.sol,
    alignItems: 'center',
    justifyContent: 'center',
  },
  aroMedio: {
    width: 96,
    height: 96,
    borderRadius: 999,
    borderWidth: 4,
    borderStyle: 'dotted',
    borderColor: colors.uva,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nucleo: {
    width: 48,
    height: 48,
    borderRadius: 999,
    backgroundColor: colors.accent,
    borderWidth: 5,
    borderColor: colors.accentBorde,
  },
  cuando: { fontFamily: fonts.sansNegrita, fontSize: 15.5, color: colors.ink },

  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.error,
    textAlign: 'center',
  },
});
