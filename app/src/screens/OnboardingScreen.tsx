import Slider from '@react-native-community/slider';
import * as Location from 'expo-location';
import { useEffect, useState, type ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import {
  ApiError,
  fetchInterests,
  saveProfile,
  type Gender,
  type Intent,
  type Interest,
  type LanguageLevel,
  type Profile,
} from '../api';
import {
  Boton,
  CabeceraPaso,
  Campo,
  DosOpciones,
  Etiqueta,
  FilaOpcion,
  PantallaPaso,
  Pastilla,
  Tarjeta,
} from '../components';
import {
  CONVERSACIONES,
  RASGOS,
  profundidadDe,
  sociabilidadDe,
  type Rasgos,
  type TipoConversacion,
} from '../mapeo';
import { colors, fonts, radios, text } from '../theme';

const GENEROS: { valor: Gender; etiqueta: string }[] = [
  { valor: 'WOMAN', etiqueta: 'Mujer' },
  { valor: 'MAN', etiqueta: 'Hombre' },
  { valor: 'NON_BINARY', etiqueta: 'No binarie' },
  { valor: 'OTHER', etiqueta: 'Prefiero no decirlo' },
];

const BUSCO: { valor: Gender; etiqueta: string }[] = [
  { valor: 'WOMAN', etiqueta: 'Mujeres' },
  { valor: 'MAN', etiqueta: 'Hombres' },
  { valor: 'NON_BINARY', etiqueta: 'Personas no binarias' },
  { valor: 'OTHER', etiqueta: 'Todo el mundo' },
];

const INTENCIONES: { valor: Intent; etiqueta: string; pista: string }[] = [
  { valor: 'FRIENDSHIP', etiqueta: 'Amistad', pista: 'Gente con quien hablar' },
  { valor: 'DATING', etiqueta: 'Citas', pista: 'Conocer sin prisa' },
  { valor: 'RELATIONSHIP', etiqueta: 'Pareja', pista: 'Algo que dure' },
  { valor: 'CASUAL', etiqueta: 'Algo casual', pista: 'Sin planes a futuro' },
];

const IDIOMAS = [
  { code: 'es', etiqueta: 'Español' },
  { code: 'en', etiqueta: 'Inglés' },
  { code: 'ca', etiqueta: 'Catalán' },
  { code: 'gl', etiqueta: 'Gallego' },
  { code: 'eu', etiqueta: 'Euskera' },
  { code: 'fr', etiqueta: 'Francés' },
  { code: 'pt', etiqueta: 'Portugués' },
  { code: 'it', etiqueta: 'Italiano' },
  { code: 'de', etiqueta: 'Alemán' },
  { code: 'ar', etiqueta: 'Árabe' },
];

/**
 * Once pantallas en total, contando la de intereses, que es la última y la que
 * remata el registro. Este número tiene que incluirla: si se queda corto, el
 * botón deja de avanzar en la penúltima y el registro se atasca sin decir nada.
 */
const TOTAL = 11;
const INTERESES_MIN = 5;
const INTERESES_MAX = 8;

type Borrador = {
  apodo: string;
  bio: string;
  fecha: string;
  genero: Gender | null;
  busco: Gender[];
  edadMin: number;
  edadMax: number;
  distancia: number;
  ubicacion: { latitude: number; longitude: number } | null;
  idiomas: string[];
  rasgos: Rasgos;
  conversacion: TipoConversacion;
  intencion: Intent | null;
  intereses: string[];
};

const VACIO: Borrador = {
  apodo: '',
  bio: '',
  fecha: '',
  genero: null,
  busco: [],
  edadMin: 27,
  edadMax: 38,
  distancia: 30,
  ubicacion: null,
  idiomas: ['es'],
  rasgos: { largo: 'Mensajes largos', ritmo: 'Cuando puedo', rol: 'Pregunto mucho' },
  conversacion: 'A fondo',
  intencion: null,
  intereses: [],
};

export function OnboardingScreen({
  token,
  onTerminado,
}: {
  token: string;
  onTerminado: (perfil: Profile) => void;
}) {
  const [paso, setPaso] = useState(1);
  const [b, setB] = useState<Borrador>(VACIO);
  const [catalogo, setCatalogo] = useState<Interest[]>([]);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchInterests()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

  const cambiar = (cambio: Partial<Borrador>) => setB((actual) => ({ ...actual, ...cambio }));
  const siguiente = () => setPaso((p) => Math.min(TOTAL, p + 1));
  const atras = () => setPaso((p) => Math.max(1, p - 1));

  const edad = edadDe(b.fecha);

  function teclear(tecla: string) {
    cambiar({
      fecha: tecla === 'del' ? b.fecha.slice(0, -1) : b.fecha.length < 8 ? b.fecha + tecla : b.fecha,
    });
  }

  async function pedirUbicacion() {
    setOcupado(true);
    setError(null);
    try {
      const permiso = await Location.requestForegroundPermissionsAsync();
      if (permiso.status !== 'granted') {
        setError('Sin ubicación no podemos calcular la distancia. Puedes darla en Ajustes.');
        return;
      }
      const posicion = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Low });
      cambiar({
        ubicacion: { latitude: posicion.coords.latitude, longitude: posicion.coords.longitude },
      });
    } catch {
      setError('No se pudo leer la ubicación.');
    } finally {
      setOcupado(false);
    }
  }

  async function terminar() {
    setOcupado(true);
    setError(null);
    try {
      const perfil = await saveProfile(token, {
        nickname: b.apodo.trim(),
        bio: b.bio.trim(),
        birthDate: `${b.fecha.slice(4)}-${b.fecha.slice(2, 4)}-${b.fecha.slice(0, 2)}`,
        gender: b.genero!,
        seeking: b.busco.length === 4 ? ['WOMAN', 'MAN', 'NON_BINARY', 'OTHER'] : b.busco,
        ageMin: b.edadMin,
        ageMax: b.edadMax,
        maxDistanceKm: b.distancia,
        latitude: b.ubicacion!.latitude,
        longitude: b.ubicacion!.longitude,
        languages: b.idiomas.map((code) => ({ code, level: 'NATIVE' as LanguageLevel })),
        sociability: sociabilidadDe(b.rasgos),
        conversationDepth: profundidadDe(b.conversacion),
        intent: b.intencion!,
        interests: b.intereses,
      });
      onTerminado(perfil);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo guardar el registro.');
    } finally {
      setOcupado(false);
    }
  }

  const pantallas: Record<number, ReactNode> = {
    1: (
      <PantallaPaso
        titulo="¿Cómo quieres que te llamen?"
        ayuda="Tu apodo aparece en el nivel 1, en cuanto escribáis los dos. Tu nombre real no hace falta."
        listo={b.apodo.trim().length >= 2}
        onSiguiente={siguiente}>
        <Campo
          valor={b.apodo}
          onChange={(v) => cambiar({ apodo: v })}
          placeholder="Tu apodo"
          maxLength={20}
          autoFocus
        />
      </PantallaPaso>
    ),

    2: (
      <PantallaPaso
        titulo="Tu fecha de nacimiento"
        ayuda="Solo para mayores de 18. Antes de la beta verificaremos la edad de verdad, no con una casilla."
        listo={edad !== null && edad >= 18}
        onSiguiente={siguiente}>
        <View style={estilos.fecha}>
          {['D', 'D', '/', 'M', 'M', '/', 'A', 'A', 'A', 'A'].map((ph, posicion) => {
            if (ph === '/') {
              return (
                <Text key={`sep${posicion}`} style={estilos.separadorFecha}>
                  /
                </Text>
              );
            }
            const indice = posicion - (posicion > 5 ? 2 : posicion > 2 ? 1 : 0);
            const puesto = b.fecha[indice];
            return (
              <View
                key={posicion}
                style={[estilos.casilla, b.fecha.length === indice && estilos.casillaActiva]}>
                <Text style={[estilos.casillaTexto, !puesto && { color: '#CDC3B2' }]}>
                  {puesto ?? ph}
                </Text>
              </View>
            );
          })}
        </View>

        <Text style={[estilos.notaEdad, edad !== null && edad < 18 && { color: colors.error }]}>
          {edad === null
            ? 'Escribe día, mes y año'
            : edad < 18
              ? 'Olimpus es solo para mayores de 18'
              : `Tienes ${edad} años`}
        </Text>

        <View style={{ flex: 1 }} />

        <View style={estilos.teclado}>
          {['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del'].map((tecla, i) => (
            <Pressable
              key={i}
              style={[estilos.tecla, tecla === '' && { opacity: 0 }]}
              disabled={tecla === ''}
              onPress={() => teclear(tecla)}>
              <Text style={estilos.teclaTexto}>{tecla === 'del' ? '⌫' : tecla}</Text>
            </Pressable>
          ))}
        </View>
      </PantallaPaso>
    ),

    3: (
      <PantallaPaso
        titulo="Tu género"
        ayuda="Se usa para emparejar. En el nivel 0 solo se ve tu edad, dos intereses y tu zona."
        listo={b.genero !== null}
        onSiguiente={siguiente}>
        <View style={{ gap: 10 }}>
          {GENEROS.map((g) => (
            <FilaOpcion
              key={g.valor}
              etiqueta={g.etiqueta}
              elegida={b.genero === g.valor}
              onPress={() => cambiar({ genero: g.valor })}
            />
          ))}
        </View>
      </PantallaPaso>
    ),

    4: (
      <PantallaPaso
        titulo="¿Con quién quieres hablar?"
        ayuda="Puedes marcar varias. Solo emparejamos si encaja por los dos lados."
        listo={b.busco.length > 0}
        onSiguiente={siguiente}>
        <View style={{ gap: 10 }}>
          {BUSCO.map((g) => (
            <FilaOpcion
              key={g.valor}
              etiqueta={g.etiqueta}
              varias
              elegida={b.busco.includes(g.valor)}
              onPress={() =>
                cambiar({
                  busco: b.busco.includes(g.valor)
                    ? b.busco.filter((v) => v !== g.valor)
                    : [...b.busco, g.valor],
                })
              }
            />
          ))}
        </View>
      </PantallaPaso>
    ),

    5: (
      <PantallaPaso
        titulo="Edad y distancia"
        ayuda="Si llevas días esperando, ampliamos esto poco a poco y te avisamos."
        listo
        onSiguiente={siguiente}>
        <View style={estilos.filaValor}>
          <Etiqueta>Rango de edad</Etiqueta>
          <Text style={estilos.valorGrande}>
            {b.edadMin} – {b.edadMax}
          </Text>
        </View>
        <Slider
          minimumValue={18}
          maximumValue={70}
          step={1}
          value={b.edadMin}
          onValueChange={(v) => cambiar({ edadMin: Math.min(v, b.edadMax) })}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.lineFuerte}
          thumbTintColor={colors.accent}
        />
        <Slider
          minimumValue={18}
          maximumValue={70}
          step={1}
          value={b.edadMax}
          onValueChange={(v) => cambiar({ edadMax: Math.max(v, b.edadMin) })}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.lineFuerte}
          thumbTintColor={colors.accent}
        />

        <View style={[estilos.filaValor, { marginTop: 24 }]}>
          <Etiqueta>Distancia máxima</Etiqueta>
          <Text style={estilos.valorGrande}>{b.distancia} km</Text>
        </View>
        <Slider
          minimumValue={5}
          maximumValue={120}
          step={5}
          value={b.distancia}
          onValueChange={(v) => cambiar({ distancia: v })}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.lineFuerte}
          thumbTintColor={colors.accent}
        />

        <View style={{ marginTop: 18 }}>
          <Tarjeta>
            <Text style={estilos.notaTarjeta}>
              Tu zona nunca se muestra exacta: la otra persona lee «a 3 km de ti».
            </Text>
          </Tarjeta>
        </View>
      </PantallaPaso>
    ),

    6: (
      <PantallaPaso
        titulo="¿Dónde estás?"
        ayuda="Se pide una vez y se guarda redondeada a más de un kilómetro. No se sigue tu recorrido."
        listo={b.ubicacion !== null}
        onSiguiente={siguiente}>
        {b.ubicacion ? (
          <Tarjeta>
            <Etiqueta tono="accent">Ubicación tomada</Etiqueta>
            <Text style={estilos.notaTarjeta}>
              Guardada con poca precisión a propósito: sirve para la distancia, no para encontrarte.
            </Text>
          </Tarjeta>
        ) : (
          <Boton texto="Usar mi ubicación" onPress={pedirUbicacion} ocupado={ocupado} tono="oscuro" />
        )}
      </PantallaPaso>
    ),

    7: (
      <PantallaPaso
        titulo="¿En qué idiomas hablas?"
        ayuda="Aquí todo pasa por escribir, así que hace falta un idioma en común."
        listo={b.idiomas.length > 0}
        onSiguiente={siguiente}
        desplazable>
        <View style={estilos.rejilla}>
          {IDIOMAS.map((idioma) => (
            <Pastilla
              key={idioma.code}
              texto={idioma.etiqueta}
              elegida={b.idiomas.includes(idioma.code)}
              deshabilitada={b.idiomas.length >= 5}
              onPress={() =>
                cambiar({
                  idiomas: b.idiomas.includes(idioma.code)
                    ? b.idiomas.filter((c) => c !== idioma.code)
                    : b.idiomas.length >= 5
                      ? b.idiomas
                      : [...b.idiomas, idioma.code],
                })
              }
            />
          ))}
        </View>
      </PantallaPaso>
    ),

    8: (
      <PantallaPaso
        titulo="Cómo te relacionas"
        ayuda="Sin respuestas buenas ni malas: sirve para no juntar ritmos incompatibles."
        listo
        onSiguiente={siguiente}>
        <View style={{ gap: 22 }}>
          {RASGOS.map((rasgo) => (
            <View key={rasgo.clave} style={{ gap: 9 }}>
              <Etiqueta>{rasgo.titulo}</Etiqueta>
              <DosOpciones
                opciones={rasgo.opciones}
                elegida={b.rasgos[rasgo.clave]}
                onElegir={(v) =>
                  cambiar({ rasgos: { ...b.rasgos, [rasgo.clave]: v } as Rasgos })
                }
              />
            </View>
          ))}
        </View>
      </PantallaPaso>
    ),

    9: (
      <PantallaPaso
        titulo="¿Qué conversación te gusta?"
        ayuda="De aquí sale la pregunta con la que arranca tu primer chat."
        listo
        onSiguiente={siguiente}>
        <View style={{ gap: 10 }}>
          {CONVERSACIONES.map((c) => (
            <FilaOpcion
              key={c.valor}
              etiqueta={c.valor}
              detalle={c.pista}
              elegida={b.conversacion === c.valor}
              onPress={() => cambiar({ conversacion: c.valor })}
            />
          ))}
        </View>
      </PantallaPaso>
    ),

    10: (
      <PantallaPaso
        titulo="¿Qué buscas ahora?"
        ayuda="Elige lo que más pese hoy. Se puede cambiar cuando quieras."
        listo={b.intencion !== null}
        onSiguiente={siguiente}>
        <View style={{ gap: 10 }}>
          {INTENCIONES.map((i) => (
            <FilaOpcion
              key={i.valor}
              etiqueta={i.etiqueta}
              detalle={i.pista}
              elegida={b.intencion === i.valor}
              onPress={() => cambiar({ intencion: i.valor })}
            />
          ))}
        </View>
      </PantallaPaso>
    ),
  };

  // La última es la de intereses, que además remata el registro.
  const ultima = (
    <PantallaPaso
      titulo="Entre 5 y 8 intereses"
      ayuda={
        b.intereses.length < INTERESES_MIN
          ? `Llevas ${b.intereses.length}. De aquí sale la primera pregunta de cada conversación.`
          : `${b.intereses.length} elegidos. Cuanto más raro, más dice de ti.`
      }
      listo={b.intereses.length >= INTERESES_MIN}
      ocupado={ocupado}
      botonTexto="Terminar registro"
      onSiguiente={terminar}
      desplazable>
      <View style={estilos.rejilla}>
        {catalogo.map((interes) => (
          <Pastilla
            key={interes.name}
            texto={interes.label}
            elegida={b.intereses.includes(interes.name)}
            deshabilitada={b.intereses.length >= INTERESES_MAX}
            onPress={() =>
              cambiar({
                intereses: b.intereses.includes(interes.name)
                  ? b.intereses.filter((n) => n !== interes.name)
                  : b.intereses.length >= INTERESES_MAX
                    ? b.intereses
                    : [...b.intereses, interes.name],
              })
            }
          />
        ))}
      </View>
    </PantallaPaso>
  );

  return (
    <View style={estilos.pantalla}>
      <CabeceraPaso paso={paso} total={TOTAL} onAtras={atras} />
      {pantallas[paso] ?? ultima}
      {error && <Text style={estilos.error}>{error}</Text>}
    </View>
  );
}

