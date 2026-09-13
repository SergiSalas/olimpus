import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { fetchToday, type Profile, type Today } from '../api';
import { colors } from '../theme';

/**
 * La pantalla principal: con quién hablas hoy.
 *
 * De la otra persona solo llega lo del nivel 0 (edad, dos intereses y a qué
 * distancia está). El apodo, la bio y la foto no están ni en la respuesta del
 * servidor, así que esta pantalla no podría mostrarlos ni por error.
 */
export function TodayScreen({
  token,
  perfil,
  onAbrirChat,
  onPerfil,
  onSalir,
}: {
  token: string;
  perfil: Profile;
  onAbrirChat: (conversationId: string) => void;
  onPerfil: () => void;
  onSalir: () => void;
}) {
  const [today, setToday] = useState<Today | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    setError(null);
    try {
      setToday(await fetchToday(token));
    } catch {
      setError('No se pudo hablar con el servidor.');
    } finally {
      setCargando(false);
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <ScrollView
      style={estilos.pantalla}
      contentContainerStyle={estilos.contenido}
      refreshControl={<RefreshControl refreshing={cargando} onRefresh={cargar} />}>
      <Text style={estilos.titulo}>Olimpus</Text>
      <Text style={estilos.saludo}>Hola, {perfil.nickname}</Text>

      {cargando && !today && <ActivityIndicator style={{ marginTop: 24 }} />}
      {error && <Text style={estilos.error}>{error}</Text>}

      {today?.hasConversation && today.partner && (
        <View style={estilos.tarjeta}>
          <Text style={estilos.seccion}>Hoy hablas con</Text>
          <Text style={estilos.persona}>
            Alguien de {today.partner.age} años, a {today.partner.approxDistanceKm} km
          </Text>

          <Text style={estilos.ayuda}>
            Todavía no sabes su nombre. Se descubre en cuanto escribáis los dos.
          </Text>

          <View style={estilos.rejilla}>
            {today.partner.interests.map((interes) => (
              <View key={interes} style={estilos.chip}>
                <Text style={estilos.chipTexto}>{interes.replace(/-/g, ' ')}</Text>
              </View>
            ))}
          </View>

          {today.sharedInterests.length > 0 && (
            <Text style={estilos.comun}>
              Los dos habéis puesto {listar(today.sharedInterests)}.
            </Text>
          )}

          <View style={estilos.separador} />
          <Text style={estilos.cierre}>
            La conversación cierra hoy a las {hora(today.closesAt)}.
          </Text>
          <Text style={estilos.ayuda}>
            A las 21:30 os preguntaremos a cada uno, en privado, si queréis seguir.
          </Text>

          <Pressable
            style={estilos.botonPrincipal}
            onPress={() => today.conversationId && onAbrirChat(today.conversationId)}>
            <Text style={estilos.botonPrincipalTexto}>Abrir la conversación</Text>
          </Pressable>
        </View>
      )}

      {today && !today.hasConversation && (
        <View style={estilos.tarjeta}>
          <Text style={estilos.seccion}>Hoy, nadie todavía</Text>
          <Text style={estilos.ayuda}>{today.message}</Text>
          {today.nextRoundAt && (
            <Text style={estilos.cierre}>
              Siguiente reparto: {fechaYHora(today.nextRoundAt)}.
            </Text>
          )}
        </View>
      )}

      <Pressable style={estilos.botonSecundario} onPress={onPerfil}>
        <Text style={estilos.botonSecundarioTexto}>Mi registro</Text>
      </Pressable>
      <Pressable style={estilos.botonSecundario} onPress={onSalir}>
        <Text style={estilos.botonSecundarioTexto}>Cerrar sesión</Text>
      </Pressable>
    </ScrollView>
  );
}

/** "cine y escalada", "cine, escalada y vinos". */
function listar(cosas: string[]): string {
  const limpias = cosas.map((c) => c.replace(/-/g, ' '));
  if (limpias.length === 1) return limpias[0];
  return `${limpias.slice(0, -1).join(', ')} y ${limpias[limpias.length - 1]}`;
}

function hora(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function fechaYHora(iso: string): string {
  return new Date(iso).toLocaleString([], {
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
  });
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
  saludo: { fontSize: 17, color: colors.ink2, textAlign: 'center', marginBottom: 6 },
  tarjeta: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 20,
    gap: 10,
  },
  seccion: { fontSize: 12, fontWeight: '700', color: colors.ink3, textTransform: 'uppercase' },
  persona: { fontSize: 20, fontWeight: '700', color: colors.ink },
  ayuda: { fontSize: 13, color: colors.ink3, lineHeight: 19 },
  comun: { fontSize: 15, color: colors.ink2, fontStyle: 'italic' },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip: { backgroundColor: colors.bg, borderRadius: 14, paddingHorizontal: 10, paddingVertical: 5 },
  chipTexto: { fontSize: 13, color: colors.ink2 },
  separador: { height: 1, backgroundColor: colors.line, marginVertical: 4 },
  cierre: { fontSize: 15, color: colors.ink, fontWeight: '600' },
  botonPrincipal: {
    backgroundColor: colors.accent,
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 6,
  },
  botonPrincipalTexto: { color: colors.surface, fontWeight: '600', fontSize: 16 },
  error: { fontSize: 14, color: colors.error, textAlign: 'center' },
  botonSecundario: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  botonSecundarioTexto: { color: colors.ink2, fontWeight: '600', fontSize: 14 },
});
