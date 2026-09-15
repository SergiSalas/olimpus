import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {
  ApiError,
  askToSeePhoto,
  fetchChat,
  openChatSocket,
  sendMessage,
  type Chat,
  type ChatMessage,
} from '../api';
import { Escalones, LoQueSeVe } from '../components/Escalera';
import { Etiqueta } from '../components';
import { colors, fonts, radios } from '../theme';

/**
 * El chat del día. Los mensajes se envían por HTTP y se reciben por la conexión
 * permanente; si esa conexión se cae, la app sigue funcionando (solo dejarían
 * de aparecer solos los del otro).
 */
export function ChatScreen({
  token,
  conversationId,
  onVolver,
}: {
  token: string;
  conversationId: string;
  onVolver: () => void;
}) {
  const [chat, setChat] = useState<Chat | null>(null);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pidiendoFoto, setPidiendoFoto] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const lista = useRef<FlatList<ChatMessage>>(null);

  const cargar = useCallback(async () => {
    try {
      setChat(await fetchChat(token, conversationId));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo abrir la conversación.');
    }
  }, [token, conversationId]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // Lo que escribe el otro llega por aquí.
  useEffect(() => {
    const socket = openChatSocket(token, (mensaje) => {
      if (mensaje.conversationId !== conversationId || mensaje.mine) return;
      setChat((actual) =>
        actual ? { ...actual, messages: [...actual.messages, mensaje] } : actual,
      );
    });
    return () => socket.close();
  }, [token, conversationId]);

  async function enviar() {
    const limpio = texto.trim();
    if (!limpio || enviando) return;

    setEnviando(true);
    setError(null);
    try {
      const mensaje = await sendMessage(token, conversationId, limpio);
      setTexto('');
      setChat((actual) =>
        actual ? { ...actual, messages: [...actual.messages, mensaje] } : actual,
      );
      // Ese mensaje puede haber abierto un nivel: quien lo decide es el
      // servidor, así que se le vuelve a preguntar en vez de adivinarlo aquí.
      cargar();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo enviar.');
    } finally {
      setEnviando(false);
    }
  }

  async function quieroVerte() {
    setPidiendoFoto(true);
    setError(null);
    try {
      const respuesta = await askToSeePhoto(token, conversationId);
      setAviso(
        respuesta.bothAccepted
          ? 'Los dos habéis dicho que sí. Ya podéis veros.'
          : 'Anotado. Si la otra persona también lo pide, aparecerá la foto.',
      );
      await cargar();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo pedir.');
    } finally {
      setPidiendoFoto(false);
    }
  }

  if (!chat) {
    return (
      <View style={estilos.centrado}>
        {error ? (
          <Text style={estilos.error}>{error}</Text>
        ) : (
          <ActivityIndicator color={colors.accent} />
        )}
        <Pressable onPress={onVolver}>
          <Text style={estilos.volver}>‹ Volver</Text>
        </Pressable>
      </View>
    );
  }

  const cerrada = chat.state !== 'OPEN';

  return (
    <KeyboardAvoidingView
      style={estilos.pantalla}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={estilos.cabecera}>
        <Pressable style={estilos.redondo} onPress={onVolver}>
          <Text style={estilos.flecha}>←</Text>
        </Pressable>
        <View style={estilos.circuloFoto}>
          <Text style={estilos.interrogante}>?</Text>
        </View>
        <View style={{ flexShrink: 1, gap: 2 }}>
          <Text style={estilos.nombre}>
            {chat.bothHaveWritten ? 'Conversación en marcha' : 'Alguien nuevo'}
          </Text>
          <Text style={estilos.nivel}>
            {chat.partner.nickname
              ? `${chat.partner.nickname}, ${chat.partner.age}`
              : `${chat.partner.age} años`}
          </Text>
          <Escalones nivel={chat.partner.level} />
        </View>
        <View style={estilos.pastillaCierre}>
          <Text style={estilos.pastillaCierreTexto}>Cierra {hora(chat.closesAt)}</Text>
        </View>
      </View>

      <FlatList
        ref={lista}
        data={chat.messages}
        keyExtractor={(m) => m.id}
        contentContainerStyle={estilos.mensajes}
        onContentSizeChange={() => lista.current?.scrollToEnd({ animated: true })}
        ListHeaderComponent={
          <View style={{ gap: 12, marginBottom: 10 }}>
            <LoQueSeVe
              chat={chat}
              token={token}
              pidiendo={pidiendoFoto}
              onQuieroVerte={quieroVerte}
            />
            {aviso && (
              <View style={estilos.aviso}>
                <Text style={estilos.avisoTexto}>{aviso}</Text>
              </View>
            )}
            <View style={estilos.arranque}>
              <Etiqueta tono="accent">Para empezar</Etiqueta>
              <Text style={estilos.arranqueTexto}>{chat.icebreaker}</Text>
            </View>
          </View>
        }
        renderItem={({ item }) => (
          <View style={[estilos.burbuja, item.mine ? estilos.mia : estilos.suya]}>
            <Text style={item.mine ? estilos.textoMio : estilos.textoSuyo}>{item.text}</Text>
            <Text style={item.mine ? estilos.horaMia : estilos.horaSuya}>{hora(item.sentAt)}</Text>
          </View>
        )}
      />

      {error && <Text style={estilos.error}>{error}</Text>}

      {cerrada ? (
        <View style={estilos.barra}>
          <Text style={estilos.cerradoTexto}>
            Esta conversación está cerrada. Mañana a las 4:00 hay reparto nuevo.
          </Text>
        </View>
      ) : (
        <View style={estilos.barra}>
          <TextInput
            style={estilos.campo}
            value={texto}
            onChangeText={setTexto}
            placeholder="Escribe algo"
            placeholderTextColor={colors.ink5}
            multiline
            maxLength={1000}
          />
          <Pressable
            style={[estilos.enviar, (!texto.trim() || enviando) && estilos.apagado]}
            onPress={enviar}
            disabled={!texto.trim() || enviando}>
            {enviando ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text style={estilos.enviarTexto}>Enviar</Text>
            )}
          </Pressable>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center', gap: 18 },

  cabecera: {
    paddingTop: 54,
    paddingHorizontal: 20,
    paddingBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.bg,
    borderBottomWidth: 1,
    borderBottomColor: '#EAE0D0',
  },
  redondo: {
    width: 34,
    height: 34,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 16, color: colors.ink2 },
  circuloFoto: {
    width: 38,
    height: 38,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  interrogante: { fontFamily: fonts.sansNegrita, fontSize: 12, color: colors.ink5 },
  nombre: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.ink },
  nivel: { fontFamily: fonts.sans, fontSize: 12.5, color: colors.ink3 },
  pastillaCierre: {
    marginLeft: 'auto',
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 999,
    backgroundColor: colors.ink,
  },
  pastillaCierreTexto: { fontFamily: fonts.sansNegrita, fontSize: 11.5, color: colors.onInk },

  mensajes: { padding: 20, gap: 10 },
  arranque: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderLeftWidth: 3,
    borderLeftColor: colors.accent,
    borderRadius: 16,
    padding: 16,
    marginBottom: 10,
    gap: 6,
  },
  aviso: {
    backgroundColor: colors.accentWash,
    borderWidth: 1,
    borderColor: colors.accentBorde,
    borderRadius: 14,
    padding: 14,
  },
  avisoTexto: { fontFamily: fonts.sansMedia, fontSize: 13.5, lineHeight: 19, color: colors.ink },
  arranqueTexto: { fontFamily: fonts.sans, fontSize: 15, lineHeight: 22, color: colors.ink },

  burbuja: { maxWidth: '82%', borderRadius: 20, paddingHorizontal: 18, paddingVertical: 14 },
  mia: { alignSelf: 'flex-end', backgroundColor: colors.ink, borderBottomRightRadius: 6 },
  suya: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderBottomLeftRadius: 6,
  },
  textoMio: { fontFamily: fonts.sans, fontSize: 16, lineHeight: 23, color: colors.onInk },
  textoSuyo: { fontFamily: fonts.sans, fontSize: 16, lineHeight: 23, color: colors.ink },
  horaMia: { fontFamily: fonts.sans, fontSize: 10.5, color: colors.onInk2, marginTop: 4, textAlign: 'right' },
  horaSuya: { fontFamily: fonts.sans, fontSize: 10.5, color: colors.ink5, marginTop: 4 },

  barra: {
    flexDirection: 'row',
    gap: 9,
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 26,
    borderTopWidth: 1,
    borderTopColor: '#EAE0D0',
    backgroundColor: colors.bg,
    alignItems: 'flex-end',
  },
  campo: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 999,
    backgroundColor: colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontFamily: fonts.sans,
    fontSize: 16,
    maxHeight: 120,
    color: colors.ink,
  },
  enviar: {
    backgroundColor: colors.accent,
    borderRadius: 999,
    paddingHorizontal: 20,
    paddingVertical: 13,
  },
  apagado: { opacity: 0.4 },
  enviarTexto: { fontFamily: fonts.sansNegrita, fontSize: 15, color: '#FFFFFF' },
  cerradoTexto: {
    flex: 1,
    fontFamily: fonts.sans,
    fontSize: 13,
    color: colors.ink3,
    textAlign: 'center',
  },
  volver: { fontFamily: fonts.sansMedia, fontSize: 15, color: colors.accent },
  error: {
    fontFamily: fonts.sansMedia,
    fontSize: 13,
    color: colors.error,
    textAlign: 'center',
    paddingHorizontal: 20,
  },
});
