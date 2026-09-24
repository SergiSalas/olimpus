import * as Location from 'expo-location';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Image,
  Keyboard,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {
  fetchInterests,
  fetchPrompts,
  ownPhotoSource,
  type Gender,
  type Intent,
  type Interest,
  type LanguageLevel,
  type PromptQuestion,
} from './api';
import {
  Boton,
  Campo,
  Entrada,
  Etiqueta,
  FilaOpcion,
  Flotar,
  Opciones,
  Pastilla,
  Rebote,
  Tarjeta,
} from './components';
import { Rango } from './components/Rango';
import { elegirFoto } from './foto';
import { conEmoji } from './interests';
import {
  APODO_MAX,
  APODO_MIN,
  EDAD_MAX,
  EDAD_MIN,
  GENERO_ETIQUETA_MAX,
  IDIOMAS_MAX,
  INTERESES_MAX,
  INTERESES_MIN,
  PREGUNTAS,
  RESPUESTA_MAX,
  SITIO_MAX,
  TRABAJO_MAX,
  rangoSugerido,
  type Borrador,
} from './borrador';
import { CONVERSACIONES, RASGOS, edadDe, type Rasgos } from './mapeo';
import { colors, fonts, radios, text } from './theme';

/**
 * Cada pregunta del registro, suelta.
 *
 * Están aquí y no dentro de una pantalla porque hay dos que las usan: el
 * registro de la primera vez, que las agrupa en seis bloques, y la pantalla de
 * editar, que las pone todas seguidas. Si vivieran en una de las dos, la otra
 * tendría que copiarlas.
 */
export type Props = {
  b: Borrador;
  cambiar: (cambio: Partial<Borrador>) => void;
  /** Se llama cuando esta pregunta queda contestada, para pasar a la siguiente. */
  siguiente?: () => void;
  /** Le toca a esta pregunta: si tiene un campo, se abre el teclado. */
  enfocar?: boolean;
};

const GENEROS: { valor: Gender; etiqueta: string }[] = [
  { valor: 'WOMAN', etiqueta: 'Mujer' },
  { valor: 'MAN', etiqueta: 'Hombre' },
  { valor: 'NON_BINARY', etiqueta: 'No binarie' },
  { valor: 'OTHER', etiqueta: 'Otro' },
];

/**
 * Términos para decirlo con más precisión. Son sugerencias que rellenan el
 * campo, no una lista cerrada: quien no se vea en ninguno lo escribe.
 */
const GENERO_DETALLE = [
  'Mujer',
  'Hombre',
  'Mujer trans',
  'Hombre trans',
  'No binarie',
  'Género fluido',
  'Agénero',
  'Bigénero',
  'Queer',
  'Intergénero',
  'Cuestionándomelo',
  'Prefiero no decirlo',
];

/**
 * Cada opción es un género, incluido el último: OTHER es "otro género", no un
 * comodín. "Todo el mundo" se marca aparte y enciende los cuatro, porque quien
 * lo elige quiere decir eso y no "solo gente de género no declarado".
 */
const BUSCO: { valor: Gender; etiqueta: string }[] = [
  { valor: 'WOMAN', etiqueta: 'Mujeres' },
  { valor: 'MAN', etiqueta: 'Hombres' },
  { valor: 'NON_BINARY', etiqueta: 'Personas no binarias' },
  { valor: 'OTHER', etiqueta: 'Otros géneros' },
];

const TODOS: Gender[] = ['WOMAN', 'MAN', 'NON_BINARY', 'OTHER'];

const INTENCIONES: { valor: Intent; etiqueta: string; pista: string }[] = [
  { valor: 'FRIENDSHIP', etiqueta: 'Amistad', pista: 'Gente con quien hablar' },
  { valor: 'DATING', etiqueta: 'Citas', pista: 'Conocer sin prisa' },
  { valor: 'RELATIONSHIP', etiqueta: 'Pareja', pista: 'Algo que dure' },
  { valor: 'CASUAL', etiqueta: 'Algo casual', pista: 'Sin planes a futuro' },
];

/**
 * Con la bandera del país donde más se habla. Catalán, gallego y euskera no
 * tienen bandera en los emojis estándar, así que llevan un bocadillo.
 */
export const IDIOMAS = [
  { code: 'es', bandera: '🇪🇸', etiqueta: 'Español' },
  { code: 'en', bandera: '🇬🇧', etiqueta: 'Inglés' },
  { code: 'ca', bandera: '💬', etiqueta: 'Catalán' },
  { code: 'gl', bandera: '💬', etiqueta: 'Gallego' },
  { code: 'eu', bandera: '💬', etiqueta: 'Euskera' },
  { code: 'fr', bandera: '🇫🇷', etiqueta: 'Francés' },
  { code: 'pt', bandera: '🇵🇹', etiqueta: 'Portugués' },
  { code: 'it', bandera: '🇮🇹', etiqueta: 'Italiano' },
  { code: 'de', bandera: '🇩🇪', etiqueta: 'Alemán' },
  { code: 'ar', bandera: '🇸🇦', etiqueta: 'Árabe' },
  { code: 'ro', bandera: '🇷🇴', etiqueta: 'Rumano' },
  { code: 'nl', bandera: '🇳🇱', etiqueta: 'Neerlandés' },
  { code: 'pl', bandera: '🇵🇱', etiqueta: 'Polaco' },
  { code: 'ru', bandera: '🇷🇺', etiqueta: 'Ruso' },
  { code: 'uk', bandera: '🇺🇦', etiqueta: 'Ucraniano' },
  { code: 'zh', bandera: '🇨🇳', etiqueta: 'Chino' },
  { code: 'ja', bandera: '🇯🇵', etiqueta: 'Japonés' },
  { code: 'ko', bandera: '🇰🇷', etiqueta: 'Coreano' },
  { code: 'hi', bandera: '🇮🇳', etiqueta: 'Hindi' },
  { code: 'ur', bandera: '🇵🇰', etiqueta: 'Urdu' },
  { code: 'bn', bandera: '🇧🇩', etiqueta: 'Bengalí' },
  { code: 'tr', bandera: '🇹🇷', etiqueta: 'Turco' },
  { code: 'el', bandera: '🇬🇷', etiqueta: 'Griego' },
  { code: 'sv', bandera: '🇸🇪', etiqueta: 'Sueco' },
  { code: 'no', bandera: '🇳🇴', etiqueta: 'Noruego' },
  { code: 'da', bandera: '🇩🇰', etiqueta: 'Danés' },
  { code: 'fi', bandera: '🇫🇮', etiqueta: 'Finés' },
  { code: 'cs', bandera: '🇨🇿', etiqueta: 'Checo' },
  { code: 'hu', bandera: '🇭🇺', etiqueta: 'Húngaro' },
  { code: 'he', bandera: '🇮🇱', etiqueta: 'Hebreo' },
];

