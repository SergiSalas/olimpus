import { StyleSheet, Text, View } from 'react-native';
import { Etiqueta, Rebote, Tarjeta } from '../components';
import { colors, fonts, radios } from '../theme';
import { t } from '../i18n';

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
        <Etiqueta tono="accent">{t('decision.respondido')}</Etiqueta>
        <Text style={estilos.texto}>
          {respondido === 'YES' ? t('decision.dijisteSi') : t('decision.dijisteNo')}
        </Text>
        <Text style={estilos.pista}>{t('decision.cambiarIdea')}</Text>
      </Tarjeta>
    );
  }

  return (
    <Tarjeta>
      <Etiqueta tono="accent">{t('decision.ultimosMinutos')}</Etiqueta>
      <Text style={estilos.pregunta}>{t('decision.pregunta')}</Text>
      <Text style={estilos.pista}>{t('decision.anonimo')}</Text>

      <View style={{ flexDirection: 'row', gap: 9, marginTop: 4 }}>
        <Rebote
          fuera={{ flex: 1 }}
          style={[estilos.si, ocupado && estilos.apagado]}
          onPress={() => onResponder('YES')}
          disabled={ocupado}>
          <Text style={estilos.siTexto}>{t('decision.si')}</Text>
        </Rebote>
        <Rebote
          fuera={{ flex: 1 }}
          style={[estilos.no, ocupado && estilos.apagado]}
          onPress={() => onResponder('NO')}
          disabled={ocupado}>
          <Text style={estilos.noTexto}>{t('decision.no')}</Text>
        </Rebote>
      </View>
    </Tarjeta>
  );
}

const estilos = StyleSheet.create({
  pregunta: { fontFamily: fonts.display, fontSize: 21, lineHeight: 26, color: colors.ink },
  texto: { fontFamily: fonts.sans, fontSize: 15, lineHeight: 22, color: colors.ink2 },
  pista: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  si: {
    height: 52,
    borderRadius: radios.boton,
    backgroundColor: colors.accent,
    borderBottomWidth: 4,
    borderColor: colors.accentOscuro,
    alignItems: 'center',
    justifyContent: 'center',
  },
  siTexto: { fontFamily: fonts.sansNegrita, fontSize: 16.5, color: '#FFFFFF' },
  no: {
    height: 52,
    borderRadius: radios.boton,
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    alignItems: 'center',
    justifyContent: 'center',
  },
  noTexto: { fontFamily: fonts.sansNegrita, fontSize: 16.5, color: colors.ink2 },
  apagado: { opacity: 0.5 },
});
