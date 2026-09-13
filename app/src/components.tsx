import { type ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { colors, espacios, fonts, radios, text } from './theme';

/** Botón principal. Terracota el de avanzar, negro el de rematar. */
export function Boton({
  texto,
  onPress,
  ocupado,
  deshabilitado,
  tono = 'accent',
}: {
  texto: string;
  onPress: () => void;
  ocupado?: boolean;
  deshabilitado?: boolean;
  tono?: 'accent' | 'oscuro';
}) {
  const apagado = ocupado || deshabilitado;
  return (
    <Pressable
      style={[
        estilos.boton,
        tono === 'oscuro' ? estilos.botonOscuro : estilos.botonAccent,
        apagado && estilos.botonApagado,
      ]}
      onPress={onPress}
      disabled={apagado}>
      {ocupado ? (
        <ActivityIndicator color="#FFFFFF" />
      ) : (
        <Text
          style={[
            text.boton,
            apagado && !ocupado ? { color: colors.ink5 } : null,
            tono === 'oscuro' ? { color: colors.onInk } : null,
          ]}>
          {texto}
        </Text>
      )}
    </Pressable>
  );
}

/** Botón de texto, sin fondo. */
export function BotonPlano({ texto, onPress }: { texto: string; onPress: () => void }) {
  return (
    <Pressable style={estilos.botonPlano} onPress={onPress}>
      <Text style={estilos.botonPlanoTexto}>{texto}</Text>
    </Pressable>
  );
}

/** Fila de opción, con círculo (elegir una) o cuadro (elegir varias). */
export function FilaOpcion({
  etiqueta,
  detalle,
  elegida,
  varias,
  onPress,
}: {
  etiqueta: string;
  detalle?: string;
  elegida: boolean;
  varias?: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={[estilos.fila, elegida ? estilos.filaElegida : estilos.filaNormal]}
      onPress={onPress}>
      <View style={{ flexShrink: 1, gap: 3 }}>
        <Text style={[estilos.filaTexto, elegida && { fontFamily: fonts.sansNegrita }]}>
          {etiqueta}
        </Text>
        {detalle && <Text style={estilos.filaDetalle}>{detalle}</Text>}
      </View>
      <View
        style={[
          varias ? estilos.cuadro : estilos.circulo,
          elegida && (varias ? estilos.cuadroElegido : estilos.circuloElegido),
        ]}
      />
    </Pressable>
  );
}

/** Pastilla: idiomas, intereses, intereses en común. */
export function Pastilla({
  texto: etiqueta,
  elegida,
  onPress,
  deshabilitada,
  tono = 'oscuro',
}: {
  texto: string;
  elegida: boolean;
  onPress?: () => void;
  deshabilitada?: boolean;
  tono?: 'oscuro' | 'suave';
}) {
  return (
    <Pressable
      style={[
        estilos.pastilla,
        elegida
          ? tono === 'oscuro'
            ? estilos.pastillaOscura
            : estilos.pastillaSuave
          : estilos.pastillaNormal,
        deshabilitada && !elegida && { opacity: 0.45 },
      ]}
      onPress={onPress}
      disabled={!onPress || (deshabilitada && !elegida)}>
      <Text
        style={[
          estilos.pastillaTexto,
          elegida && tono === 'oscuro' && { color: colors.onInk, fontFamily: fonts.sansNegrita },
          elegida && tono === 'suave' && { color: colors.ink, fontFamily: fonts.sansMedia },
        ]}>
        {etiqueta}
      </Text>
    </Pressable>
  );
}

/** Dos opciones lado a lado: las parejas de "cómo te relacionas". */
export function DosOpciones({
  opciones,
  elegida,
  onElegir,
}: {
  opciones: string[];
  elegida: string;
  onElegir: (v: string) => void;
}) {
  return (
    <View style={{ flexDirection: 'row', gap: 9 }}>
      {opciones.map((opcion) => (
        <Pressable
          key={opcion}
          style={[estilos.segmento, opcion === elegida && estilos.segmentoElegido]}
          onPress={() => onElegir(opcion)}>
          <Text
            style={[
              estilos.segmentoTexto,
              opcion === elegida && { color: colors.ink, fontFamily: fonts.sansNegrita },
            ]}>
            {opcion}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

export function Tarjeta({ children, oscura }: { children: ReactNode; oscura?: boolean }) {
  return <View style={[estilos.tarjeta, oscura && estilos.tarjetaOscura]}>{children}</View>;
}

export function Etiqueta({ children, tono }: { children: string; tono?: 'accent' | 'claro' }) {
  return (
    <Text
      style={[
        text.etiqueta,
        tono === 'accent' && { color: colors.accent },
        tono === 'claro' && { color: colors.accentClaro },
      ]}>
      {children}
    </Text>
  );
}

export function Campo({
  valor,
  onChange,
  placeholder,
  maxLength,
  multiline,
  keyboardType,
  autoFocus,
  onSubmit,
}: {
  valor: string;
  onChange: (v: string) => void;
  placeholder?: string;
  maxLength?: number;
  multiline?: boolean;
  keyboardType?: 'default' | 'number-pad' | 'email-address';
  autoFocus?: boolean;
  onSubmit?: () => void;
}) {
  return (
    <TextInput
      style={[estilos.campo, multiline && estilos.campoLargo]}
      value={valor}
      onChangeText={onChange}
      placeholder={placeholder}
      placeholderTextColor={colors.ink5}
      maxLength={maxLength}
      multiline={multiline}
      keyboardType={keyboardType ?? 'default'}
      autoCapitalize={keyboardType === 'email-address' ? 'none' : 'sentences'}
      autoCorrect={keyboardType !== 'email-address'}
      autoFocus={autoFocus}
      onSubmitEditing={onSubmit}
      returnKeyType={onSubmit ? 'go' : 'default'}
    />
  );
}

/** Cabecera del registro: flecha atrás, barra de avance y "3 de 11". */
export function CabeceraPaso({
  paso,
  total,
  onAtras,
}: {
  paso: number;
  total: number;
  onAtras: () => void;
}) {
  return (
    <View style={estilos.cabecera}>
      <Pressable style={estilos.redondo} onPress={onAtras}>
        <Text style={estilos.flecha}>←</Text>
      </Pressable>
      <View style={estilos.barra}>
        <View style={[estilos.barraLlena, { width: `${(paso / total) * 100}%` }]} />
      </View>
      <Text style={estilos.pasoTexto}>
        {paso} de {total}
      </Text>
    </View>
  );
}

/** Envoltorio de pantalla del registro: titular, ayuda, contenido y botón abajo. */
export function PantallaPaso({
  titulo,
  ayuda,
  children,
  botonTexto,
  onSiguiente,
  listo,
  ocupado,
  desplazable,
}: {
  titulo: string;
  ayuda?: string;
  children: ReactNode;
  botonTexto?: string;
  onSiguiente: () => void;
  listo: boolean;
  ocupado?: boolean;
  desplazable?: boolean;
}) {
  const contenido = (
    <>
      <Text style={text.titulo}>{titulo}</Text>
      {ayuda && <Text style={[text.ayuda, { marginTop: 8, marginBottom: 14 }]}>{ayuda}</Text>}
      {children}
    </>
  );

  return (
    <View style={estilos.pantallaPaso}>
      {desplazable ? (
        <ScrollView
          style={{ flex: 1 }}
          contentContainerStyle={{ paddingBottom: 16 }}
          keyboardShouldPersistTaps="handled">
          {contenido}
        </ScrollView>
      ) : (
        <View style={{ flex: 1 }}>{contenido}</View>
      )}
      <Boton
        texto={botonTexto ?? 'Continuar'}
        onPress={onSiguiente}
        deshabilitado={!listo}
        ocupado={ocupado}
      />
    </View>
  );
}

const estilos = StyleSheet.create({
  boton: {
    height: 56,
    borderRadius: radios.boton,
    alignItems: 'center',
    justifyContent: 'center',
  },
  botonAccent: { backgroundColor: colors.accent },
  botonOscuro: { backgroundColor: colors.ink },
  botonApagado: { backgroundColor: colors.lineFuerte },
  botonPlano: { height: 46, alignItems: 'center', justifyContent: 'center' },
  botonPlanoTexto: { fontFamily: fonts.sansMedia, fontSize: 15, color: colors.ink2 },

  fila: {
    minHeight: 60,
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderRadius: radios.fila,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  filaNormal: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line },
  filaElegida: { backgroundColor: colors.accentWash, borderWidth: 1.5, borderColor: colors.accent },
  filaTexto: { fontFamily: fonts.sansMedia, fontSize: 16, color: colors.ink },
  filaDetalle: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink3 },

  circulo: { width: 22, height: 22, borderRadius: 999, borderWidth: 1.5, borderColor: '#D8CDBB' },
  circuloElegido: {
    backgroundColor: colors.accent,
    borderColor: colors.accent,
    borderWidth: 4,
  },
  cuadro: { width: 22, height: 22, borderRadius: 7, borderWidth: 1.5, borderColor: '#D8CDBB' },
  cuadroElegido: { backgroundColor: colors.accent, borderColor: colors.accent, borderWidth: 4 },

  pastilla: {
    paddingHorizontal: 15,
    paddingVertical: 10,
    borderRadius: radios.pastilla,
    borderWidth: 1,
  },
  pastillaNormal: { backgroundColor: colors.surface, borderColor: colors.line },
  pastillaOscura: { backgroundColor: colors.ink, borderColor: colors.ink },
  pastillaSuave: { backgroundColor: colors.accentWash, borderColor: colors.accentBorde },
  pastillaTexto: { fontFamily: fonts.sansMedia, fontSize: 14.5, color: colors.ink2 },

  segmento: {
    flex: 1,
    paddingVertical: 15,
    paddingHorizontal: 12,
    borderRadius: radios.campo,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    alignItems: 'center',
  },
  segmentoElegido: { backgroundColor: colors.accentWash, borderWidth: 1.5, borderColor: colors.accent },
  segmentoTexto: {
    fontFamily: fonts.sansMedia,
    fontSize: 14.5,
    color: colors.ink2,
    textAlign: 'center',
  },

  tarjeta: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radios.tarjeta,
    padding: 20,
    gap: 12,
  },
  tarjetaOscura: { backgroundColor: colors.ink, borderColor: colors.ink },

  campo: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radios.campo,
    backgroundColor: colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontFamily: fonts.sans,
    fontSize: 17,
    color: colors.ink,
  },
  campoLargo: { minHeight: 96, textAlignVertical: 'top' },

  cabecera: {
    paddingTop: 58,
    paddingHorizontal: 20,
    paddingBottom: 6,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  redondo: {
    width: 36,
    height: 36,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 17, color: colors.ink2 },
  barra: { flex: 1, height: 4, borderRadius: 999, backgroundColor: colors.lineFuerte, overflow: 'hidden' },
  barraLlena: { height: 4, borderRadius: 999, backgroundColor: colors.accent },
  pasoTexto: { fontFamily: fonts.sansMedia, fontSize: 12, color: colors.ink4 },

  pantallaPaso: {
    flex: 1,
    paddingHorizontal: espacios.pantalla,
    paddingTop: 24,
    paddingBottom: 30,
    gap: 16,
  },
});

export { estilos as estilosComunes };
