import { useRef, useState } from 'react';
import {
  Dimensions,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';
import { Boton, BotonPlano, Etiqueta } from '../components';
import { colors, fonts, radios, text } from '../theme';

const NIVELES = [
  { titulo: 'Nivel 0 · Match', desc: 'Edad, dos intereses, zona' },
  { titulo: 'Nivel 1 · Primer mensaje', desc: 'Apodo y todos los intereses' },
  { titulo: 'Nivel 2 · Conversación', desc: 'Nota de voz y bio corta' },
  { titulo: 'Nivel 3 · Buena conexión', desc: 'Foto completa, si los dos aceptan' },
  { titulo: 'Nivel 4 · Confianza', desc: 'Lo que cada uno quiera compartir' },
];

/**
 * Los tres paneles de bienvenida. Explican el producto antes de pedir nada:
 * primero se ve de qué va, y solo después se pide el email.
 */
export function IntroScreen({ onEmpezar }: { onEmpezar: () => void }) {
  const [panel, setPanel] = useState(0);
  const scroll = useRef<ScrollView>(null);
  const ancho = Dimensions.get('window').width;

  function irA(indice: number) {
    setPanel(indice);
    scroll.current?.scrollTo({ x: indice * ancho, animated: true });
  }

  function alDeslizar(evento: NativeSyntheticEvent<NativeScrollEvent>) {
    setPanel(Math.round(evento.nativeEvent.contentOffset.x / ancho));
  }

  return (
    <View style={estilos.pantalla}>
      <ScrollView
        ref={scroll}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={alDeslizar}
        style={{ flex: 1 }}>
        <View style={[estilos.panel, { width: ancho }]}>
          <View style={estilos.centro}>
            <View style={estilos.burbujaSuya}>
              <Text style={estilos.textoSuyo}>¿Montaña o rocódromo?</Text>
            </View>
            <View style={estilos.burbujaMia}>
              <Text style={estilos.textoMio}>Montaña. El rocódromo es el plan B de febrero.</Text>
            </View>
            <View style={estilos.sinFoto}>
              <View style={estilos.circuloFoto}>
                <Text style={estilos.interrogante}>?</Text>
              </View>
              <Text style={estilos.sinFotoTexto}>
                Su foto llega cuando la conversación se la gane
              </Text>
            </View>
          </View>
          <Text style={[text.tituloGrande, { marginBottom: 10 }]}>
            Habla primero.{'\n'}
            <Text style={{ fontFamily: fonts.serifItalic }}>Ver</Text> viene después.
          </Text>
          <Text style={text.cuerpo}>
            Aquí nadie juzga por una foto: el perfil se descubre conforme la conversación va bien.
          </Text>
        </View>

        <View style={[estilos.panel, { width: ancho }]}>
          <View style={estilos.centro}>
            <View style={estilos.cajaPunteada}>
              <Etiqueta>Las apps de siempre</Etiqueta>
              <Text style={estilos.ordenViejo}>Foto → Juicio → ¿Hablar?</Text>
            </View>
            <View style={estilos.cajaOscura}>
              <Etiqueta tono="claro">Aquí</Etiqueta>
              <Text style={estilos.ordenNuevo}>
                Conversación <Text style={{ color: colors.accent }}>→</Text> Interés{' '}
                <Text style={{ color: colors.accent }}>→</Text> Foto
              </Text>
            </View>
          </View>
          <Text style={[text.titulo, { fontSize: 38, lineHeight: 40, marginBottom: 10 }]}>
            Cambiamos el orden
          </Text>
          <Text style={text.cuerpo}>
            No emparejamos mejor: empezamos por otro sitio. Hablas, y solo si te interesa esa
            persona llega su cara.
          </Text>
        </View>

        <View style={[estilos.panel, { width: ancho }]}>
          <View style={[estilos.centro, { justifyContent: 'center' }]}>
            {NIVELES.map((nivel, i) => (
              <View key={nivel.titulo} style={estilos.nivel}>
                <View style={estilos.nivelIzquierda}>
                  <View style={[estilos.punto, i === 0 && estilos.puntoLleno]} />
                  {i < NIVELES.length - 1 && <View style={estilos.linea} />}
                </View>
                <View style={{ paddingBottom: 14, flexShrink: 1 }}>
                  <Text style={[estilos.nivelTitulo, i === 0 && { color: colors.ink }]}>
                    {nivel.titulo}
                  </Text>
                  <Text style={estilos.nivelDesc}>{nivel.desc}</Text>
                </View>
              </View>
            ))}
          </View>
          <Text style={[text.titulo, { fontSize: 38, lineHeight: 40, marginBottom: 10 }]}>
            Se desbloquea por tramos
          </Text>
          <Text style={text.cuerpo}>
            Hace falta ida y vuelta y también tiempo. Y de la foto en adelante, decidís los dos.
          </Text>
        </View>
      </ScrollView>

      <View style={estilos.pie}>
        <View style={estilos.puntos}>
          {[0, 1, 2].map((i) => (
            <Pressable
              key={i}
              onPress={() => irA(i)}
              style={[estilos.puntoPagina, i === panel && estilos.puntoPaginaActivo]}
            />
          ))}
        </View>
        <Boton
          texto={panel === 2 ? 'Crear mi cuenta' : 'Siguiente'}
          onPress={() => (panel === 2 ? onEmpezar() : irA(panel + 1))}
        />
        <BotonPlano texto="Ya tengo cuenta" onPress={onEmpezar} />
      </View>
    </View>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bgCalido },
  panel: { paddingHorizontal: 26, paddingTop: 70, flex: 1 },
  centro: { flex: 1, justifyContent: 'center', gap: 14 },

  burbujaSuya: {
    borderRadius: 20,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    paddingHorizontal: 20,
    paddingVertical: 18,
  },
  textoSuyo: { fontFamily: fonts.sans, fontSize: 16, lineHeight: 23, color: colors.ink },
  burbujaMia: { borderRadius: 20, backgroundColor: colors.ink, paddingHorizontal: 20, paddingVertical: 18 },
  textoMio: { fontFamily: fonts.sans, fontSize: 16, lineHeight: 23, color: colors.onInk },
  sinFoto: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 4 },
  circuloFoto: {
    width: 52,
    height: 52,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  interrogante: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.ink5 },
  sinFotoTexto: { fontFamily: fonts.sansMedia, fontSize: 13.5, color: colors.ink3, flexShrink: 1 },

  cajaPunteada: {
    borderRadius: 20,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: '#D8CDBB',
    padding: 18,
    gap: 10,
  },
  ordenViejo: { fontFamily: fonts.sansMedia, fontSize: 15, color: colors.ink5 },
  cajaOscura: { borderRadius: 20, backgroundColor: colors.ink, padding: 18, gap: 10 },
  ordenNuevo: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.onInk },

  nivel: { flexDirection: 'row', gap: 14, alignItems: 'flex-start' },
  nivelIzquierda: { width: 26, alignItems: 'center' },
  punto: { width: 11, height: 11, borderRadius: 999, borderWidth: 1.5, borderColor: '#D8CDBB' },
  puntoLleno: { backgroundColor: colors.accent, borderColor: colors.accent },
  linea: { width: 1.5, flex: 1, minHeight: 26, backgroundColor: colors.lineFuerte },
  nivelTitulo: { fontFamily: fonts.sansNegrita, fontSize: 14.5, color: colors.ink3 },
  nivelDesc: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink3 },

  pie: { paddingHorizontal: 26, paddingTop: 20, paddingBottom: 38 },
  puntos: { flexDirection: 'row', gap: 7, marginBottom: 18 },
  puntoPagina: { width: 7, height: 7, borderRadius: 999, backgroundColor: colors.lineFuerte },
  puntoPaginaActivo: { width: 22, backgroundColor: colors.accent },
});
