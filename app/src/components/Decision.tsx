import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Etiqueta, Tarjeta } from '../components';
import { colors, fonts, radios } from '../theme';

/**
 * La pregunta del final del día.
 *
 * Aparece en la última media hora, mientras la conversación sigue viva: una vez
 * cortada, la gente responde en frío y por cortesía.
 *
 * No se dice nunca qué respondió el otro. Si no coincidís, nadie sabe quién
 * dijo que no, y por eso decir que no no le cuesta nada a nadie.
 */
export function PreguntaFinal({
  respondido,
  ocupado,
  onResponder,
}: {
  respondido: 'YES' | 'NO' | null;
  ocupado: boolean;
  onResponder: (respuesta: 'YES' | 'NO') => void;
}) {
  if (respondido) {
    return (
      <Tarjeta>
        <Etiqueta tono="accent">Respondido</Etiqueta>
        <Text style={estilos.texto}>
          {respondido === 'YES'
            ? 'Has dicho que sí. A las 22:00 sabrás si la otra persona también quiso seguir.'
            : 'Has dicho que no. La otra persona no sabrá qué respondiste.'}
        </Text>
        <Text style={estilos.pista}>Puedes cambiar de idea hasta las 22:00.</Text>
      </Tarjeta>
    );
  }

  return (
    <Tarjeta>
      <Etiqueta tono="accent">Últimos minutos</Etiqueta>
      <Text style={estilos.pregunta}>¿Quieres seguir conociendo a esta persona?</Text>
      <Text style={estilos.pista}>
        Cada uno responde por su cuenta. Si no coincidís, nadie sabe qué respondió el otro.
      </Text>

      <View style={{ gap: 9, marginTop: 4 }}>
        <Pressable
          style={[estilos.si, ocupado && estilos.apagado]}
          onPress={() => onResponder('YES')}
          disabled={ocupado}>
          <Text style={estilos.siTexto}>Sí, seguir</Text>
        </Pressable>
        <Pressable
          style={[estilos.no, ocupado && estilos.apagado]}
          onPress={() => onResponder('NO')}
          disabled={ocupado}>
          <Text style={estilos.noTexto}>No, aquí lo dejo</Text>
        </Pressable>
      </View>
    </Tarjeta>
  );
}

const estilos = StyleSheet.create({
  pregunta: { fontFamily: fonts.serif, fontSize: 26, lineHeight: 29, color: colors.ink },
  texto: { fontFamily: fonts.sans, fontSize: 15, lineHeight: 22, color: colors.ink2 },
  pista: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  si: {
    height: 56,
    borderRadius: radios.boton,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  siTexto: { fontFamily: fonts.sansNegrita, fontSize: 16.5, color: '#FFFFFF' },
  no: {
    height: 56,
    borderRadius: radios.boton,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  noTexto: { fontFamily: fonts.sansNegrita, fontSize: 16.5, color: colors.ink2 },
  apagado: { opacity: 0.5 },
});
