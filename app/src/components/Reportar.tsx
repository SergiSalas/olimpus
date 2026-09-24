import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, reportar, type ReportReason } from '../api';
import { Boton, Etiqueta } from '../components';
import { colors, fonts, radios } from '../theme';

const MOTIVOS: { valor: ReportReason; etiqueta: string }[] = [
  { valor: 'DISRESPECT', etiqueta: 'Me falta al respeto' },
  { valor: 'UNWANTED_SEXUAL', etiqueta: 'Contenido sexual que no he pedido' },
  { valor: 'SPAM', etiqueta: 'Spam o me quiere vender algo' },
  { valor: 'FAKE_PROFILE', etiqueta: 'No parece quien dice ser' },
  { valor: 'LOOKS_UNDERAGE', etiqueta: 'Parece menor de edad' },
  { valor: 'OTHER', etiqueta: 'Otra cosa' },
];

/**
 * Reportar o bloquear, en un toque.
 *
 * La conversación se corta al momento; la revisión viene después. Quien lo está
 * pasando mal no tiene que esperar a que nadie revise nada.
 */
export function Reportar({
  visible,
  token,
  conversationId,
  onCerrar,
  onHecho,
}: {
  visible: boolean;
  token: string;
  conversationId: string;
  onCerrar: () => void;
  onHecho: () => void;
}) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function enviar(motivo: ReportReason | null) {
    setEnviando(true);
    setError(null);
    try {
      await reportar(token, conversationId, motivo);
      onHecho();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo enviar.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onCerrar}>
      <View style={estilos.fondo}>
        <View style={estilos.hoja}>
          <Etiqueta tono="accent">Reportar o bloquear</Etiqueta>
          <Text style={estilos.titulo}>¿Qué ha pasado?</Text>
          <Text style={estilos.ayuda}>
            La conversación se corta ahora mismo y no os volveremos a emparejar. La otra persona no
            sabrá que has sido tú.
          </Text>

          <View style={{ gap: 8, marginTop: 6 }}>
            {MOTIVOS.map((motivo) => (
              <Pressable
                key={motivo.valor}
                style={estilos.motivo}
                disabled={enviando}
                onPress={() => enviar(motivo.valor)}>
                <Text style={estilos.motivoTexto}>{motivo.etiqueta}</Text>
              </Pressable>
            ))}
          </View>

          {error && <Text style={estilos.error}>{error}</Text>}

          <View style={{ gap: 8, marginTop: 10 }}>
            <Boton
              texto="Solo bloquear, sin motivo"
              tono="oscuro"
              ocupado={enviando}
              onPress={() => enviar(null)}
            />
            <Pressable style={{ paddingVertical: 12 }} onPress={onCerrar} disabled={enviando}>
              <Text style={estilos.cancelar}>Cancelar</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const estilos = StyleSheet.create({
  fondo: { flex: 1, backgroundColor: colors.velo, justifyContent: 'flex-end' },
  hoja: {
    backgroundColor: colors.bg,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    paddingBottom: 40,
    gap: 8,
  },
  titulo: { fontFamily: fonts.display, fontSize: 28, color: colors.ink },
  ayuda: { fontFamily: fonts.sans, fontSize: 14, lineHeight: 20, color: colors.ink3 },
  motivo: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radios.fila,
    paddingVertical: 15,
    paddingHorizontal: 16,
  },
  motivoTexto: { fontFamily: fonts.sansMedia, fontSize: 15.5, color: colors.ink },
  cancelar: { fontFamily: fonts.sansMedia, fontSize: 15, color: colors.ink3, textAlign: 'center' },
  error: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.error, textAlign: 'center' },
});
