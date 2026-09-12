import * as Location from 'expo-location';
import { useEffect, useState, type ReactNode } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
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
import { Boton, Campo, Escala, Pastilla, Progreso } from '../components';
import { colors } from '../theme';

const GENEROS: { valor: Gender; texto: string }[] = [
  { valor: 'MUJER', texto: 'Mujer' },
  { valor: 'HOMBRE', texto: 'Hombre' },
  { valor: 'NO_BINARIO', texto: 'No binario' },
  { valor: 'OTRO', texto: 'Otro' },
];

const INTENCIONES: { valor: Intent; texto: string }[] = [
  { valor: 'AMISTAD', texto: 'Amistad' },
  { valor: 'CITAS', texto: 'Citas' },
  { valor: 'PAREJA', texto: 'Pareja' },
  { valor: 'CASUAL', texto: 'Algo casual' },
];

const IDIOMAS = [
  { code: 'es', texto: 'Español' },
  { code: 'en', texto: 'Inglés' },
  { code: 'ca', texto: 'Catalán' },
  { code: 'fr', texto: 'Francés' },
  { code: 'de', texto: 'Alemán' },
  { code: 'it', texto: 'Italiano' },
  { code: 'pt', texto: 'Portugués' },
  { code: 'ar', texto: 'Árabe' },
  { code: 'zh', texto: 'Chino' },
];

const NIVELES: { valor: LanguageLevel; texto: string }[] = [
  { valor: 'BASICO', texto: 'Básico' },
  { valor: 'MEDIO', texto: 'Medio' },
  { valor: 'NATIVO', texto: 'Nativo' },
];

const DISTANCIAS = [5, 15, 30, 50];
const TOTAL_PASOS = 13;
const INTERESES_MIN = 5;
const INTERESES_MAX = 8;

/** Lo que se va rellenando. Nada se manda al servidor hasta el último paso. */
type Borrador = {
  nickname: string;
  dia: string;
  mes: string;
  anio: string;
  gender: Gender | null;
  seeking: Gender[];
  ageMin: string;
  ageMax: string;
  maxDistanceKm: number | null;
  ubicacion: { latitude: number; longitude: number } | null;
  languages: { code: string; level: LanguageLevel }[];
  sociability: number;
  conversationDepth: number;
  intent: Intent | null;
  interests: string[];
  bio: string;
};