/** Del DDMMAAAA tecleado a la edad de hoy, o null si aún no está completa. */
function edadDe(fecha: string): number | null {
  if (fecha.length < 8) return null;
  const dia = Number(fecha.slice(0, 2));
  const mes = Number(fecha.slice(2, 4));
  const anio = Number(fecha.slice(4));
  if (dia < 1 || dia > 31 || mes < 1 || mes > 12 || anio < 1920) return -1;

  const nacimiento = new Date(anio, mes - 1, dia);
  if (nacimiento.getDate() !== dia || nacimiento.getMonth() !== mes - 1) return -1;

  const hoy = new Date();
  let edad = hoy.getFullYear() - anio;
  const cumpleEsteAno = new Date(hoy.getFullYear(), mes - 1, dia);
  if (hoy < cumpleEsteAno) edad--;
  return edad;
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },

  fecha: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  casilla: {
    flex: 1,
    height: 64,
    borderRadius: 13,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  casillaActiva: { borderWidth: 1.5, borderColor: colors.accent },
  casillaTexto: { fontFamily: fonts.sansNegrita, fontSize: 20, color: colors.ink },
  separadorFecha: { fontFamily: fonts.sans, fontSize: 18, color: colors.ink5 },
  notaEdad: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.ink3, marginTop: 12 },

  teclado: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  tecla: {
    width: '31.5%',
    height: 58,
    borderRadius: radios.campo,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  teclaTexto: { fontFamily: fonts.sansMedia, fontSize: 24, color: colors.ink },

  filaValor: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  valorGrande: { fontFamily: fonts.serif, fontSize: 24, color: colors.ink },
  notaTarjeta: { fontFamily: fonts.sans, fontSize: 13.5, lineHeight: 20, color: colors.ink2 },

  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 9 },
  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.error,
    textAlign: 'center',
    paddingHorizontal: 26,
    paddingBottom: 12,
  },
});
