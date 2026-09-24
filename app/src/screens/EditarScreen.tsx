import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, type Profile } from '../api';
import { Boton, Entrada, Etiqueta, Flotar, Rebote } from '../components';
import { FotoFallida, borradorDe, completo, guardar, type Borrador } from '../borrador';
import {
  Apodo,
  Busco,
  ComoHablas,
  EdadYDistancia,
  Foto,
  Genero,
  Idiomas,
  Intencion,
  Intereses,
  Nacimiento,
  Preguntas,
  TrabajoYSitio,
  Ubicacion,
} from '../preguntas';
import { colors, espacios, fonts, text } from '../theme';

/**
 * Cambiar lo ya respondido, todo en una pantalla.
 *
 * Antes esto reabría el registro entero desde cero, foto incluida. Quien vuelve
 * aquí casi siempre viene a tocar una cosa —la distancia, una respuesta— y
 * hacerle pasar otra vez por seis bloques para eso es el motivo por el que nadie
 * mantiene su perfil al día.
 */
export function EditarScreen({
  token,
  perfil,
  onVolver,
  onGuardado,
}: {
  token: string;
  perfil: Profile;
  onVolver: () => void;
  onGuardado: (perfil: Profile) => void;
}) {
  const [b, setB] = useState<Borrador>(() => borradorDe(perfil));
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cambiar = (cambio: Partial<Borrador>) => setB((actual) => ({ ...actual, ...cambio }));
  const props = { b, cambiar };

  async function aplicar() {
    setOcupado(true);
    setError(null);
    try {
      onGuardado(await guardar(token, b));
    } catch (e) {
      if (e instanceof FotoFallida) {
        setError(`Tus cambios están guardados, pero la foto no subió: ${e.message}`);
      } else {
        setError(e instanceof ApiError ? e.message : 'No se pudieron guardar los cambios.');
      }
    } finally {
      setOcupado(false);
    }
  }

  return (
    <View style={estilos.pantalla}>
      <View style={estilos.cabecera}>
        <Rebote style={estilos.redondo} onPress={onVolver}>
          <Text style={estilos.flecha}>←</Text>
        </Rebote>
        <Text style={estilos.cabeceraTexto}>Cambiar mis respuestas</Text>
      </View>

      <View style={estilos.cuerpo}>
        <ScrollView
          style={{ flex: 1 }}
          contentContainerStyle={{ paddingBottom: 24 }}
          keyboardShouldPersistTaps="handled"
          automaticallyAdjustKeyboardInsets>
          <Text style={[text.ayuda, { marginBottom: 22 }]}>
            Toca solo lo que quieras cambiar. Se guarda todo junto al darle abajo.
          </Text>

          <Seccion nombre="Quién eres" emoji="👋" orden={0}>
            <Apodo {...props} />
            <Nacimiento {...props} />
            <Genero {...props} />
          </Seccion>

          <Seccion nombre="A quién buscas" emoji="🔍" orden={1}>
            <Busco {...props} />
            <EdadYDistancia {...props} />
          </Seccion>

          <Seccion nombre="Dónde y en qué idioma" emoji="🌍" orden={2}>
            <Ubicacion {...props} />
            <Idiomas {...props} />
          </Seccion>

          <Seccion nombre="Cómo hablas" emoji="🗣️" orden={3}>
            <ComoHablas {...props} />
            <Intencion {...props} />
          </Seccion>

          <Seccion nombre="Qué cuentas de ti" emoji="💫" orden={4}>
            <Intereses {...props} />
            <Preguntas {...props} />
            <TrabajoYSitio {...props} />
          </Seccion>

          <Seccion nombre="Tu foto" emoji="📸" orden={5}>
            <Foto {...props} token={token} yaHayUna />
          </Seccion>
        </ScrollView>

        <Boton
          texto="Guardar cambios"
          onPress={aplicar}
          deshabilitado={!completo(b)}
          ocupado={ocupado}
        />
      </View>

      {error && (
        <Entrada key={error}>
          <Text style={estilos.error}>{error}</Text>
        </Entrada>
      )}
    </View>
  );
}

/** Cada sección entra un poco después que la anterior, según su `orden`. */
function Seccion({
  nombre,
  emoji,
  orden,
  children,
}: {
  nombre: string;
  emoji: string;
  orden: number;
  children: React.ReactNode;
}) {
  return (
    <Entrada retraso={100 + orden * 110} style={estilos.seccion}>
      <View style={estilos.encabezado}>
        <Flotar distancia={6} duracion={2400 + orden * 300}>
          <Text style={estilos.emoji}>{emoji}</Text>
        </Flotar>
        <Etiqueta tono="accent">{nombre}</Etiqueta>
      </View>
      {children}
    </Entrada>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  cabecera: {
    paddingTop: 58,
    paddingHorizontal: 20,
    paddingBottom: 6,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  redondo: {
    width: 40,
    height: 40,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 18, color: colors.uva },
  cabeceraTexto: { fontFamily: fonts.display, fontSize: 18, color: colors.ink },

  cuerpo: {
    flex: 1,
    paddingHorizontal: espacios.pantalla,
    paddingTop: 20,
    paddingBottom: 30,
    gap: 14,
  },
  seccion: {
    borderTopWidth: 2,
    borderTopColor: colors.line,
    paddingTop: 26,
    marginBottom: 8,
  },
  encabezado: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 18 },
  emoji: { fontSize: 28 },
  error: {
    ...text.ayuda,
    color: colors.error,
    textAlign: 'center',
    paddingHorizontal: 26,
    paddingBottom: 12,
  },
});