const VACIO: Borrador = {
  nickname: '',
  dia: '',
  mes: '',
  anio: '',
  gender: null,
  seeking: [],
  ageMin: '25',
  ageMax: '40',
  maxDistanceKm: null,
  ubicacion: null,
  languages: [],
  sociability: 3,
  conversationDepth: 3,
  intent: null,
  interests: [],
  bio: '',
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
    fetchInterests().then(setCatalogo).catch(() => setCatalogo([]));
  }, []);

  const cambiar = (cambio: Partial<Borrador>) => setB((actual) => ({ ...actual, ...cambio }));

  function alterna<T>(lista: T[], valor: T, tope?: number): T[] {
    if (lista.includes(valor)) return lista.filter((v) => v !== valor);
    if (tope && lista.length >= tope) return lista;
    return [...lista, valor];
  }

  async function pedirUbicacion() {
    setOcupado(true);
    setError(null);
    try {
      const permiso = await Location.requestForegroundPermissionsAsync();
      if (permiso.status !== 'granted') {
        setError('Sin ubicación no se puede calcular la distancia. Puedes darla en Ajustes.');
        return;
      }
      const posicion = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Low,
      });
      cambiar({
        ubicacion: {
          latitude: posicion.coords.latitude,
          longitude: posicion.coords.longitude,
        },
      });
      setPaso(paso + 1);
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
        nickname: b.nickname.trim(),
        bio: b.bio.trim(),
        birthDate: `${b.anio}-${b.mes.padStart(2, '0')}-${b.dia.padStart(2, '0')}`,
        gender: b.gender!,
        seeking: b.seeking,
        ageMin: Number(b.ageMin),
        ageMax: Number(b.ageMax),
        maxDistanceKm: b.maxDistanceKm!,
        latitude: b.ubicacion!.latitude,
        longitude: b.ubicacion!.longitude,
        languages: b.languages,
        sociability: b.sociability,
        conversationDepth: b.conversationDepth,
        intent: b.intent!,
        interests: b.interests,
      });
      onTerminado(perfil);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo guardar el registro.');
    } finally {
      setOcupado(false);
    }
  }

  const pasos: Record<
    number,
    { titulo: string; ayuda?: string; cuerpo: ReactNode; listo: boolean }
  > = {
      1: {
        titulo: '¿Cómo quieres que te llamen?',
        ayuda: 'Es lo primero que verá la otra persona, en cuanto los dos escribáis algo.',
        cuerpo: (
          <Campo
            valor={b.nickname}
            onChange={(v) => cambiar({ nickname: v })}
            placeholder="Tu apodo"
            maxLength={20}
          />
        ),
        listo: b.nickname.trim().length >= 2,
      },
      2: {
        titulo: '¿Cuándo naciste?',
        ayuda: 'Olimpus es solo para mayores de 18 años.',
        cuerpo: (
          <View style={{ flexDirection: 'row', gap: 10 }}>
            <Campo
              valor={b.dia}
              onChange={(v) => cambiar({ dia: v.replace(/\D/g, '').slice(0, 2) })}
              placeholder="DD"
              keyboardType="number-pad"
              ancho={70}
            />
            <Campo
              valor={b.mes}
              onChange={(v) => cambiar({ mes: v.replace(/\D/g, '').slice(0, 2) })}
              placeholder="MM"
              keyboardType="number-pad"
              ancho={70}
            />
            <Campo
              valor={b.anio}
              onChange={(v) => cambiar({ anio: v.replace(/\D/g, '').slice(0, 4) })}
              placeholder="AAAA"
              keyboardType="number-pad"
              ancho={100}
            />
          </View>
        ),
        listo: fechaValida(b),
      },
      3: {
        titulo: '¿Cuál es tu género?',
        cuerpo: (
          <View style={estilos.rejilla}>
            {GENEROS.map((g) => (
              <Pastilla
                key={g.valor}
                texto={g.texto}
                elegida={b.gender === g.valor}
                onPress={() => cambiar({ gender: g.valor })}
              />
            ))}
          </View>
        ),
        listo: b.gender !== null,
      },
      4: {
        titulo: '¿Con quién quieres hablar?',
        ayuda: 'Puedes elegir varios. Esto es un filtro que nunca cede: tiene que encajar por los dos lados.',
        cuerpo: (
          <View style={estilos.rejilla}>
            {GENEROS.map((g) => (
              <Pastilla
                key={g.valor}
                texto={g.texto}
                elegida={b.seeking.includes(g.valor)}
                onPress={() => cambiar({ seeking: alterna(b.seeking, g.valor) })}
              />
            ))}
          </View>
        ),
        listo: b.seeking.length > 0,
      },
      5: {
        titulo: '¿Qué edades te encajan?',
        ayuda: 'Si llevas tiempo esperando, el rango se amplía un poco solo, y se te avisa.',
        cuerpo: (
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
            <Campo
              valor={b.ageMin}
              onChange={(v) => cambiar({ ageMin: v.replace(/\D/g, '').slice(0, 2) })}
              keyboardType="number-pad"
              ancho={80}
            />
            <Text style={estilos.ayuda}>hasta</Text>
            <Campo
              valor={b.ageMax}
              onChange={(v) => cambiar({ ageMax: v.replace(/\D/g, '').slice(0, 2) })}
              keyboardType="number-pad"
              ancho={80}
            />
            <Text style={estilos.ayuda}>años</Text>
          </View>
        ),
        listo:
          Number(b.ageMin) >= 18 && Number(b.ageMax) <= 99 && Number(b.ageMin) <= Number(b.ageMax),
      },
      6: {
        titulo: '¿Hasta dónde estás dispuesto a moverte?',
        cuerpo: (
          <View style={estilos.rejilla}>
            {DISTANCIAS.map((km) => (
              <Pastilla
                key={km}
                texto={`${km} km`}
                elegida={b.maxDistanceKm === km}
                onPress={() => cambiar({ maxDistanceKm: km })}
              />
            ))}
          </View>
        ),
        listo: b.maxDistanceKm !== null,
      },
      7: {
        titulo: '¿Dónde estás?',
        ayuda:
          'Se pide una vez y se guarda redondeada a más de un kilómetro: sirve para la distancia, pero no señala tu casa. No se sigue tu recorrido.',
        cuerpo: b.ubicacion ? (
          <Text style={estilos.ok}>Ubicación tomada.</Text>
        ) : (
          <Boton texto="Usar mi ubicación" onPress={pedirUbicacion} ocupado={ocupado} />
        ),
        listo: b.ubicacion !== null,
      },
      8: {
        titulo: '¿Qué idiomas hablas?',
        ayuda: 'Toca un idioma y luego elige tu nivel.',
        cuerpo: (
          <View style={{ gap: 14 }}>
            <View style={estilos.rejilla}>
              {IDIOMAS.map((idioma) => (
                <Pastilla
                  key={idioma.code}
                  texto={idioma.texto}
                  elegida={b.languages.some((l) => l.code === idioma.code)}
                  deshabilitada={b.languages.length >= 5}
                  onPress={() =>
                    cambiar({
                      languages: b.languages.some((l) => l.code === idioma.code)
                        ? b.languages.filter((l) => l.code !== idioma.code)
                        : b.languages.length >= 5
                          ? b.languages
                          : [...b.languages, { code: idioma.code, level: 'MEDIO' }],
                    })
                  }
                />
              ))}
            </View>
            {b.languages.map((elegido) => (
              <View key={elegido.code} style={{ gap: 6 }}>
                <Text style={estilos.ayuda}>
                  {IDIOMAS.find((i) => i.code === elegido.code)?.texto}
                </Text>
                <View style={estilos.rejilla}>
                  {NIVELES.map((nivel) => (
                    <Pastilla
                      key={nivel.valor}
                      texto={nivel.texto}
                      elegida={elegido.level === nivel.valor}
                      onPress={() =>
                        cambiar({
                          languages: b.languages.map((l) =>
                            l.code === elegido.code ? { ...l, level: nivel.valor } : l,
                          ),
                        })
                      }
                    />
                  ))}
                </View>
              </View>
            ))}
          </View>
        ),
        listo: b.languages.length > 0,
      },
      9: {
        titulo: '¿Cómo te relacionas?',
        cuerpo: (
          <Escala
            valor={b.sociability}
            onChange={(v) => cambiar({ sociability: v })}
            izquierda="Me cuesta arrancar"
            derecha="Hablo con cualquiera"
          />
        ),
        listo: true,
      },
      10: {
        titulo: '¿Qué conversación te gusta?',
        cuerpo: (
          <Escala
            valor={b.conversationDepth}
            onChange={(v) => cambiar({ conversationDepth: v })}
            izquierda="Ligera y divertida"
            derecha="De las que van hondo"
          />
        ),
        listo: true,
      },
      11: {
        titulo: '¿Qué buscas?',
        cuerpo: (
          <View style={estilos.rejilla}>
            {INTENCIONES.map((i) => (
              <Pastilla
                key={i.valor}
                texto={i.texto}
                elegida={b.intent === i.valor}
                onPress={() => cambiar({ intent: i.valor })}
              />
            ))}
          </View>
        ),
        listo: b.intent !== null,
      },
      12: {
        titulo: 'Elige entre 5 y 8 intereses',
        ayuda: `De aquí sale la primera pregunta de cada conversación. Llevas ${b.interests.length}.`,
        cuerpo: (
          <View style={estilos.rejilla}>
            {catalogo.map((interes) => (
              <Pastilla
                key={interes.name}
                texto={interes.label}
                elegida={b.interests.includes(interes.name)}
                deshabilitada={b.interests.length >= INTERESES_MAX}
                onPress={() =>
                  cambiar({ interests: alterna(b.interests, interes.name, INTERESES_MAX) })
                }
              />
            ))}
          </View>
        ),
        listo: b.interests.length >= INTERESES_MIN && b.interests.length <= INTERESES_MAX,
      },
      13: {
        titulo: 'Dos líneas sobre ti',
        ayuda:
          'Opcional. No se ve al principio: aparece en el nivel 2, cuando la conversación ya va bien.',
        cuerpo: (
          <Campo
            valor={b.bio}
            onChange={(v) => cambiar({ bio: v })}
            placeholder="Lo que quieras contar"
            maxLength={200}
            multiline
          />
        ),
        listo: true,
      },
    };

  const actual = pasos[paso];
  const ultimo = paso === TOTAL_PASOS;

  return (
    <View style={estilos.pantalla}>
      <View style={estilos.cabecera}>
        <Progreso paso={paso} total={TOTAL_PASOS} />
        <Text style={estilos.contador}>
          Pregunta {paso} de {TOTAL_PASOS}
        </Text>
      </View>

      <ScrollView contentContainerStyle={estilos.contenido} keyboardShouldPersistTaps="handled">
        <Text style={estilos.titulo}>{actual.titulo}</Text>
        {actual.ayuda && <Text style={estilos.ayuda}>{actual.ayuda}</Text>}
        <View style={{ marginTop: 8 }}>{actual.cuerpo}</View>
        {error && <Text style={estilos.error}>{error}</Text>}
      </ScrollView>

      <View style={estilos.pie}>
        {paso > 1 && (
          <Pressable onPress={() => setPaso(paso - 1)} style={estilos.atras}>
            <Text style={estilos.atrasTexto}>Atrás</Text>
          </Pressable>
        )}
        <View style={{ flex: 1 }}>
          <Boton
            texto={ultimo ? 'Terminar el registro' : 'Siguiente'}
            onPress={() => (ultimo ? terminar() : setPaso(paso + 1))}
            deshabilitado={!actual.listo}
            ocupado={ocupado && ultimo}
          />
        </View>
      </View>
    </View>
  );
}

