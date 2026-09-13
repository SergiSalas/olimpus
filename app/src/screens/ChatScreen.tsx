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
  fetchChat,
  openChatSocket,
  sendMessage,
  type Chat,
  type ChatMessage,
} from '../api';
import { colors } from '../theme';

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
      setChat((actual) => (actual ? { ...actual, messages: [...actual.messages, mensaje] } : actual));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo enviar.');
    } finally {
      setEnviando(false);
    }
  }

  if (!chat) {
    return (
      <View style={estilos.centrado}>
        {error ? <Text style={estilos.error}>{error}</Text> : <ActivityIndicator />}
        <Pressable onPress={onVolver}>
          <Text style={estilos.volver}>‹ Volver</Text>
        </Pressable>
      </View>
    );
  }

  const cerrada = chat.state !== 'ABIERTA';

  return (
    <KeyboardAvoidingView
      style={estilos.pantalla}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={0}>
      <View style={estilos.cabecera}>
        <Pressable onPress={onVolver}>
          <Text style={estilos.volver}>‹ Hoy</Text>
        </Pressable>
        <Text style={estilos.tituloCabecera}>
          {chat.bothHaveWritten ? 'Conversación en marcha' : 'Alguien nuevo'} ·{' '}
          {chat.partner.age} años
        </Text>
        <Text style={estilos.subCabecera}>Cierra a las {hora(chat.closesAt)}</Text>
      </View>

      <FlatList
        ref={lista}
        data={chat.messages}
        keyExtractor={(m) => m.id}
        contentContainerStyle={estilos.mensajes}
        onContentSizeChange={() => lista.current?.scrollToEnd({ animated: true })}
        ListHeaderComponent={
          <View style={estilos.arranque}>
            <Text style={estilos.arranqueEtiqueta}>Para empezar</Text>
            <Text style={estilos.arranqueTexto}>{chat.icebreaker}</Text>
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
        <View style={estilos.cerrado}>
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
            placeholderTextColor={colors.ink3}
            multiline
            maxLength={1000}
          />
          <Pressable
            style={[estilos.enviar, (!texto.trim() || enviando) && estilos.apagado]}
            onPress={enviar}
            disabled={!texto.trim() || enviando}>
            {enviando ? (
              <ActivityIndicator color={colors.surface} />
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
  centrado: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center', gap: 16 },
  cabecera: {
    paddingTop: 56,
    paddingHorizontal: 20,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    backgroundColor: colors.surface,
    gap: 2,
  },
  volver: { fontSize: 16, color: colors.accent },
  tituloCabecera: { fontSize: 17, fontWeight: '700', color: colors.ink },
  subCabecera: { fontSize: 12, color: colors.ink3 },
  mensajes: { padding: 16, gap: 8 },
  arranque: {
    backgroundColor: colors.surface,
    borderLeftWidth: 3,
    borderLeftColor: colors.accent,
    borderRadius: 6,
    padding: 14,
    marginBottom: 8,
    gap: 4,
  },
  arranqueEtiqueta: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.ink3,
    textTransform: 'uppercase',
  },
  arranqueTexto: { fontSize: 15, color: colors.ink, lineHeight: 21 },
  burbuja: { maxWidth: '82%', borderRadius: 14, paddingHorizontal: 14, paddingVertical: 9 },
  mia: { alignSelf: 'flex-end', backgroundColor: colors.accent, borderBottomRightRadius: 4 },
  suya: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderBottomLeftRadius: 4,
  },
  textoMio: { color: colors.surface, fontSize: 15, lineHeight: 20 },
  textoSuyo: { color: colors.ink, fontSize: 15, lineHeight: 20 },
  horaMia: { color: '#CFE3EC', fontSize: 10, marginTop: 3, textAlign: 'right' },
  horaSuya: { color: colors.ink3, fontSize: 10, marginTop: 3 },
  barra: {
    flexDirection: 'row',
    gap: 8,
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: colors.line,
    backgroundColor: colors.surface,
    alignItems: 'flex-end',
  },
  campo: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingTop: 10,
    paddingBottom: 10,
    fontSize: 16,
    maxHeight: 120,
    color: colors.ink,
    backgroundColor: colors.bg,
  },
  enviar: {
    backgroundColor: colors.accent,
    borderRadius: 20,
    paddingHorizontal: 18,
    paddingVertical: 11,
  },
  apagado: { opacity: 0.4 },
  enviarTexto: { color: colors.surface, fontWeight: '600' },
  cerrado: { padding: 16, backgroundColor: colors.surface, borderTopWidth: 1, borderTopColor: colors.line },
  cerradoTexto: { fontSize: 13, color: colors.ink3, textAlign: 'center' },
  error: { color: colors.error, fontSize: 13, textAlign: 'center', paddingHorizontal: 16 },
});