/**
 * El nivel se pregunta en palabras; el algoritmo lo usa como 0,4 / 0,7 / 1.
 * Importa de verdad cuando es bajo: por debajo de "me defiendo" no se emparejan
 * dos personas que solo comparten ese idioma.
 */
const NIVELES: { valor: LanguageLevel; etiqueta: string }[] = [
  { valor: 'BASIC', etiqueta: 'Lo chapurreo' },
  { valor: 'INTERMEDIATE', etiqueta: 'Me defiendo' },
  { valor: 'NATIVE', etiqueta: 'Como en casa' },
];

const etiquetaDeNivel = (nivel: LanguageLevel) =>
  NIVELES.find((n) => n.valor === nivel)?.etiqueta ?? '';

const nivelDeEtiqueta = (etiqueta: string): LanguageLevel =>
  NIVELES.find((n) => n.etiqueta === etiqueta)?.valor ?? 'INTERMEDIATE';

/** "🇫🇷 Francés": el nombre del idioma con su bandera. */
export const nombreDeIdioma = (code: string) => {
  const idioma = IDIOMAS.find((i) => i.code === code);
  return idioma ? `${idioma.bandera} ${idioma.etiqueta}` : code;
};

export const nombreDeNivel = (nivel: string) =>
  NIVELES.find((n) => n.valor === nivel)?.etiqueta.toLowerCase() ?? nivel;

/**
 * El catálogo de preguntas, que llega del servidor ya escrito en el idioma de
 * quien mira. Lo usan la pantalla que las elige y la que las repasa.
 */
