import { useEffect, useRef, useState, type ComponentType } from 'react';
import { Animated, Easing, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, type Profile } from '../api';
import { Boton, CabeceraPaso, Entrada, Etiqueta, Flotar } from '../components';
import { BLOQUES_LISTOS, FotoFallida, VACIO, guardar, type Borrador } from '../borrador';
import {
  Apodo,
  Busco,
  ComoHablas,
  EdadYDistancia,
  Foto,
  Genero,
  Idiomas,
  Intencion,
  Intereses,
  Nacimiento,
  Preguntas,
  TrabajoYSitio,
  Ubicacion,
  type Props,
} from '../preguntas';
import { colors, espacios, text } from '../theme';
import { t } from '../i18n';

/**
 * El registro de la primera vez, en seis bloques.
 *
 * Antes era una pregunta por pantalla, y con las preguntas nuevas habrían sido
 * diecisiete «Continuar» seguidos. Agrupadas no se pide ni un dato menos: lo que
 * cambia es cuántas veces hay que levantar el dedo.
 *
 * Cambiar lo ya guardado no pasa por aquí. Para eso está la pantalla de editar,
 * que lo enseña todo junto, porque quien vuelve casi siempre viene a tocar una
 * cosa y no catorce.
 */
export function OnboardingScreen({
  token,
  onTerminado,
}: {
  token: string;
  onTerminado: (perfil: Profile) => void;
}) {
  const [bloque, setBloque] = useState(0);
  /** Hacia dónde se movió el último cambio de bloque: el nuevo llega por ese lado. */
  const [sentido, setSentido] = useState<1 | -1>(1);
  const [b, setB] = useState<Borrador>(VACIO);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** La pregunta a la que se acaba de pasar sola; null al entrar en un bloque. */
  const [activa, setActiva] = useState<number | null>(null);

  const scroll = useRef<ScrollView>(null);
  /** Dónde empieza cada pregunta dentro del bloque, para poder deslizarse hasta ella. */
  const alturas = useRef<number[]>([]);
  const latido = useRef(new Animated.Value(1)).current;

  const cambiar = (cambio: Partial<Borrador>) => setB((actual) => ({ ...actual, ...cambio }));

  function irA(siguiente: number) {
    setSentido(siguiente > bloque ? 1 : -1);
    setBloque(siguiente);
    setActiva(null);
    alturas.current = [];
  }

  /**
   * Una pregunta quedó contestada: la pantalla se desliza a la siguiente, que da
   * un latido para que se vea a dónde ha ido. Tras la última, baja hasta el botón.
   * La pausa deja ver antes el saltito de la opción elegida.
   */
  function pasarDe(pregunta: number) {
    setTimeout(() => {
      const y = alturas.current[pregunta + 1];
      if (y === undefined) {
        scroll.current?.scrollToEnd({ animated: true });
        return;
      }
      scroll.current?.scrollTo({ y: Math.max(0, y - 12), animated: true });
      setActiva(pregunta + 1);
    }, 250);
  }

  useEffect(() => {
    if (activa === null) return;
    latido.setValue(0);
    Animated.timing(latido, {
      toValue: 1,
      duration: 600,
      easing: Easing.out(Easing.quad),
      useNativeDriver: true,
    }).start();
  }, [activa, latido]);

  const bloques: {
    nombre: string;
    emoji: string;
    contenido: ComponentType<Props>[];
    listo: boolean;
  }[] = [
    {
      nombre: t('bloque.quien'),
      emoji: '👋',
      contenido: [Apodo, Nacimiento, Genero],
      listo: BLOQUES_LISTOS[0](b),
    },
    {
      nombre: t('bloque.busca'),
      emoji: '🔍',
      contenido: [Busco, EdadYDistancia],
      listo: BLOQUES_LISTOS[1](b),
    },
    {
      nombre: t('bloque.donde'),
      emoji: '🌍',
      contenido: [Ubicacion, Idiomas],
      listo: BLOQUES_LISTOS[2](b),
    },
    {
      nombre: t('bloque.hablas'),
      emoji: '🗣️',
      contenido: [ComoHablas, Intencion],
      listo: BLOQUES_LISTOS[3](b),
    },
    {
      nombre: t('bloque.cuentas'),
      emoji: '💫',
      contenido: [Intereses, Preguntas, TrabajoYSitio],
      listo: BLOQUES_LISTOS[4](b),
    },
    {
      nombre: t('bloque.foto'),
      emoji: '📸',
      contenido: [(p) => <Foto {...p} token={token} yaHayUna={false} />],
      listo: b.foto !== null,
    },
  ];

  const actual = bloques[bloque];
  const ultimo = bloque === bloques.length - 1;

  async function terminar() {
    setOcupado(true);
    setError(null);
    try {
      onTerminado(await guardar(token, b));
    } catch (e) {
      // El registro quedó guardado y solo falló la foto: se dice tal cual, con
      // el motivo, en vez de mandar a nadie a rehacer seis bloques para nada.
      if (e instanceof FotoFallida) {
        setError(t('registro.fotoFallo', { motivo: e.message }));
      } else {
        setError(e instanceof ApiError ? e.message : t('registro.error'));
      }
    } finally {
      setOcupado(false);
    }
  }

  return (
    <View style={estilos.pantalla}>
      <CabeceraPaso
        paso={bloque + 1}
        total={bloques.length}
        onAtras={() => irA(Math.max(0, bloque - 1))}
      />

      <View style={estilos.cuerpo}>
        {/*
          La key hace que cada bloque nuevo empiece arriba del todo y entre de
          cero, por el lado hacia el que se va.
        */}
        <ScrollView
          key={bloque}
          ref={scroll}
          style={{ flex: 1 }}
          contentContainerStyle={{ paddingBottom: 20 }}
          keyboardShouldPersistTaps="handled"
          automaticallyAdjustKeyboardInsets>
          <Entrada desdeX={sentido * 60} style={estilos.encabezado}>
            <Flotar distancia={6}>
              <Text style={estilos.emoji}>{actual.emoji}</Text>
            </Flotar>
            <Etiqueta tono="accent">{actual.nombre}</Etiqueta>
          </Entrada>
          {actual.contenido.map((Pregunta, i) => (
            <View
              key={i}
              onLayout={(e) => {
                alturas.current[i] = e.nativeEvent.layout.y;
              }}>
              <Entrada retraso={120 + i * 110} desdeX={sentido * 60}>
                <Animated.View
                  style={
                    activa === i && {
                      transform: [
                        {
                          scale: latido.interpolate({
                            inputRange: [0, 0.4, 1],
                            outputRange: [1, 1.03, 1],
                          }),
                        },
                      ],
                    }
                  }>
                  <Pregunta
                    b={b}
                    cambiar={cambiar}
                    siguiente={() => pasarDe(i)}
                    enfocar={activa === i}
                  />
                </Animated.View>
              </Entrada>
            </View>
          ))}
        </ScrollView>

        <Boton
          texto={ultimo ? t('registro.terminar') : t('registro.continuar')}
          onPress={ultimo ? terminar : () => irA(bloque + 1)}
          deshabilitado={!actual.listo}
          ocupado={ocupado}
        />
      </View>

      {error && <Text style={estilos.error}>{error}</Text>}
    </View>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  cuerpo: {
    flex: 1,
    paddingHorizontal: espacios.pantalla,
    paddingTop: 20,
    paddingBottom: 30,
    gap: 14,
  },
  encabezado: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 18 },
  emoji: { fontSize: 30 },
  error: {
    ...text.ayuda,
    color: colors.error,
    textAlign: 'center',
    paddingHorizontal: 26,
    paddingBottom: 12,
  },
});
