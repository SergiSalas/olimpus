import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ownPhotoSource, type Me, type Profile } from '../api';
import { Boton, Etiqueta, Pastilla, Tarjeta } from '../components';
import { useNombreInteres } from '../interests';
import { tipoDeProfundidad } from '../mapeo';
import { colors, fonts, text } from '../theme';

const GENEROS: Record<string, string> = {
  WOMAN: 'Mujer',
  MAN: 'Hombre',
  NON_BINARY: 'No binarie',
  OTHER: 'Prefiero no decirlo',
};

const BUSCA: Record<string, string> = {
  WOMAN: 'mujeres',
  MAN: 'hombres',
  NON_BINARY: 'personas no binarias',
  OTHER: 'todo el mundo',
};

const INTENCIONES: Record<string, string> = {
  FRIENDSHIP: 'Amistad',
  DATING: 'Citas',
  RELATIONSHIP: 'Pareja',
  CASUAL: 'Algo casual',
};

const IDIOMAS: Record<string, string> = {
  es: 'Español',
  en: 'Inglés',
  ca: 'Catalán',
  gl: 'Gallego',
  eu: 'Euskera',
  fr: 'Francés',
  pt: 'Portugués',
  it: 'Italiano',
  de: 'Alemán',
  ar: 'Árabe',
};

const SOCIABILIDAD = [
  'Me cuesta arrancar',
  'Más bien reservado',
  'Según el día',
  'Bastante sociable',
  'Hablo con cualquiera',
];

/** Lo que se guardó en el registro, para poder repasarlo y cambiarlo. */
export function HomeScreen({
  me,
  perfil,
  token,
  onVolver,
  onEditar,
  onSalir,
}: {
  me: Me;
  perfil: Profile;
  token: string;
  onVolver: () => void;
  onEditar: () => void;
  onSalir: () => void;
}) {
  const nombreInteres = useNombreInteres();

  return (
    <ScrollView style={estilos.pantalla} contentContainerStyle={estilos.contenido}>
      <Pressable style={estilos.redondo} onPress={onVolver}>
        <Text style={estilos.flecha}>←</Text>
      </Pressable>

      <Text style={text.titulo}>Tu registro</Text>
      <Text style={[text.ayuda, { marginBottom: 4 }]}>
        Es solo el punto de partida: después pesa más con quién sigues hablando de verdad.
      </Text>

      <Tarjeta>
        <Etiqueta>Tu foto · nadie la ve hasta el nivel 3</Etiqueta>
        <View style={{ alignItems: 'center', paddingVertical: 4 }}>
          <Image source={ownPhotoSource(token)} style={estilos.foto} />
        </View>
      </Tarjeta>

      <Tarjeta>
        <Etiqueta>Lo básico</Etiqueta>
        <Fila clave="Apodo" valor={perfil.nickname} />
        <Fila clave="Edad" valor={`${perfil.age} años`} />
        <Fila clave="Género" valor={GENEROS[perfil.gender] ?? perfil.gender} />
        <Fila
          clave="Quieres hablar con"
          valor={perfil.seeking.map((g) => BUSCA[g] ?? g).join(', ')}
        />
        <Fila clave="Edades" valor={`${perfil.ageMin} – ${perfil.ageMax} años`} />
        <Fila clave="Distancia" valor={`hasta ${perfil.maxDistanceKm} km`} />
      </Tarjeta>

      <Tarjeta>
        <Etiqueta>Compatibilidad</Etiqueta>
        <Fila
          clave="Idiomas"
          valor={perfil.languages.map((l) => IDIOMAS[l.code] ?? l.code).join(', ')}
        />
        <Fila clave="Cómo te relacionas" valor={SOCIABILIDAD[perfil.sociability - 1]} />
        <Fila clave="Conversación" valor={tipoDeProfundidad(perfil.conversationDepth)} />
        <Fila clave="Buscas" valor={INTENCIONES[perfil.intent] ?? perfil.intent} />
      </Tarjeta>

      <Tarjeta>
        <Etiqueta>Tus intereses</Etiqueta>
        <View style={estilos.rejilla}>
          {perfil.interests.map((interes) => (
            <Pastilla key={interes} texto={nombreInteres(interes)} elegida tono="suave" />
          ))}
        </View>
        {perfil.bio.length > 0 && (
          <>
            <Etiqueta>Tu bio · se ve en el nivel 2</Etiqueta>
            <Text style={estilos.bio}>{perfil.bio}</Text>
          </>
        )}
      </Tarjeta>

      <Boton texto="Cambiar mis respuestas" onPress={onEditar} tono="oscuro" />

      <Pressable style={{ paddingVertical: 14 }} onPress={onSalir}>
        <Text style={estilos.enlaceFlojo}>Cerrar sesión ({me.email})</Text>
      </Pressable>
    </ScrollView>
  );
}

function Fila({ clave, valor }: { clave: string; valor: string }) {
  return (
    <View style={estilos.fila}>
      <Text style={estilos.claveTexto}>{clave}</Text>
      <Text style={estilos.valorTexto}>{valor}</Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  contenido: { paddingHorizontal: 26, paddingTop: 58, paddingBottom: 40, gap: 14 },
  redondo: {
    width: 36,
    height: 36,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 17, color: colors.ink2 },
  fila: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', gap: 14 },
  claveTexto: { fontFamily: fonts.sans, fontSize: 14.5, color: colors.ink3 },
  valorTexto: {
    fontFamily: fonts.sansNegrita,
    fontSize: 14.5,
    color: colors.ink,
    textAlign: 'right',
    flexShrink: 1,
  },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  foto: { width: 150, aspectRatio: 3 / 4, borderRadius: 14, backgroundColor: colors.surface2 },
  bio: { fontFamily: fonts.sans, fontSize: 14.5, lineHeight: 21, color: colors.ink2 },
  enlaceFlojo: {
    fontFamily: fonts.sansMedia,
    fontSize: 13.5,
    color: colors.ink4,
    textAlign: 'center',
  },
});
