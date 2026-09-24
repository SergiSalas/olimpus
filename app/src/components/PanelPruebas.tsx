import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { ApiError, advanceDemo, startDemo, type PasoDemo } from '../api';
import { Rebote } from '../components';
import { colors, fonts } from '../theme';

const PASOS: { paso: PasoDemo; emoji: string; titulo: string; detalle: string }[] = [
  {
    paso: 'HOUR',
    emoji: '⏩',
    titulo: '+1 hora',
    detalle: 'Nivel 2, si ya os habéis escrito 3 veces cada uno',
  },
  { paso: 'PHOTO', emoji: '📸', titulo: '+4 horas', detalle: 'Aparece «quiero verte»' },
  { paso: 'DECISION', emoji: '⏳', titulo: 'Última media hora', detalle: 'Se abre la pregunta final' },
  { paso: 'CLOSE', emoji: '🌙', titulo: 'Cerrar el día ya', detalle: 'Si los dos dijisteis sí, conexión' },
];

/**
 * El mando de pruebas: una persona de prueba que contesta sola y botones para
 * adelantar el reloj. Solo existe en desarrollo; en una build de verdad no se
 * pinta, y el servidor de la beta ni siquiera tiene esos endpoints.
 */
export function PanelPruebas({ token, onHecho }: { token: string; onHecho: () => void }) {
  const [abierto, setAbierto] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [nota, setNota] = useState<string | null>(null);

  if (!__DEV__) return null;

  async function hacer(accion: () => Promise<string>) {
    setOcupado(true);
    setNota(null);
    try {
      setNota(await accion());
      onHecho();
    } catch (e) {
      setNota(e instanceof ApiError ? e.message : 'No se pudo. ¿Está el backend en marcha?');
    } finally {
      setOcupado(false);
    }
  }

  return (
    <>
      <Rebote style={estilos.flotante} fuera={estilos.sitio} onPress={() => setAbierto(true)}>
        <Text style={estilos.flotanteTexto}>🧪</Text>
      </Rebote>

      <Modal
        visible={abierto}
        transparent
        animationType="slide"
        onRequestClose={() => setAbierto(false)}>
        <View style={estilos.velo}>
          <Pressable style={{ flex: 1 }} onPress={() => setAbierto(false)} />
          <View style={estilos.hoja}>
            <View style={estilos.asa} />
            <Text style={estilos.titulo}>🧪 Modo pruebas</Text>
            <Text style={estilos.ayuda}>
              La persona de prueba contesta sola, pide verte en cuanto puede y dice que sí al final.
            </Text>

            <Rebote
              style={estilos.nueva}
              disabled={ocupado}
              onPress={() =>
                hacer(async () => {
                  const r = await startDemo(token);
                  return `Hoy hablas con ${r.nickname}. Vuelve a la pantalla principal.`;
                })
              }>
              <Text style={estilos.nuevaTexto}>✨ Nueva conversación de prueba</Text>
            </Rebote>

            <View style={estilos.rejilla}>
              {PASOS.map((p) => (
                <Rebote
                  key={p.paso}
                  fuera={{ width: '48%' }}
                  style={estilos.paso}
                  disabled={ocupado}
                  onPress={() =>
                    hacer(async () => {
                      await advanceDemo(token, p.paso);
                      return `${p.emoji} Hecho: ${p.titulo.toLowerCase()}.`;
                    })
                  }>
                  <Text style={estilos.pasoEmoji}>{p.emoji}</Text>
                  <Text style={estilos.pasoTitulo}>{p.titulo}</Text>
                  <Text style={estilos.pasoDetalle}>{p.detalle}</Text>
                </Rebote>
              ))}
            </View>

            {nota && <Text style={estilos.nota}>{nota}</Text>}
          </View>
        </View>
      </Modal>
    </>
  );
}

const estilos = StyleSheet.create({
  sitio: { position: 'absolute', right: 16, bottom: 110, zIndex: 5 },
  flotante: {
    width: 50,
    height: 50,
    borderRadius: 999,
    backgroundColor: colors.ink,
    borderBottomWidth: 4,
    borderColor: '#150A28',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
  },
  flotanteTexto: { fontSize: 22 },

  velo: { flex: 1, backgroundColor: colors.velo },
  hoja: {
    backgroundColor: colors.bg,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 22,
    paddingTop: 12,
    paddingBottom: 40,
    gap: 12,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.trazo,
  },
  titulo: { fontFamily: fonts.display, fontSize: 26, color: colors.ink },
  ayuda: { fontFamily: fonts.sans, fontSize: 13.5, lineHeight: 20, color: colors.ink3 },
  nueva: {
    height: 54,
    borderRadius: 20,
    backgroundColor: colors.uva,
    borderBottomWidth: 4,
    borderColor: '#6A3FD1',
    alignItems: 'center',
    justifyContent: 'center',
  },
  nuevaTexto: { fontFamily: fonts.sansNegrita, fontSize: 15.5, color: '#FFFFFF' },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', rowGap: 10 },
  paso: {
    minHeight: 104,
    borderRadius: 20,
    backgroundColor: colors.surface,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 12,
    gap: 3,
  },
  pasoEmoji: { fontSize: 22 },
  pasoTitulo: { fontFamily: fonts.sansNegrita, fontSize: 14.5, color: colors.ink },
  pasoDetalle: { fontFamily: fonts.sans, fontSize: 12, lineHeight: 16, color: colors.ink3 },
  nota: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.uva, textAlign: 'center' },
});
