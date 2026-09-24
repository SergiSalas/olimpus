import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {
  ApiError,
  askToSeePhoto,
  decide,
  fetchChat,
  likeMessage,
  openChatSocket,
  partnerPhotoSource,
  sendMessage,
  type Chat,
  type ChatMessage,
} from '../api';
import { Burbuja } from '../components/Burbuja';
import { PreguntaFinal } from '../components/Decision';
import { Reportar } from '../components/Reportar';
import { Escalones, LoQueSeVe, NIVELES, PedirFoto } from '../components/Escalera';
import { PanelPruebas } from '../components/PanelPruebas';
import { Presentacion } from '../components/Presentacion';
import { Entrada, Rebote } from '../components';
import { marcarPresentacionVista, presentacionVista } from '../session';
import { colors, fonts } from '../theme';

/**
 * El chat del día. Los mensajes se envían por HTTP y se reciben por la conexión
 * permanente; si esa conexión se cae, la app sigue funcionando (solo dejarían
 * de aparecer solos los del otro).
 *
 * Lo que se sabe de la otra persona está siempre a un toque: la cabecera abre
 * una hoja con su perfil. Y lo que pide una respuesta ("quiero verte", la
 * pregunta del final) se queda fijo encima del campo de escribir.
 */
export function ChatScreen({
  token,
  conversationId,
  yo,
  onVolver,
}: {
  token: string;
  conversationId: string;
  /** Tu apodo, para tu círculo en la presentación. */
  yo: string;
  onVolver: () => void;
}) {
  const [chat, setChat] = useState<Chat | null>(null);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pidiendoFoto, setPidiendoFoto] = useState(false);
  const [respondiendo, setRespondiendo] = useState(false);
  const [reportando, setReportando] = useState(false);
  const [viendoPerfil, setViendoPerfil] = useState(false);
  const [presentando, setPresentando] = useState(false);
  /** Salta a verdadero un momento cuando sube el nivel: la cabecera da un brinco. */
  const [subio, setSubio] = useState(false);
  const nivelAnterior = useRef<number | null>(null);
  const lista = useRef<FlatList<Fila>>(null);

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

  // La presentación sale la primera vez que se abre una conversación del día.
  const abierta = chat?.state === 'OPEN';
  useEffect(() => {
    if (!abierta) return;
    presentacionVista(conversationId).then((vista) => {
      if (vista) return;
      setPresentando(true);
      marcarPresentacionVista(conversationId);
    });
  }, [abierta, conversationId]);

  const nivel = chat?.partner.level ?? null;
  useEffect(() => {
    if (nivel === null) return;
    if (nivelAnterior.current !== null && nivel > nivelAnterior.current) {
      setSubio(true);
      const t = setTimeout(() => setSubio(false), 700);
      nivelAnterior.current = nivel;
      return () => clearTimeout(t);
    }
    nivelAnterior.current = nivel;
  }, [nivel]);

  // Lo que escribe el otro llega por aquí.
  useEffect(() => {
    const socket = openChatSocket(
      token,
      (mensaje) => {
        if (mensaje.conversationId !== conversationId || mensaje.mine) return;

        setChat((actual) => {
          if (!actual) return actual;
          // Si ese mensaje ha abierto un nivel, hay cosas nuevas que enseñar (su
          // apodo, sus preguntas) y el aviso que lo cuenta: se le pide todo al servidor.
          if (mensaje.level > actual.partner.level) {
            cargar();
          }
          return { ...actual, messages: [...actual.messages, mensaje] };
        });
      },
      (corazon) => {
        if (corazon.conversationId !== conversationId) return;
        marcarCorazon(corazon.messageId, corazon.liked);
      },
    );
    return () => socket.close();
  }, [token, conversationId, cargar]);

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

  function marcarCorazon(messageId: string, liked: boolean) {
    setChat((actual) =>
      actual
        ? {
            ...actual,
            messages: actual.messages.map((m) => (m.id === messageId ? { ...m, liked } : m)),
          }
        : actual,
    );
  }

  /** Se pinta al momento y se deshace si el servidor dice que no. */
  async function darCorazon(mensaje: ChatMessage) {
    const liked = !mensaje.liked;
    marcarCorazon(mensaje.id, liked);
    try {
      await likeMessage(token, conversationId, mensaje.id, liked);
    } catch (e) {
      marcarCorazon(mensaje.id, !liked);
      setError(e instanceof ApiError ? e.message : 'No se pudo dar el corazón.');
    }
  }

  async function quieroVerte() {
    setPidiendoFoto(true);
    setError(null);
    try {
      const respuesta = await askToSeePhoto(token, conversationId);
      await cargar();
      // Si los dos lo habéis pedido, la foto está lista: se abre su perfil.
      if (respuesta.bothAccepted) setViendoPerfil(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo pedir.');
    } finally {
      setPidiendoFoto(false);
    }
  }

  async function responder(respuesta: 'YES' | 'NO') {
    setRespondiendo(true);
    setError(null);
    try {
      await decide(token, conversationId, respuesta);
      await cargar();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo responder.');
    } finally {
      setRespondiendo(false);
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
  const { partner } = chat;
  const pedirFoto = !cerrada && chat.canAskForPhoto && !partner.photoAvailable;
  const puedeEnviar = texto.trim().length > 0 && !enviando;

  return (
    <KeyboardAvoidingView
      style={estilos.pantalla}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={estilos.cabecera}>
        <Rebote style={estilos.redondo} onPress={onVolver}>
          <Text style={estilos.flecha}>←</Text>
        </Rebote>

        <Rebote
          fuera={{ flex: 1 }}
          style={estilos.quien}
          pop={subio}
          onPress={() => setViendoPerfil(true)}>
          {partner.photoAvailable ? (
            <Image source={partnerPhotoSource(token, conversationId)} style={estilos.avatar} />
          ) : (
            <View style={[estilos.avatar, estilos.avatarVacio]}>
              <Text style={estilos.interrogante}>?</Text>
            </View>
          )}
          <View style={{ flexShrink: 1, gap: 3 }}>
            <Text style={estilos.nombre} numberOfLines={1}>
              {partner.nickname ? `${partner.nickname}, ${partner.age}` : `${partner.age} años`}
            </Text>
            <View style={estilos.filaNivel}>
              <Escalones nivel={partner.level} />
              <Text style={estilos.nivel}>{NIVELES[partner.level]?.titulo}</Text>
            </View>
            <Text style={estilos.verPerfil}>Ver perfil ›</Text>
          </View>
        </Rebote>

        <View style={estilos.derecha}>
          <View style={[estilos.pastillaCierre, chat.connected && estilos.pastillaConexion]}>
            <Text style={estilos.pastillaCierreTexto}>
              {chat.connected ? '💖 Conexión' : `⏳ ${hora(chat.closesAt)}`}
            </Text>
          </View>
          <Pressable onPress={() => setReportando(true)} hitSlop={10}>
            <Text style={estilos.reportar}>Reportar</Text>
          </Pressable>
        </View>
      </View>

      <Reportar
        visible={reportando}
        token={token}
        conversationId={conversationId}
        onCerrar={() => setReportando(false)}
        onHecho={() => {
          setReportando(false);
          onVolver();
        }}
      />

      <Modal
        visible={viendoPerfil}
        transparent
        animationType="slide"
        onRequestClose={() => setViendoPerfil(false)}>
        <View style={estilos.veloHoja}>
          <Pressable style={{ flex: 1 }} onPress={() => setViendoPerfil(false)} />
          <View style={estilos.hoja}>
            <View style={estilos.asa} />
            <ScrollView contentContainerStyle={{ paddingBottom: 30 }}>
              <LoQueSeVe chat={chat} token={token} />
            </ScrollView>
          </View>
        </View>
      </Modal>

      <FlatList
        ref={lista}
        data={conAvisos(chat)}
        keyExtractor={(item) => item.clave}
        contentContainerStyle={estilos.mensajes}
        onContentSizeChange={() => lista.current?.scrollToEnd({ animated: true })}
        ListHeaderComponent={
          <View style={estilos.arranque}>
            <Text style={estilos.arranqueEmoji}>💡</Text>
            <View style={{ flexShrink: 1, gap: 4 }}>
              <Text style={estilos.arranqueEtiqueta}>Para romper el hielo</Text>
              <Text style={estilos.arranqueTexto}>{chat.icebreaker}</Text>
              <Text style={estilos.pistaCorazon}>Toca dos veces un mensaje suyo para darle ❤️</Text>
            </View>
          </View>
        }
        renderItem={({ item }) =>
          item.tipo === 'aviso' ? (
            <Entrada style={{ alignItems: 'center', paddingVertical: 6 }}>
              <Rebote style={estilos.desbloqueo} onPress={() => setViendoPerfil(true)}>
                <Text style={estilos.desbloqueoTexto}>🔓 {item.texto}</Text>
                <Text style={estilos.desbloqueoVer}>Ver perfil ›</Text>
              </Rebote>
            </Entrada>
          ) : (
            <Burbuja
              mensaje={item.mensaje}
              hora={hora(item.mensaje.sentAt)}
              onCorazon={item.mensaje.mine || cerrada ? undefined : () => darCorazon(item.mensaje)}
            />
          )
        }
      />

      {error && <Text style={estilos.error}>{error}</Text>}

      {!cerrada && (chat.decisionTime || pedirFoto) && (
        <Entrada style={estilos.fijos}>
          {chat.decisionTime && (
            <PreguntaFinal
              respondido={chat.yourDecision}
              ocupado={respondiendo}
              onResponder={responder}
            />
          )}
          {pedirFoto && (
            <PedirFoto chat={chat} pidiendo={pidiendoFoto} onQuieroVerte={quieroVerte} />
          )}
        </Entrada>
      )}

      {cerrada ? (
        <View style={estilos.barra}>
          <Text style={estilos.cerradoTexto}>
            {chat.state === 'BLOCKED'
              ? 'Esta conversación está cortada. No os volveremos a emparejar.'
              : 'Esta conversación se cerró a las 22:00. Mañana a las 4:00 hay reparto nuevo.'}
          </Text>
        </View>
      ) : (
        <View style={estilos.barra}>
          <TextInput
            style={estilos.campo}
            value={texto}
            onChangeText={setTexto}
            placeholder="Escribe algo…"
            placeholderTextColor={colors.ink5}
            multiline
            maxLength={1000}
          />
          <Rebote
            style={[estilos.enviar, !puedeEnviar && estilos.apagado]}
            pop={puedeEnviar}
            onPress={enviar}
            disabled={!puedeEnviar}>
            {enviando ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text style={estilos.enviarTexto}>➤</Text>
            )}
          </Rebote>
        </View>
      )}

      <PanelPruebas token={token} onHecho={cargar} />

      {presentando && (
        <Presentacion
          inicial={(yo.charAt(0) || '?').toUpperCase()}
          detalle={`${partner.age} años · a ${partner.approxDistanceKm} km`}
          onFin={() => setPresentando(false)}
        />
      )}
    </KeyboardAvoidingView>
  );
}

/** Lo que se ve en el hilo: mensajes y, entre ellos, los avisos de desbloqueo. */
type Fila =
  | { clave: string; tipo: 'mensaje'; mensaje: ChatMessage }
  | { clave: string; tipo: 'aviso'; texto: string };

/**
 * Mezcla los dos. El servidor dice después de qué mensaje va cada aviso, así
 * que aquí no se decide nada: se colocan donde toca y siguen ahí al recargar.
 */
function conAvisos(chat: Chat): Fila[] {
  const filas: Fila[] = [];

  for (const mensaje of chat.messages) {
    filas.push({ clave: mensaje.id, tipo: 'mensaje', mensaje });

    for (const unlock of chat.unlocks) {
      if (unlock.afterMessageId === mensaje.id) {
        filas.push({ clave: `nivel-${unlock.level}`, tipo: 'aviso', texto: unlock.text });
      }
    }
  }

  // Los que no cuelgan de ningún mensaje (los abrió el tiempo, o el "quiero
  // verte" de los dos) van al final, que es cuando ocurrieron.
  for (const unlock of chat.unlocks) {
    if (!unlock.afterMessageId) {
      filas.push({ clave: `nivel-${unlock.level}`, tipo: 'aviso', texto: unlock.text });
    }
  }

  return filas;
}

function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },
  centrado: {
    flex: 1,
    backgroundColor: colors.bg,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 18,
  },

  cabecera: {
    paddingTop: 54,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: colors.surface,
    borderBottomLeftRadius: 28,
    borderBottomRightRadius: 28,
    borderBottomWidth: 4,
    borderColor: colors.line,
    shadowColor: colors.uva,
    shadowOpacity: 0.12,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 6 },
    elevation: 4,
    zIndex: 1,
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
  quien: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 999,
    borderWidth: 3,
    borderColor: colors.sol,
    backgroundColor: colors.surface2,
  },
  avatarVacio: { backgroundColor: colors.sol, alignItems: 'center', justifyContent: 'center' },
  interrogante: { fontFamily: fonts.displayFuerte, fontSize: 22, color: colors.ink },
  nombre: { fontFamily: fonts.display, fontSize: 18, color: colors.ink },
  filaNivel: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  nivel: { fontFamily: fonts.sansMedia, fontSize: 11.5, color: colors.ink3 },
  verPerfil: { fontFamily: fonts.sansNegrita, fontSize: 11.5, color: colors.uva },
  derecha: { alignItems: 'flex-end', gap: 6 },
  pastillaCierre: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: colors.ink,
  },
  pastillaConexion: { backgroundColor: colors.accent },
  pastillaCierreTexto: { fontFamily: fonts.sansNegrita, fontSize: 11.5, color: colors.onInk },
  reportar: { fontFamily: fonts.sansMedia, fontSize: 11.5, color: colors.ink4 },

  veloHoja: { flex: 1, backgroundColor: colors.velo },
  hoja: {
    maxHeight: '85%',
    backgroundColor: colors.bg,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    paddingHorizontal: 22,
    paddingTop: 12,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.trazo,
    marginBottom: 16,
  },

  mensajes: { padding: 18, gap: 10 },
  arranque: {
    flexDirection: 'row',
    gap: 12,
    backgroundColor: '#FFF6D9',
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.sol,
    borderRadius: 22,
    padding: 16,
    marginBottom: 10,
  },
  arranqueEmoji: { fontSize: 24 },
  arranqueEtiqueta: {
    fontFamily: fonts.sansNegrita,
    fontSize: 11,
    letterSpacing: 1.2,
    textTransform: 'uppercase',
    color: '#A87A00',
  },
  pistaCorazon: { fontFamily: fonts.sansMedia, fontSize: 12, color: '#A87A00', marginTop: 4 },
  arranqueTexto: { fontFamily: fonts.sansMedia, fontSize: 15, lineHeight: 22, color: colors.ink },

  desbloqueo: {
    backgroundColor: colors.uva,
    borderRadius: 999,
    borderBottomWidth: 3,
    borderColor: '#6A3FD1',
    paddingHorizontal: 16,
    paddingVertical: 9,
    alignItems: 'center',
    gap: 2,
  },
  desbloqueoTexto: {
    fontFamily: fonts.sansNegrita,
    fontSize: 13,
    color: '#FFFFFF',
    textAlign: 'center',
  },
  desbloqueoVer: { fontFamily: fonts.sansMedia, fontSize: 11.5, color: colors.onInk2 },

  fijos: { paddingHorizontal: 16, paddingTop: 8, gap: 8 },
  barra: {
    flexDirection: 'row',
    gap: 10,
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 28,
    backgroundColor: colors.bg,
    alignItems: 'flex-end',
  },
  campo: {
    flex: 1,
    borderWidth: 2,
    borderColor: colors.line,
    borderRadius: 24,
    backgroundColor: colors.surface,
    paddingHorizontal: 18,
    paddingTop: 13,
    paddingBottom: 13,
    fontFamily: fonts.sans,
    fontSize: 16,
    maxHeight: 120,
    color: colors.ink,
  },
  enviar: {
    width: 50,
    height: 50,
    borderRadius: 999,
    backgroundColor: colors.accent,
    borderBottomWidth: 4,
    borderColor: colors.accentOscuro,
    alignItems: 'center',
    justifyContent: 'center',
  },
  apagado: { opacity: 0.4 },
  enviarTexto: { fontSize: 20, color: '#FFFFFF', marginLeft: 3 },
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
