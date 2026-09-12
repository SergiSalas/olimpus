import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { type Me, type Profile } from '../api';
import { colors } from '../theme';

const GENEROS: Record<string, string> = {
  MUJER: 'Mujer',
  HOMBRE: 'Hombre',
  NO_BINARIO: 'No binario',
  OTRO: 'Otro',
};

const INTENCIONES: Record<string, string> = {
  AMISTAD: 'Amistad',
  CITAS: 'Citas',
  PAREJA: 'Pareja',
  CASUAL: 'Algo casual',
};

const ESCALA_SOCIAL = [
  'Me cuesta arrancar',
  'Más bien reservado',
  'Según el día',
  'Bastante sociable',
  'Hablo con cualquiera',
];

const ESCALA_CHARLA = [
  'Ligera y divertida',
  'Más bien ligera',
  'De todo un poco',
  'Más bien honda',
  'De las que van hondo',
];

/**
 * Pantalla de "ya estás dentro". En el paso 4 la sustituye la conversación
 * del día; por ahora resume lo que el registro ha guardado.
 */
export function HomeScreen({
  me,
  perfil,
  onEditar,
  onSalir,
}: {
  me: Me;
  perfil: Profile;
  onEditar: () => void;
  onSalir: () => void;
}) {
  return (
    <ScrollView style={estilos.pantalla} contentContainerStyle={estilos.contenido}>
      <Text style={estilos.titulo}>Olimpus</Text>
      <Text style={estilos.saludo}>Hola, {perfil.nickname}</Text>

      <View style={estilos.aviso}>
        <Text style={estilos.avisoTexto}>
          Tu registro está guardado. Todavía no hay reparto de conversaciones: llegará en el paso 4,
          con la ronda diaria de las 4:00.
        </Text>
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.seccion}>Tu registro</Text>
        <Fila etiqueta="Edad" valor={`${perfil.age} años`} />
        <Fila etiqueta="Género" valor={GENEROS[perfil.gender] ?? perfil.gender} />
        <Fila
          etiqueta="Quieres hablar con"
          valor={perfil.seeking.map((g) => GENEROS[g] ?? g).join(', ')}
        />
        <Fila etiqueta="Edades" valor={`${perfil.ageMin} a ${perfil.ageMax} años`} />
        <Fila etiqueta="Distancia" valor={`hasta ${perfil.maxDistanceKm} km`} />
        <Fila
          etiqueta="Idiomas"
          valor={perfil.languages.map((l) => `${l.code.toUpperCase()} (${l.level.toLowerCase()})`).join(', ')}
        />
        <Fila etiqueta="Cómo te relacionas" valor={ESCALA_SOCIAL[perfil.sociability - 1]} />
        <Fila etiqueta="Conversación" valor={ESCALA_CHARLA[perfil.conversationDepth - 1]} />
        <Fila etiqueta="Buscas" valor={INTENCIONES[perfil.intent] ?? perfil.intent} />
      </View>

      <View style={estilos.tarjeta}>
        <Text style={estilos.seccion}>Tus intereses</Text>
        <View style={estilos.rejilla}>
          {perfil.interests.map((interes) => (
            <View key={interes} style={estilos.chip}>
              <Text style={estilos.chipTexto}>{interes.replace(/-/g, ' ')}</Text>
            </View>
          ))}
        </View>
        {perfil.bio.length > 0 && (
          <>
            <Text style={[estilos.seccion, { marginTop: 10 }]}>Tu bio (se ve en el nivel 2)</Text>
            <Text style={estilos.bio}>{perfil.bio}</Text>
          </>
        )}
      </View>

      <Pressable style={estilos.botonSecundario} onPress={onEditar}>
        <Text style={estilos.botonSecundarioTexto}>Cambiar mis respuestas</Text>
      </Pressable>
      <Pressable style={estilos.botonSecundario} onPress={onSalir}>
        <Text style={estilos.botonSecundarioTexto}>Cerrar sesión ({me.email})</Text>
      </Pressable>
    </ScrollView>
  );
}

function Fila({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <View style={estilos.fila}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      <Text style={estilos.valor}>{valor}</Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  contenido: { padding: 24, paddingTop: 60, gap: 14, paddingBottom: 48 },
  titulo: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.ink,
    textAlign: 'center',
    letterSpacing: 1,
  },
  saludo: { fontSize: 18, color: colors.ink2, textAlign: 'center' },
  aviso: {
    backgroundColor: colors.surface,
    borderLeftWidth: 3,
    borderLeftColor: colors.accent,
    borderRadius: 6,
    padding: 14,
  },
  avisoTexto: { fontSize: 13, color: colors.ink2, lineHeight: 19 },
  tarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
    gap: 8,
  },
  seccion: { fontSize: 12, fontWeight: '700', color: colors.ink3, textTransform: 'uppercase' },
  fila: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  etiqueta: { fontSize: 13, color: colors.ink3, flexShrink: 0 },
  valor: { fontSize: 13, color: colors.ink, fontWeight: '600', flexShrink: 1, textAlign: 'right' },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip: {
    backgroundColor: colors.bg,
    borderRadius: 14,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  chipTexto: { fontSize: 13, color: colors.ink2 },
  bio: { fontSize: 14, color: colors.ink2, lineHeight: 20 },
  botonSecundario: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  botonSecundarioTexto: { color: colors.ink2, fontWeight: '600', fontSize: 14 },
});
