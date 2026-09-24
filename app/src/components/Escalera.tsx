import { Image, StyleSheet, Text, View } from 'react-native';
import { partnerPhotoSource, type Chat } from '../api';
import { Boton, Etiqueta, Pastilla, Tarjeta } from '../components';
import { useNombreInteres } from '../interests';
import { nombreDeIdioma } from '../preguntas';
import { colors, fonts } from '../theme';

export const NIVELES = [
  { n: 0, titulo: 'Match', desc: 'Edad, dos intereses, zona' },
  { n: 1, titulo: 'Primer mensaje', desc: 'Apodo y todos los intereses' },
  { n: 2, titulo: 'Conversación', desc: 'Sus tres preguntas' },
  { n: 3, titulo: 'Buena conexión', desc: 'Foto, si los dos aceptáis' },
  { n: 4, titulo: 'Confianza', desc: 'Lo que cada uno quiera' },
];

/** Un color de juego por nivel, del primero al último. */
export const COLOR_NIVEL = [colors.accent, colors.sol, colors.menta, colors.cielo, colors.uva];

/** Los cinco escalones, cada uno de su color, encendidos hasta el nivel actual. */
export function Escalones({ nivel }: { nivel: number }) {
  return (
    <View style={estilos.escalones}>
      {NIVELES.map((n) => (
        <View
          key={n.n}
          style={[
            estilos.escalon,
            n.n <= nivel && { backgroundColor: COLOR_NIVEL[n.n] },
            n.n === nivel && estilos.escalonActual,
          ]}
        />
      ))}
    </View>
  );
}

/**
 * Lo que se ha destapado de la otra persona. Vive en la hoja que se abre desde
 * la cabecera del chat, para no tener que subir hasta arriba a buscarlo.
 *
 * Cada trozo aparece cuando su nivel llega. Lo que no está aquí es porque el
 * servidor no lo ha mandado: no hay nada escondido en la pantalla.
 */
export function LoQueSeVe({ chat, token }: { chat: Chat; token: string }) {
  const nombreInteres = useNombreInteres();
  const { partner } = chat;
  const siguiente = NIVELES[partner.level + 1];

  return (
    <View style={{ gap: 14 }}>
      <View style={estilos.cabecera}>
        {partner.photoAvailable ? (
          <Image source={partnerPhotoSource(token, chat.conversationId)} style={estilos.foto} />
        ) : (
          <View style={estilos.marcoVacio}>
            <Text style={estilos.interrogante}>?</Text>
          </View>
        )}
        <Text style={estilos.nombre}>
          {partner.nickname ? `${partner.nickname}, ${partner.age}` : `${partner.age} años`}
        </Text>
        <Text style={estilos.pista}>a unos {partner.approxDistanceKm} km</Text>
        {partner.occupation ? <Text style={estilos.respuesta}>{partner.occupation}</Text> : null}
      </View>

      <Tarjeta>
        <View style={estilos.filaNivel}>
          <Etiqueta tono="accent">
            {`Nivel ${partner.level} · ${NIVELES[partner.level]?.titulo ?? ''}`}
          </Etiqueta>
          <Escalones nivel={partner.level} />
        </View>
        {siguiente && (
          <Text style={estilos.pista}>
            {`Siguiente: ${siguiente.titulo.toLowerCase()} · ${siguiente.desc.toLowerCase()}`}
          </Text>
        )}
      </Tarjeta>

      <Tarjeta>
        <Etiqueta>Sus intereses</Etiqueta>
        {chat.sharedInterests.length > 0 && (
          <Text style={estilos.pista}>En morado, los que tenéis en común</Text>
        )}
        <View style={estilos.rejilla}>
          {partner.interests.map((interes) => (
            <Pastilla
              key={interes}
              texto={nombreInteres(interes)}
              elegida
              tono={chat.sharedInterests.includes(interes) ? 'oscuro' : 'suave'}
            />
          ))}
        </View>
      </Tarjeta>

      {partner.prompts.length > 0 ? (
        <Tarjeta>
          <Etiqueta>Sus preguntas</Etiqueta>
          {partner.prompts.map((pregunta) => (
            <View key={pregunta.question} style={{ gap: 3 }}>
              <Text style={estilos.pregunta}>{pregunta.label}</Text>
              <Text style={estilos.respuesta}>{pregunta.answer}</Text>
            </View>
          ))}
        </Tarjeta>
      ) : (
        <View style={estilos.bloqueado}>
          <Text style={estilos.pista}>🔒 Sus tres preguntas llegan en el nivel 2</Text>
        </View>
      )}

      {partner.level >= 3 &&
      (partner.genderLabel || partner.fromPlace || partner.languages.length > 0) ? (
        <Tarjeta>
          <Etiqueta>Más de su perfil</Etiqueta>
          {partner.genderLabel || partner.fromPlace ? (
            <Text style={estilos.respuesta}>
              {[partner.genderLabel, partner.fromPlace].filter(Boolean).join(' · ')}
            </Text>
          ) : null}
          {partner.languages.length > 0 && (
            <Text style={estilos.respuesta}>
              Habla {partner.languages.map(nombreDeIdioma).join(', ')}
            </Text>
          )}
        </Tarjeta>
      ) : (
        <View style={estilos.bloqueado}>
          <Text style={estilos.pista}>🔒 Foto y resto del perfil, en el nivel 3</Text>
        </View>
      )}
    </View>
  );
}

