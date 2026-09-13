import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Easing,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { fetchToday, type Profile, type Today } from '../api';
import { Boton, Etiqueta, Pastilla, Tarjeta } from '../components';
import { useNombreInteres } from '../interests';
import { colors, fonts, text } from '../theme';
import { useRef } from 'react';

/**
 * La pantalla principal: con quién hablas hoy.
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
  onSalir,
}: {
  token: string;
  perfil: Profile;
  onAbrirChat: (conversationId: string) => void;
  onPerfil: () => void;
  onSalir: () => void;
}) {
  const [today, setToday] = useState<Today | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const nombreInteres = useNombreInteres();

  const cargar = useCallback(async () => {
    setError(null);
    try {
      setToday(await fetchToday(token));
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
    <ScrollView
      style={estilos.pantalla}
      contentContainerStyle={estilos.contenido}
      refreshControl={<RefreshControl refreshing={cargando} onRefresh={cargar} />}>
      <View style={estilos.cabecera}>
        <Text style={estilos.marca}>Olimpus</Text>
        <Pressable onPress={onPerfil}>
          <Text style={estilos.enlaceCabecera}>{perfil.nickname}</Text>
        </Pressable>
      </View>

      {cargando && !today && <ActivityIndicator style={{ marginTop: 40 }} color={colors.accent} />}
      {error && <Text style={estilos.error}>{error}</Text>}

      {today?.hasConversation && today.partner && (
        <View style={{ gap: 18 }}>
          <Etiqueta>Hoy · nivel 0</Etiqueta>
          <Text style={text.tituloGrande}>
            Tu conversación{'\n'}de hoy
          </Text>

          <Tarjeta>
            <View style={estilos.filaPersona}>
              <View style={estilos.circuloFoto}>
                <Text style={estilos.interrogante}>?</Text>
              </View>
              <View style={{ gap: 3, flexShrink: 1 }}>
                <Text style={estilos.persona}>
                  {today.partner.age} años · a {today.partner.approxDistanceKm} km
                </Text>
                <Text style={estilos.pistaPersona}>Foto y apodo, todavía no</Text>
              </View>
            </View>

            <View style={estilos.separador} />

            <View style={estilos.rejilla}>
              {today.partner.interests.map((interes) => (
                <Pastilla key={interes} texto={nombreInteres(interes)} elegida tono="suave" />
              ))}
            </View>

            <Text style={estilos.pistaPersona}>
              {today.sharedInterests.length > 0
                ? `${today.sharedInterests.length === 1 ? 'Un interés' : 'Dos intereses'} en común contigo. El resto se desbloquea hablando.`
                : 'El resto de su perfil se desbloquea hablando.'}
            </Text>
          </Tarjeta>

          <Boton
            texto="Empezar a hablar"
            tono="oscuro"
            onPress={() => today.conversationId && onAbrirChat(today.conversationId)}
          />
          <Text style={estilos.cierre}>
            Cierra hoy a las {hora(today.closesAt)}, para los dos.
          </Text>
        </View>
      )}

      {today && !today.hasConversation && <Buscando today={today} />}

      <View style={{ flex: 1 }} />
      <Pressable style={{ paddingVertical: 14 }} onPress={onSalir}>
        <Text style={estilos.enlaceFlojo}>Cerrar sesión</Text>
      </Pressable>
    </ScrollView>
  );
}

/**
 * Cuando todavía no hay nadie. En vez de una pantalla vacía, se explica cuándo
 * llega el siguiente reparto: una espera que se entiende molesta mucho menos.
 */
function Buscando({ today }: { today: Today }) {
  const pulso = useRef(new Animated.Value(0)).current;

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
    animacion.start();
    return () => animacion.stop();
  }, [pulso]);

  const escala = pulso.interpolate({ inputRange: [0, 1], outputRange: [1, 1.07] });
  const opacidad = pulso.interpolate({ inputRange: [0, 1], outputRange: [1, 0.55] });

  return (
    <View style={estilos.buscando}>
      <Animated.View
        style={[estilos.aroGrande, { transform: [{ scale: escala }], opacity: opacidad }]}>
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

function hora(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
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
  contenido: { paddingHorizontal: 26, paddingTop: 58, paddingBottom: 40, gap: 16, flexGrow: 1 },
  cabecera: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  marca: { fontFamily: fonts.serif, fontSize: 24, color: colors.accent },
  enlaceCabecera: { fontFamily: fonts.sansMedia, fontSize: 14, color: colors.ink2 },

  filaPersona: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  circuloFoto: {
    width: 56,
    height: 56,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  interrogante: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.ink5 },
  persona: { fontFamily: fonts.sansNegrita, fontSize: 16, color: colors.ink },
  pistaPersona: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  separador: { height: 1, backgroundColor: colors.lineSuave },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  cierre: {
    fontFamily: fonts.sans,
    fontSize: 13,
    color: colors.ink3,
    textAlign: 'center',
  },

  buscando: { alignItems: 'center', gap: 22, paddingVertical: 24 },
  aroGrande: {
    width: 150,
    height: 150,
    borderRadius: 999,
    borderWidth: 1.5,
    borderColor: '#E0D4C2',
    alignItems: 'center',
    justifyContent: 'center',
  },
  aroMedio: {
    width: 96,
    height: 96,
    borderRadius: 999,
    borderWidth: 1.5,
    borderColor: '#DCCFBB',
    alignItems: 'center',
    justifyContent: 'center',
  },
  nucleo: { width: 44, height: 44, borderRadius: 999, backgroundColor: colors.accent },
  cuando: { fontFamily: fonts.sansNegrita, fontSize: 15.5, color: colors.ink },

  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.error,
    textAlign: 'center',
  },
  enlaceFlojo: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.ink4, textAlign: 'center' },
});
