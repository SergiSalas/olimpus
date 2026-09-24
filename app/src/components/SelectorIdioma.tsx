import { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { Rebote } from '../components';
import { cambiarIdioma, IDIOMAS_APP, t, useIdioma } from '../i18n';
import { colors, fonts } from '../theme';

/**
 * La hoja para elegir el idioma de la app. Al elegir, la pantalla se vuelve a
 * montar ya traducida (App usa el idioma en su key), así que no hay que
 * reiniciar nada.
 */
export function HojaIdioma({ visible, onCerrar }: { visible: boolean; onCerrar: () => void }) {
  const actual = useIdioma();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onCerrar}>
      <View style={estilos.velo}>
        <Pressable style={{ flex: 1 }} onPress={onCerrar} />
        <View style={estilos.hoja}>
          <View style={estilos.asa} />
          <Text style={estilos.titulo}>{t('idiomaApp.titulo')}</Text>
          <Text style={estilos.ayuda}>{t('idiomaApp.ayuda')}</Text>
          <View style={{ gap: 10, marginTop: 6 }}>
            {IDIOMAS_APP.map((i) => (
              <Rebote
                key={i.codigo}
                pop={i.codigo === actual}
                style={[estilos.fila, i.codigo === actual && estilos.filaElegida]}
                onPress={() => {
                  onCerrar();
                  if (i.codigo !== actual) cambiarIdioma(i.codigo);
                }}>
                <Text style={estilos.bandera}>{i.bandera}</Text>
                <Text style={estilos.nombre}>{i.nombre}</Text>
                {i.codigo === actual && <Text style={estilos.marca}>✓</Text>}
              </Rebote>
            ))}
          </View>
        </View>
      </View>
    </Modal>
  );
}

/** La pastilla "🇪🇸 ES" de la bienvenida, para quien aún no tiene cuenta. */
export function PastillaIdioma() {
  const actual = useIdioma();
  const [abierta, setAbierta] = useState(false);
  const idioma = IDIOMAS_APP.find((i) => i.codigo === actual);

  return (
    <>
      <Rebote style={estilos.pastilla} onPress={() => setAbierta(true)}>
        <Text style={estilos.pastillaTexto}>
          {idioma?.bandera} {actual.toUpperCase()}
        </Text>
      </Rebote>
      <HojaIdioma visible={abierta} onCerrar={() => setAbierta(false)} />
    </>
  );
}

const estilos = StyleSheet.create({
  velo: { flex: 1, backgroundColor: colors.velo },
  hoja: {
    backgroundColor: colors.bg,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 22,
    paddingTop: 12,
    paddingBottom: 40,
    gap: 8,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.trazo,
    marginBottom: 8,
  },
  titulo: { fontFamily: fonts.display, fontSize: 24, color: colors.ink },
  ayuda: { fontFamily: fonts.sans, fontSize: 13.5, lineHeight: 20, color: colors.ink3 },
  fila: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  filaElegida: { borderColor: colors.accent, backgroundColor: colors.accentWash },
  bandera: { fontSize: 24 },
  nombre: { flex: 1, fontFamily: fonts.sansNegrita, fontSize: 16, color: colors.ink },
  marca: { fontFamily: fonts.displayFuerte, fontSize: 18, color: colors.accent },
  pastilla: {
    backgroundColor: 'rgba(255,255,255,0.7)',
    borderRadius: 999,
    paddingHorizontal: 11,
    paddingVertical: 6,
  },
  pastillaTexto: { fontFamily: fonts.sansNegrita, fontSize: 12.5, color: colors.ink },
});