/** Fecha real y con 18 años cumplidos. El servidor lo vuelve a comprobar. */
function fechaValida(b: Borrador): boolean {
  const dia = Number(b.dia);
  const mes = Number(b.mes);
  const anio = Number(b.anio);
  if (!dia || !mes || !anio || b.anio.length !== 4) return false;
  const fecha = new Date(anio, mes - 1, dia);
  if (
    fecha.getFullYear() !== anio ||
    fecha.getMonth() !== mes - 1 ||
    fecha.getDate() !== dia
  ) {
    return false;
  }
  const hoy = new Date();
  const decimoctavo = new Date(anio + 18, mes - 1, dia);
  return decimoctavo <= hoy;
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg, paddingTop: 56 },
  cabecera: { paddingHorizontal: 24, gap: 6 },
  contador: { fontSize: 12, color: colors.ink3 },
  contenido: { padding: 24, gap: 10, paddingBottom: 40 },
  titulo: { fontSize: 24, fontWeight: '700', color: colors.ink },
  ayuda: { fontSize: 14, color: colors.ink3, lineHeight: 20 },
  ok: { fontSize: 15, color: colors.ok, fontWeight: '600' },
  error: { fontSize: 14, color: colors.error, marginTop: 12 },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  pie: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 24,
    paddingTop: 12,
  },
  atras: { paddingVertical: 14, paddingHorizontal: 8 },
  atrasTexto: { color: colors.ink3, fontSize: 15 },
});