/**
 * "Quiero verte", en una franja fija encima del campo de escribir: cuando llega
 * su momento tiene que estar a la vista, no enterrado arriba del hilo.
 */
export function PedirFoto({
  chat,
  pidiendo,
  onQuieroVerte,
}: {
  chat: Chat;
  pidiendo: boolean;
  onQuieroVerte: () => void;
}) {
  if (chat.alreadyAskedForPhoto) {
    return (
      <View style={estilos.franja}>
        <Text style={estilos.franjaEmoji}>📸</Text>
        <Text style={[estilos.pista, { flexShrink: 1 }]}>
          Pedido. La foto aparecerá si la otra persona también lo pide, y no sabrá que tú lo hiciste.
        </Text>
      </View>
    );
  }
  return (
    <View style={estilos.franja}>
      <Text style={estilos.franjaEmoji}>📸</Text>
      <View style={{ flex: 1, gap: 8 }}>
        <Text style={estilos.franjaTitulo}>¿Queréis veros?</Text>
        <Text style={estilos.pista}>Hace falta que lo pidáis los dos. Nadie sabe si el otro lo pidió.</Text>
        <Boton texto="Quiero verte" onPress={onQuieroVerte} ocupado={pidiendo} />
      </View>
    </View>
  );
}

const estilos = StyleSheet.create({
  escalones: { flexDirection: 'row', gap: 4 },
  escalon: {
    width: 16,
    height: 7,
    borderRadius: 999,
    backgroundColor: colors.lineFuerte,
  },
  escalonActual: { width: 26 },

  cabecera: { alignItems: 'center', gap: 4, paddingBottom: 4 },
  foto: {
    width: 150,
    height: 190,
    borderRadius: 26,
    borderWidth: 4,
    borderColor: colors.sol,
    backgroundColor: colors.surface2,
    marginBottom: 8,
  },
  marcoVacio: {
    width: 96,
    height: 96,
    borderRadius: 999,
    backgroundColor: colors.sol,
    borderWidth: 4,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  interrogante: { fontFamily: fonts.displayFuerte, fontSize: 40, color: colors.ink },
  nombre: { fontFamily: fonts.display, fontSize: 26, color: colors.ink },
  filaNivel: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  pregunta: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.ink },
  respuesta: { fontFamily: fonts.sans, fontSize: 14.5, lineHeight: 21, color: colors.ink2 },
  pista: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  bloqueado: {
    borderRadius: 18,
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: colors.trazo,
    padding: 16,
    alignItems: 'center',
  },

  franja: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'flex-start',
    backgroundColor: colors.surface,
    borderRadius: 22,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 14,
  },
  franjaEmoji: { fontSize: 26 },
  franjaTitulo: { fontFamily: fonts.display, fontSize: 19, color: colors.ink },
});