export function useCatalogoPreguntas(): PromptQuestion[] {
  const [catalogo, setCatalogo] = useState<PromptQuestion[]>([]);

  useEffect(() => {
    fetchPrompts()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

  return catalogo;
}

/** Encabezado de cada pregunta cuando van varias en la misma pantalla. */
export function Pregunta({
  titulo,
  ayuda,
  children,
}: {
  titulo: string;
  ayuda?: string;
  children: React.ReactNode;
}) {
  return (
    <View style={estilos.pregunta}>
      <Text style={estilos.preguntaTitulo}>{titulo}</Text>
      {ayuda && <Text style={estilos.preguntaAyuda}>{ayuda}</Text>}
      {children}
    </View>
  );
}

export function Apodo({ b, cambiar, siguiente }: Props) {
  return (
    <Pregunta
      titulo="¿Cómo quieres que te llamen?"
      ayuda="Aparece en el nivel 1, en cuanto escribáis los dos. Tu nombre real no hace falta.">
      <Campo
        valor={b.apodo}
        onChange={(v) => cambiar({ apodo: v })}
        placeholder="Tu apodo"
        maxLength={APODO_MAX}
        teclaIntro="next"
        onSubmit={b.apodo.trim().length >= APODO_MIN ? siguiente : undefined}
      />
    </Pregunta>
  );
}

/**
 * La fecha se escribe con el teclado numérico del móvil. El campo de verdad es
 * invisible; las casillas solo enseñan lo escrito, y tocarlas le pasa el foco.
 *
 * Es la fila la que recoge el toque, no el campo: en iPhone una vista con
 * opacidad 0 no recibe toques, y el teclado no se abría nunca.
 */
export function Nacimiento({ b, cambiar, siguiente, enfocar }: Props) {
  const edad = edadDe(b.fecha);
  const campo = useRef<TextInput>(null);
  const [escribiendo, setEscribiendo] = useState(false);

  useEffect(() => {
    if (enfocar) campo.current?.focus();
  }, [enfocar]);

  function teclear(texto: string) {
    const fecha = texto.replace(/\D/g, '').slice(0, 8);
    cambiar({ fecha });
    // Con los ocho números y la edad bien, no hay nada más que escribir aquí.
    const nueva = edadDe(fecha);
    if (fecha.length === 8 && nueva !== null && nueva >= EDAD_MIN) {
      Keyboard.dismiss();
      siguiente?.();
    }
  }

  return (
    <Pregunta
      titulo="Tu fecha de nacimiento"
      ayuda="Solo para mayores de 18. Antes de la beta verificaremos la edad de verdad, no con una casilla.">
      <Pressable style={estilos.fecha} onPress={() => campo.current?.focus()}>
        <TextInput
          pointerEvents="none"
          ref={campo}
          value={b.fecha}
          onChangeText={teclear}
          keyboardType="number-pad"
          maxLength={8}
          caretHidden
          onFocus={() => setEscribiendo(true)}
          onBlur={() => setEscribiendo(false)}
          style={estilos.campoInvisible}
        />
        {['D', 'D', '/', 'M', 'M', '/', 'A', 'A', 'A', 'A'].map((ph, posicion) => {
          if (ph === '/') {
            return (
              <Text key={`sep${posicion}`} style={estilos.separadorFecha} pointerEvents="none">
                /
              </Text>
            );
          }
          const indice = posicion - (posicion > 5 ? 2 : posicion > 2 ? 1 : 0);
          const puesto = b.fecha[indice];
          return (
            <View
              key={posicion}
              pointerEvents="none"
              style={[
                estilos.casilla,
                escribiendo && b.fecha.length === indice && estilos.casillaActiva,
              ]}>
              <Text style={[estilos.casillaTexto, !puesto && { color: colors.trazo }]}>
                {puesto ?? ph}
              </Text>
            </View>
          );
        })}
      </Pressable>

      <Text style={[estilos.notaEdad, edad !== null && edad < 18 && { color: colors.error }]}>
        {edad === null
          ? 'Toca las casillas y escribe día, mes y año'
          : edad < 0
            ? 'Esa fecha no existe, revísala'
            : edad < 18
              ? 'Olimpus es solo para mayores de 18'
              : `Tienes ${edad} años`}
      </Text>
    </Pregunta>
  );
}

export function Genero({ b, cambiar }: Props) {
  return (
    <>
      <Pregunta
        titulo="Tu género"
        ayuda="De estos cuatro sale el emparejamiento, y por eso son cuatro. En el nivel 0 solo se ve tu edad, dos intereses y tu zona.">
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
      </Pregunta>

      <Pregunta
        titulo="¿Y cómo lo dices tú?"
        ayuda="Opcional. Esto se lee en el nivel 3 y no se usa para emparejar, así que puedes ser todo lo preciso que quieras.">
        <View style={estilos.rejilla}>
          {GENERO_DETALLE.map((termino) => (
            <Pastilla
              key={termino}
              texto={termino}
              elegida={b.generoEtiqueta === termino}
              onPress={() =>
                cambiar({ generoEtiqueta: b.generoEtiqueta === termino ? '' : termino })
              }
            />
          ))}
        </View>
        <View style={{ marginTop: 12 }}>
          <Campo
            valor={b.generoEtiqueta}
            onChange={(v) => cambiar({ generoEtiqueta: v })}
            placeholder="O escríbelo como quieras"
            maxLength={GENERO_ETIQUETA_MAX}
          />
        </View>
      </Pregunta>
    </>
  );
}

export function Busco({ b, cambiar }: Props) {
  const todos = b.busco.length === TODOS.length;

  return (
    <Pregunta
      titulo="¿Con quién quieres hablar?"
      ayuda="Puedes marcar varias. Solo emparejamos si encaja por los dos lados.">
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

        <FilaOpcion
          etiqueta="Todo el mundo"
          detalle="Marca las cuatro de arriba"
          varias
          elegida={todos}
          onPress={() => cambiar({ busco: todos ? [] : TODOS })}
        />
      </View>
    </Pregunta>
  );
}

export function EdadYDistancia({ b, cambiar }: Props) {
  const edad = edadDe(b.fecha);

  // Un 27-38 fijo es un número que nadie ha elegido y que acaba pesando en el
  // filtro igual que si lo hubiera elegido. Mientras nadie toque el mando, se
  // propone a partir de la edad de quien se registra.
  useEffect(() => {
    if (!b.rangoTocado && edad !== null && edad >= EDAD_MIN) {
      cambiar(rangoSugerido(edad));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [edad, b.rangoTocado]);

  return (
    <Pregunta
      titulo="Edad y distancia"
      ayuda="Si llevas días esperando, ampliamos esto poco a poco y te avisamos.">
      <View style={estilos.bloqueRango}>
        <Text style={estilos.rangoEtiqueta}>🎂 Edad de las personas</Text>
        <Text style={estilos.rangoValor}>
          De {b.edadMin} a {b.edadMax} años
        </Text>
        <Rango
          min={EDAD_MIN}
          max={EDAD_MAX}
          paso={1}
          valores={[b.edadMin, b.edadMax]}
          onChange={([edadMin, edadMax]) => cambiar({ edadMin, edadMax, rangoTocado: true })}
        />
        <View style={estilos.extremos}>
          <Text style={estilos.extremo}>{EDAD_MIN}</Text>
          <Text style={estilos.extremo}>{EDAD_MAX}</Text>
        </View>
      </View>

      <View style={[estilos.bloqueRango, { marginTop: 12 }]}>
        <Text style={estilos.rangoEtiqueta}>📍 Distancia</Text>
        <Text style={estilos.rangoValor}>Hasta {b.distancia} km</Text>
        <Text style={estilos.rangoPista}>{queAbarca(b.distancia)}</Text>
        <Rango
          min={DISTANCIA_MIN}
          max={DISTANCIA_MAX}
          paso={5}
          valores={[b.distancia]}
          onChange={([distancia]) => cambiar({ distancia })}
          color={colors.uva}
        />
        <View style={estilos.extremos}>
          <Text style={estilos.extremo}>{DISTANCIA_MIN} km</Text>
          <Text style={estilos.extremo}>{DISTANCIA_MAX} km</Text>
        </View>
      </View>

      <View style={{ marginTop: 18 }}>
        <Tarjeta>
          <Text style={estilos.notaTarjeta}>
            Tu zona nunca se muestra exacta: la otra persona lee «a 3 km de ti».
          </Text>
        </Tarjeta>
      </View>
    </Pregunta>
  );
}

const DISTANCIA_MIN = 5;
const DISTANCIA_MAX = 120;

/** Qué significan esos kilómetros en la vida real: un número solo no dice mucho. */
function queAbarca(km: number): string {
  if (km <= 10) return '🏘️ Tu barrio y alrededores';
  if (km <= 30) return '🏙️ Tu ciudad';
  if (km <= 60) return '🚗 Tu zona, a un rato en coche';
  return '🗺️ Toda tu provincia y más';
}

/** Una lectura de hace menos de esto vale igual que una nueva. */
const POSICION_RECIENTE_MS = 5 * 60 * 1000;

/**
 * La posición, por el camino más corto que sirva.
 *
 * Lo que se guarda se redondea a dos decimales, más de un kilómetro, así que
 * una lectura de hace cinco minutos es indistinguible de una recién hecha: se
 * pide primero la que ya hay. Solo si no hay ninguna reciente se espera a una
 * nueva, que bajo techo o con el GPS recién encendido puede no llegar nunca; en
 * ese caso vale la última conocida aunque sea vieja, porque dejar a alguien
 * plantado en el registro es peor que emparejarlo desde el barrio de al lado.
 * Siempre se puede volver a tomar desde «cambiar mis respuestas».
 */
async function leerPosicion() {
  const reciente = await Location.getLastKnownPositionAsync({ maxAge: POSICION_RECIENTE_MS });
  if (reciente) return reciente;

  try {
    return await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Low });
  } catch {
    return Location.getLastKnownPositionAsync();
  }
}

export function Ubicacion({ b, cambiar, siguiente }: Props) {
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function pedir() {
    setOcupado(true);
    setError(null);
    try {
      const permiso = await Location.requestForegroundPermissionsAsync();
      if (permiso.status !== 'granted') {
        setError('Sin ubicación no podemos calcular la distancia. Puedes darla en Ajustes.');
        return;
      }
      const posicion = await leerPosicion();
      if (!posicion) {
        setError('No se pudo leer la ubicación. Comprueba que el GPS está encendido.');
        return;
      }
      cambiar({
        ubicacion: { latitude: posicion.coords.latitude, longitude: posicion.coords.longitude },
      });
      siguiente?.();
    } catch {
      setError('No se pudo leer la ubicación.');
    } finally {
      setOcupado(false);
    }
  }

  return (
    <Pregunta
      titulo="¿Dónde estás?"
      ayuda="Se pide una vez y se guarda redondeada a más de un kilómetro. No se sigue tu recorrido.">
      {b.ubicacion ? (
        <Tarjeta>
          <Etiqueta tono="accent">Ubicación tomada</Etiqueta>
          <Text style={estilos.notaTarjeta}>
            Guardada con poca precisión a propósito: sirve para la distancia, no para encontrarte.
          </Text>
          <Pressable onPress={pedir} disabled={ocupado}>
            <Text style={estilos.enlace}>{ocupado ? 'Tomando…' : 'Volver a tomarla'}</Text>
          </Pressable>
        </Tarjeta>
      ) : (
        <Boton texto="Usar mi ubicación" onPress={pedir} ocupado={ocupado} tono="oscuro" />
      )}
      {error && <Text style={estilos.error}>{error}</Text>}
    </Pregunta>
  );
}

export function Idiomas({ b, cambiar }: Props) {
  return (
    <Pregunta
      titulo="¿En qué idiomas hablas?"
      ayuda="Aquí todo pasa por escribir, así que hace falta un idioma en común. Di también cómo lo hablas: si lo marcas flojo, no te pondremos con alguien con quien solo compartes ese idioma.">
      <View style={estilos.rejilla}>
        {IDIOMAS.map((idioma) => {
          const elegido = b.idiomas.some((i) => i.code === idioma.code);
          return (
            <Pastilla
              key={idioma.code}
              texto={`${idioma.bandera} ${idioma.etiqueta}`}
              elegida={elegido}
              deshabilitada={b.idiomas.length >= IDIOMAS_MAX}
              onPress={() =>
                cambiar({
                  idiomas: elegido
                    ? b.idiomas.filter((i) => i.code !== idioma.code)
                    : b.idiomas.length >= IDIOMAS_MAX
                      ? b.idiomas
                      : // Entra por "me defiendo" a propósito: si el valor de
                        // salida fuera "como en casa", nadie lo bajaría y
                        // volveríamos a tener el nivel de todo el mundo igual.
                        [
                          ...b.idiomas,
                          { code: idioma.code, level: 'INTERMEDIATE' as LanguageLevel },
                        ],
                })
              }
            />
          );
        })}
      </View>

      <View style={{ gap: 18, marginTop: 24 }}>
        {b.idiomas.map((hablado) => (
          <View key={hablado.code} style={{ gap: 9 }}>
            <Etiqueta>{nombreDeIdioma(hablado.code)}</Etiqueta>
            <Opciones
              opciones={NIVELES.map((n) => n.etiqueta)}
              elegida={etiquetaDeNivel(hablado.level)}
              onElegir={(etiqueta) =>
                cambiar({
                  idiomas: b.idiomas.map((i) =>
                    i.code === hablado.code ? { ...i, level: nivelDeEtiqueta(etiqueta) } : i,
                  ),
                })
              }
            />
          </View>
        ))}
      </View>
    </Pregunta>
  );
}

export function ComoHablas({ b, cambiar, siguiente }: Props) {
  return (
    <>
      <Pregunta
        titulo="Cómo te relacionas"
        ayuda="Sin respuestas buenas ni malas: sirve para no juntar ritmos incompatibles.">
        <View style={{ gap: 22 }}>
          {RASGOS.map((rasgo) => (
            <View key={rasgo.clave} style={{ gap: 9 }}>
              <Etiqueta>{rasgo.titulo}</Etiqueta>
              <Opciones
                opciones={rasgo.opciones}
                elegida={b.rasgos[rasgo.clave]}
                onElegir={(v) => cambiar({ rasgos: { ...b.rasgos, [rasgo.clave]: v } as Rasgos })}
              />
            </View>
          ))}
        </View>
      </Pregunta>

      <Pregunta
        titulo="¿Qué conversación te gusta?"
        ayuda="De aquí sale la pregunta con la que arranca tu primer chat.">
        <View style={{ gap: 10 }}>
          {CONVERSACIONES.map((c) => (
            <FilaOpcion
              key={c.valor}
              etiqueta={c.valor}
              detalle={c.pista}
              elegida={b.conversacion === c.valor}
              onPress={() => {
                cambiar({ conversacion: c.valor });
                siguiente?.();
              }}
            />
          ))}
        </View>
      </Pregunta>
    </>
  );
}

export function Intencion({ b, cambiar }: Props) {
  return (
    <Pregunta
      titulo="¿Qué buscas ahora?"
      ayuda="Elige lo que más pese hoy. Se puede cambiar cuando quieras.">
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
    </Pregunta>
  );
}

export function Intereses({ b, cambiar }: Props) {
  const [catalogo, setCatalogo] = useState<Interest[]>([]);
  const [busca, setBusca] = useState('');

  useEffect(() => {
    fetchInterests()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

  /**
   * Por orden alfabético, no por popularidad. El catálogo llega con los más
   * comunes delante, y el algoritmo pesa cada interés por lo raro que es: dejar
   * "viajar" y "música" arriba empuja justo hacia lo que luego no dice nada.
   */
  const visibles = useMemo(() => {
    const limpio = busca.trim().toLowerCase();
    return catalogo
      .filter((i) => i.label.toLowerCase().includes(limpio))
      .sort((uno, otro) => uno.label.localeCompare(otro.label, 'es'));
  }, [catalogo, busca]);

  return (
    <Pregunta
      titulo={`Entre ${INTERESES_MIN} y ${INTERESES_MAX} intereses`}
      ayuda={
        b.intereses.length < INTERESES_MIN
          ? `Llevas ${b.intereses.length}. De aquí sale la primera pregunta de cada conversación.`
          : `${b.intereses.length} elegidos. Cuanto más raro, más dice de ti.`
      }>
      <Campo valor={busca} onChange={setBusca} placeholder="Buscar" />

      {b.intereses.length > 0 && (
        <View style={{ marginTop: 14 }}>
          <Etiqueta>Los tuyos</Etiqueta>
          <View style={[estilos.rejilla, { marginTop: 8 }]}>
            {b.intereses.map((elegido) => (
              <Pastilla
                key={elegido}
                texto={conEmoji(
                  elegido,
                  catalogo.find((i) => i.name === elegido)?.label ?? elegido,
                )}
                elegida
                onPress={() => cambiar({ intereses: b.intereses.filter((n) => n !== elegido) })}
              />
            ))}
          </View>
        </View>
      )}

      <View style={[estilos.rejilla, { marginTop: 16 }]}>
        {visibles.map((interes) => (
          <Pastilla
            key={interes.name}
            texto={conEmoji(interes.name, interes.label)}
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

      {visibles.length === 0 && catalogo.length > 0 && (
        <Text style={estilos.notaTarjeta}>Nada con ese nombre.</Text>
      )}
    </Pregunta>
  );
}

/** Un color por hueco: la tarjeta, su número y su punto de progreso. */
export const COLOR_HUECO = [colors.accent, colors.sol, colors.menta];
export const FONDO_HUECO = [colors.accentWash, '#FFF6D9', '#E3FAF1'];
const ORDINAL = ['primera', 'segunda', 'tercera'];

/** A partir de aquí el contador aparece: antes solo agobia. */
const AVISO_LARGO = 150;

/** Un emoji y un ejemplo por pregunta: una caja vacía se contesta peor que una con pista. */
const PISTAS: Record<string, { emoji: string; ejemplo: string }> = {
  'last-hooked': { emoji: '🎬', ejemplo: 'Ej.: un documental sobre pulpos, me lo vi dos veces' },
  'always-ask': { emoji: '🙋', ejemplo: 'Ej.: ¿cuál es tu desayuno ideal?' },
  'weird-habit': { emoji: '🙃', ejemplo: 'Ej.: le pongo nombre a todas mis plantas' },
  'makes-me-laugh': { emoji: '😂', ejemplo: 'Ej.: los vídeos de gatos que se caen' },
  'perfect-tuesday': { emoji: '☕', ejemplo: 'Ej.: café, paseo por la playa y una serie' },
  'learned-late': { emoji: '🐢', ejemplo: 'Ej.: a nadar, con 25 años' },
  'hill-to-die-on': { emoji: '🏔️', ejemplo: 'Ej.: la tortilla, con cebolla' },
  'surprisingly-good-at': { emoji: '🦸', ejemplo: 'Ej.: aparcar a la primera' },
  'always-return': { emoji: '📍', ejemplo: 'Ej.: un bar de mi barrio con las mejores bravas' },
  'changed-my-mind': { emoji: '🔄', ejemplo: 'Ej.: antes odiaba correr' },
  'too-much-internet': { emoji: '📱', ejemplo: 'Ej.: los vídeos de diez segundos' },
  'want-to-try': { emoji: '🎯', ejemplo: 'Ej.: hacer surf de una vez' },
  'worst-recommendation': { emoji: '🙈', ejemplo: 'Ej.: una peli que nadie aguantó entera' },
  'gives-me-away': { emoji: '🕵️', ejemplo: 'Ej.: mi risa, se oye desde la otra punta' },
  'not-in-my-interests': { emoji: '🧩', ejemplo: 'Ej.: colecciono postales antiguas' },
};
export const pistaDe = (question: string) =>
  PISTAS[question] ?? { emoji: '💬', ejemplo: 'Termina la frase a tu manera' };

/**
 * Las tres preguntas que sustituyen a la bio.
 *
 * Una caja vacía pidiendo que te describas se rellena con un encogimiento de
 * hombros; una pregunta concreta se contesta. Y la respuesta le da al otro algo
 * a lo que responder, que es justo lo que hace falta en el nivel 2.
 *
 * Cada una es una tarjeta donde la pregunta manda y la respuesta termina la
 * frase. Se eligen en una hoja aparte, para no desplegar quince filas en medio.
 */
export function Preguntas({ b, cambiar }: Props) {
  const catalogo = useCatalogoPreguntas();
  const [eligiendo, setEligiendo] = useState<number | null>(null);
  const [viendoMuestra, setViendoMuestra] = useState(false);
  /** Los campos de respuesta, para llevar el teclado a la pregunta recién elegida. */
  const respuestas = useRef<(TextInput | null)[]>([]);
  const [porEnfocar, setPorEnfocar] = useState<number | null>(null);

  // Se espera a que la hoja termine de bajar: con ella delante, el foco no llega.
  useEffect(() => {
    if (porEnfocar === null) return;
    const t = setTimeout(() => {
      respuestas.current[porEnfocar]?.focus();
      setPorEnfocar(null);
    }, 450);
    return () => clearTimeout(t);
  }, [porEnfocar]);

  function abrirHoja(hueco: number) {
    Keyboard.dismiss();
    setEligiendo(hueco);
  }

  const textoDe = (question: string) =>
    catalogo.find((q) => q.name === question)?.label ?? question;

  const contestadas = b.preguntas.filter((p) => p.answer.trim().length > 0).length;
  const completas = b.preguntas.length === PREGUNTAS && contestadas === PREGUNTAS;

  // Al elegir, las que ya usas no salen, salvo la del hueco que se está cambiando.
  const disponibles = catalogo.filter(
    (q) => !b.preguntas.some((p, i) => p.question === q.name && i !== eligiendo),
  );

  function elegir(question: string) {
    if (eligiendo === null) return;
    const siguientes = [...b.preguntas];
    if (eligiendo < siguientes.length) siguientes[eligiendo] = { question, answer: '' };
    else siguientes.push({ question, answer: '' });
    cambiar({ preguntas: siguientes });
    setPorEnfocar(eligiendo);
    setEligiendo(null);
  }

  function responder(hueco: number, answer: string) {
    cambiar({
      preguntas: b.preguntas.map((p, i) => (i === hueco ? { ...p, answer } : p)),
    });
  }

  return (
    <Pregunta
      titulo="Tus tres preguntas"
      ayuda="Elige tres y termina la frase. Es lo que la otra persona leerá en el nivel 2, y le da algo a lo que responder.">
      <View style={estilos.progreso}>
        {COLOR_HUECO.map((color, i) => (
          <View
            key={i}
            style={[estilos.puntoProgreso, i < contestadas && { backgroundColor: color }]}
          />
        ))}
        <Text style={estilos.progresoTexto}>
          {contestadas} de {PREGUNTAS}
        </Text>
      </View>

      <View style={{ gap: 14 }}>
        {b.preguntas.map((puesta, hueco) => {
          const pista = pistaDe(puesta.question);
          return (
            <Entrada key={hueco}>
              <View
                style={[
                  estilos.tarjetaPregunta,
                  { backgroundColor: FONDO_HUECO[hueco], borderColor: COLOR_HUECO[hueco] },
                ]}>
                <View style={estilos.filaTarjeta}>
                  <View style={[estilos.numero, { backgroundColor: COLOR_HUECO[hueco] }]}>
                    <Text style={[estilos.numeroTexto, hueco > 0 && { color: colors.ink }]}>
                      {hueco + 1}
                    </Text>
                  </View>
                  <Text style={estilos.emojiPregunta}>{pista.emoji}</Text>
                  <Pressable
                    style={{ marginLeft: 'auto' }}
                    hitSlop={10}
                    onPress={() => abrirHoja(hueco)}>
                    <Text style={estilos.enlace}>Cambiar ›</Text>
                  </Pressable>
                </View>
                <Text style={estilos.preguntaGrande}>{textoDe(puesta.question)}…</Text>
                <TextInput
                  ref={(campo) => {
                    respuestas.current[hueco] = campo;
                  }}
                  style={[estilos.respuestaCampo, { borderLeftColor: COLOR_HUECO[hueco] }]}
                  value={puesta.answer}
                  onChangeText={(v) => responder(hueco, v)}
                  placeholder={pista.ejemplo}
                  placeholderTextColor={colors.ink5}
                  maxLength={RESPUESTA_MAX}
                  multiline
                />
                {puesta.answer.length >= AVISO_LARGO && (
                  <Text style={estilos.contador}>
                    {puesta.answer.length} / {RESPUESTA_MAX}
                  </Text>
                )}
              </View>
            </Entrada>
          );
        })}

        {/*
          Solo existe el siguiente hueco libre: si se pudiera saltar al tercero,
          la respuesta acabaría guardada en el segundo y el número de la tarjeta
          dejaría de decir la verdad.
        */}
        {b.preguntas.length < PREGUNTAS && (
          <Rebote onPress={() => abrirHoja(b.preguntas.length)}>
            <Flotar distancia={3} giro={0} duracion={1800}>
              <View style={estilos.huecoLibre}>
                <Text style={estilos.huecoMas}>＋</Text>
                <Text style={estilos.huecoLibreTexto}>
                  Elige tu {ORDINAL[b.preguntas.length]} pregunta
                </Text>
              </View>
            </Flotar>
          </Rebote>
        )}
      </View>

      {completas && (
        <Rebote style={estilos.verMuestra} onPress={() => setViendoMuestra(true)}>
          <Text style={estilos.verMuestraTexto}>👀 Así lo verá la otra persona</Text>
        </Rebote>
      )}

      <Hoja visible={eligiendo !== null} onCerrar={() => setEligiendo(null)}>
        <Text style={estilos.hojaTitulo}>Elige una pregunta</Text>
        <Rebote
          style={estilos.sorpresa}
          onPress={() =>
            disponibles.length > 0 &&
            elegir(disponibles[Math.floor(Math.random() * disponibles.length)].name)
          }>
          <Text style={estilos.sorpresaTexto}>🎲 Sorpréndeme</Text>
        </Rebote>
        <View style={estilos.rejillaPreguntas}>
          {disponibles.map((q) => (
            <Rebote
              key={q.name}
              fuera={{ width: '48%' }}
              style={estilos.opcionPregunta}
              onPress={() => elegir(q.name)}>
              <Text style={estilos.opcionEmoji}>{pistaDe(q.name).emoji}</Text>
              <Text style={estilos.opcionTexto}>{q.label}…</Text>
            </Rebote>
          ))}
        </View>
      </Hoja>

      <Hoja visible={viendoMuestra} onCerrar={() => setViendoMuestra(false)}>
        <Text style={estilos.hojaTitulo}>En su pantalla, en el nivel 2</Text>
        <Tarjeta>
          <Etiqueta>Sus preguntas</Etiqueta>
          {b.preguntas.map((p) => (
            <View key={p.question} style={{ gap: 3 }}>
              <Text style={estilos.muestraPregunta}>{textoDe(p.question)}</Text>
              <Text style={estilos.muestraRespuesta}>{p.answer}</Text>
            </View>
          ))}
        </Tarjeta>
      </Hoja>
    </Pregunta>
  );
}

/** Una hoja que sube desde abajo, con su asa y el fondo velado. */
function Hoja({
  visible,
  onCerrar,
  children,
}: {
  visible: boolean;
  onCerrar: () => void;
  children: React.ReactNode;
}) {
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onCerrar}>
      <View style={estilos.veloHoja}>
        <Pressable style={{ flex: 1 }} onPress={onCerrar} />
        <View style={estilos.hoja}>
          <View style={estilos.asa} />
          <ScrollView contentContainerStyle={{ gap: 12, paddingBottom: 30 }}>{children}</ScrollView>
        </View>
      </View>
    </Modal>
  );
}

export function TrabajoYSitio({ b, cambiar }: Props) {
  return (
    <Pregunta titulo="Un poco más de ti">
      <View style={estilos.chipsInfo}>
        <View style={estilos.chipInfo}>
          <Text style={estilos.chipInfoTexto}>Opcional</Text>
        </View>
        <View style={estilos.chipInfo}>
          <Text style={estilos.chipInfoTexto}>🔒 Se ve a partir del nivel 3</Text>
        </View>
      </View>

      <View style={{ gap: 12 }}>
        <View style={estilos.campoTarjeta}>
          <Text style={estilos.campoTitulo}>💼 A qué te dedicas</Text>
          <TextInput
            style={estilos.campoLibre}
            value={b.trabajo}
            onChangeText={(v) => cambiar({ trabajo: v })}
            placeholder="Ej.: enfermera, estudio diseño…"
            placeholderTextColor={colors.ink5}
            maxLength={TRABAJO_MAX}
          />
        </View>
        <View style={estilos.campoTarjeta}>
          <Text style={estilos.campoTitulo}>🏡 De dónde eres</Text>
          <TextInput
            style={estilos.campoLibre}
            value={b.sitio}
            onChangeText={(v) => cambiar({ sitio: v })}
            placeholder="Ej.: Girona, aunque vivo en Barcelona"
            placeholderTextColor={colors.ink5}
            maxLength={SITIO_MAX}
          />
        </View>
      </View>
      <Text style={estilos.notaOpcional}>
        No se usan para emparejar: solo para que te conozcan.
      </Text>
    </Pregunta>
  );
}

export function Foto({
  b,
  cambiar,
  token,
  yaHayUna,
}: Props & { token: string; yaHayUna: boolean }) {
  const [error, setError] = useState<string | null>(null);

  async function escoger() {
    setError(null);
    try {
      const elegida = await elegirFoto();
      if (elegida) cambiar({ foto: elegida });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo abrir la galería.');
    }
  }

  return (
    <Pregunta
      titulo="Una foto tuya"
      ayuda={
        yaHayUna
          ? 'Ya tienes una guardada. Tócala solo si quieres cambiarla.'
          : 'Nadie la ve al emparejar. Aparece en el nivel 3, y solo si los dos habéis dicho que queréis veros.'
      }>
      <View style={{ alignItems: 'center', gap: 18, marginTop: 10 }}>
        <Pressable style={estilos.marco} onPress={escoger}>
          {b.foto ? (
            <Image source={{ uri: b.foto }} style={estilos.previa} />
          ) : yaHayUna ? (
            <Image source={ownPhotoSource(token)} style={estilos.previa} />
          ) : (
            <View style={estilos.marcoVacio}>
              <Text style={estilos.marcoTexto}>Tocar para elegir</Text>
            </View>
          )}
        </Pressable>

        {(b.foto || yaHayUna) && (
          <Pressable onPress={escoger}>
            <Text style={estilos.enlace}>Elegir otra</Text>
          </Pressable>
        )}

        <Tarjeta>
          <Etiqueta>Qué hacemos con ella</Etiqueta>
          <Text style={estilos.notaTarjeta}>
            Se reduce en tu móvil antes de enviarla, y al reescribirla se pierden los datos ocultos
            que llevan las fotos, incluido el lugar donde se hizo. En el servidor no hay ningún
            enlace público: cada vez que alguien la pide, se comprueba si tiene derecho a verla.
          </Text>
        </Tarjeta>

        {error && <Text style={estilos.error}>{error}</Text>}
      </View>
    </Pregunta>
  );
}

const estilos = StyleSheet.create({
  pregunta: { gap: 0, marginBottom: 34 },
  preguntaTitulo: { ...text.titulo, fontSize: 24 },
  preguntaAyuda: { ...text.ayuda, marginTop: 8, marginBottom: 14 },

  fecha: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  casilla: {
    flex: 1,
    height: 58,
    borderRadius: 13,
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  casillaActiva: { borderColor: colors.accent, backgroundColor: colors.accentWash },
  // Encima de las casillas y transparente: recoge el toque y abre el teclado.
  campoInvisible: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    opacity: 0,
    zIndex: 1,
  },
  casillaTexto: { fontFamily: fonts.sansNegrita, fontSize: 19, color: colors.ink },
  separadorFecha: { fontFamily: fonts.sans, fontSize: 18, color: colors.ink5 },
  notaEdad: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.ink3,
    marginTop: 12,
    marginBottom: 14,
  },

  filaValor: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  bloqueRango: {
    backgroundColor: colors.surface,
    borderRadius: 24,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 18,
    paddingBottom: 12,
  },
  rangoEtiqueta: { fontFamily: fonts.sansNegrita, fontSize: 13, color: colors.ink3 },
  rangoValor: { fontFamily: fonts.display, fontSize: 28, color: colors.ink, marginTop: 2 },
  rangoPista: { fontFamily: fonts.sansMedia, fontSize: 14, color: colors.uva, marginTop: 2 },
  extremos: { flexDirection: 'row', justifyContent: 'space-between', marginTop: -4 },
  extremo: { fontFamily: fonts.sansMedia, fontSize: 12, color: colors.ink4 },
  valorGrande: { fontFamily: fonts.display, fontSize: 24, color: colors.ink },
  notaTarjeta: { fontFamily: fonts.sans, fontSize: 13.5, lineHeight: 20, color: colors.ink2 },

  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 9 },

  contador: {
    fontFamily: fonts.sansMedia,
    fontSize: 12.5,
    color: colors.ink4,
    textAlign: 'right',
  },

  marco: { width: 190, aspectRatio: 3 / 4, borderRadius: 18, overflow: 'hidden' },
  previa: { width: '100%', height: '100%' },
  marcoVacio: {
    flex: 1,
    borderRadius: 18,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: colors.trazo,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  marcoTexto: { fontFamily: fonts.sansMedia, fontSize: 14, color: colors.ink4 },

  enlace: { fontFamily: fonts.sansMedia, fontSize: 14, color: colors.accent },
  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.error,
    marginTop: 10,
  },

  progreso: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 14 },
  puntoProgreso: { width: 22, height: 8, borderRadius: 999, backgroundColor: colors.lineFuerte },
  progresoTexto: {
    fontFamily: fonts.sansNegrita,
    fontSize: 12.5,
    color: colors.ink3,
    marginLeft: 4,
  },
  tarjetaPregunta: {
    borderRadius: 24,
    borderWidth: 2,
    borderBottomWidth: 5,
    padding: 16,
    gap: 10,
  },
  filaTarjeta: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  numero: {
    width: 28,
    height: 28,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
  },
  numeroTexto: { fontFamily: fonts.displayFuerte, fontSize: 15, color: '#FFFFFF' },
  emojiPregunta: { fontSize: 22 },
  preguntaGrande: { fontFamily: fonts.display, fontSize: 21, lineHeight: 26, color: colors.ink },
  respuestaCampo: {
    borderLeftWidth: 4,
    borderRadius: 4,
    paddingLeft: 12,
    paddingVertical: 6,
    minHeight: 48,
    fontFamily: fonts.sansMedia,
    fontSize: 16,
    lineHeight: 23,
    color: colors.ink,
    textAlignVertical: 'top',
  },
  huecoLibre: {
    minHeight: 76,
    borderRadius: 24,
    borderWidth: 2.5,
    borderStyle: 'dashed',
    borderColor: colors.uva,
    backgroundColor: colors.surface,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  huecoMas: { fontFamily: fonts.displayFuerte, fontSize: 24, color: colors.uva },
  huecoLibreTexto: { fontFamily: fonts.display, fontSize: 18, color: colors.uva },
  verMuestra: {
    marginTop: 14,
    alignSelf: 'center',
    borderRadius: 999,
    backgroundColor: colors.surface2,
    paddingHorizontal: 18,
    paddingVertical: 11,
  },
  verMuestraTexto: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.uva },

  veloHoja: { flex: 1, backgroundColor: colors.velo },
  hoja: {
    maxHeight: '85%',
    backgroundColor: colors.bg,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    paddingHorizontal: 20,
    paddingTop: 12,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.trazo,
    marginBottom: 14,
  },
  hojaTitulo: { fontFamily: fonts.display, fontSize: 24, color: colors.ink },
  sorpresa: {
    height: 50,
    borderRadius: 18,
    backgroundColor: colors.sol,
    borderBottomWidth: 4,
    borderColor: '#D9A21F',
    alignItems: 'center',
    justifyContent: 'center',
  },
  sorpresaTexto: { fontFamily: fonts.sansNegrita, fontSize: 15.5, color: colors.ink },
  rejillaPreguntas: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    rowGap: 10,
  },
  opcionPregunta: {
    minHeight: 112,
    borderRadius: 20,
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 13,
    gap: 6,
  },
  opcionEmoji: { fontSize: 24 },
  opcionTexto: { fontFamily: fonts.sansNegrita, fontSize: 14.5, lineHeight: 19, color: colors.ink },
  muestraPregunta: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.ink },
  muestraRespuesta: { fontFamily: fonts.sans, fontSize: 14.5, lineHeight: 21, color: colors.ink2 },

  chipsInfo: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 10, marginBottom: 14 },
  chipInfo: {
    borderRadius: 999,
    backgroundColor: colors.surface2,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  chipInfoTexto: { fontFamily: fonts.sansNegrita, fontSize: 12.5, color: colors.uva },
  campoTarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 6,
  },
  campoTitulo: { fontFamily: fonts.sansNegrita, fontSize: 13.5, color: colors.ink3 },
  campoLibre: {
    fontFamily: fonts.sansMedia,
    fontSize: 16.5,
    color: colors.ink,
    paddingVertical: 8,
  },
  notaOpcional: { fontFamily: fonts.sans, fontSize: 12.5, color: colors.ink4, marginTop: 10 },
});
