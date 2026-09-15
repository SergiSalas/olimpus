import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { partnerPhotoSource, type Chat } from '../api';
import { Boton, Etiqueta, Pastilla, Tarjeta } from '../components';
import { useNombreInteres } from '../interests';
import { colors, fonts } from '../theme';

const NIVELES = [
  { n: 0, titulo: 'Match', desc: 'Edad, dos intereses, zona' },
  { n: 1, titulo: 'Primer mensaje', desc: 'Apodo y todos los intereses' },
  { n: 2, titulo: 'Conversación', desc: 'Bio corta' },
  { n: 3, titulo: 'Buena conexión', desc: 'Foto, si los dos aceptáis' },
  { n: 4, titulo: 'Confianza', desc: 'Lo que cada uno quiera' },
];

/** Los cinco puntos de la escalera, con el nivel actual encendido. */
export function Escalones({ nivel }: { nivel: number }) {
  return (
    <View style={estilos.escalones}>
      {NIVELES.map((n) => (
        <View
          key={n.n}
          style={[
            estilos.escalon,
            n.n <= nivel && estilos.escalonGanado,
            n.n === nivel && estilos.escalonActual,
          ]}
        />
      ))}
    </View>
  );
}

/**
 * Lo que se ha destapado de la otra persona, dentro del chat.
 *
 * Cada trozo aparece cuando su nivel llega. Lo que no está aquí es porque el
 * servidor no lo ha mandado: no hay nada escondido en la pantalla.
 */
export function LoQueSeVe({
  chat,
  token,
  pidiendo,
  onQuieroVerte,
}: {
  chat: Chat;
  token: string;
  pidiendo: boolean;
  onQuieroVerte: () => void;
}) {
  const nombreInteres = useNombreInteres();
  const { partner } = chat;

  return (
    <View style={{ gap: 12 }}>
      <Tarjeta>
        <View style={estilos.cabecera}>
          <Etiqueta tono="accent">
            {`Nivel ${partner.level} · ${NIVELES[partner.level]?.titulo ?? ''}`}
          </Etiqueta>
          <Escalones nivel={partner.level} />
        </View>

        {partner.photoAvailable ? (
          <View style={estilos.filaFoto}>
            <Image
              source={partnerPhotoSource(token, chat.conversationId)}
              style={estilos.foto}
            />
            <View style={{ flexShrink: 1, gap: 4 }}>
              <Text style={estilos.nombre}>
                {partner.nickname}, {partner.age}
              </Text>
              {partner.bio ? <Text style={estilos.bio}>{partner.bio}</Text> : null}
            </View>
          </View>
        ) : (
          <View style={estilos.filaFoto}>
            <View style={estilos.marcoVacio}>
              <Text style={estilos.interrogante}>?</Text>
            </View>
            <View style={{ flexShrink: 1, gap: 4 }}>
              <Text style={estilos.nombre}>
                {partner.nickname ? `${partner.nickname}, ${partner.age}` : `${partner.age} años`}
              </Text>
              <Text style={estilos.pista}>
                {partner.level === 0
                  ? 'Su apodo aparece en cuanto escribáis los dos'
                  : partner.level === 1
                    ? 'Su bio llega con más conversación y algo de tiempo'
                    : 'Su foto, solo si los dos decís que queréis veros'}
              </Text>
            </View>
          </View>
        )}

        <View style={estilos.rejilla}>
          {partner.interests.map((interes) => (
            <Pastilla key={interes} texto={nombreInteres(interes)} elegida tono="suave" />
          ))}
        </View>

        {partner.bio && !partner.photoAvailable ? (
          <Text style={estilos.bio}>{partner.bio}</Text>
        ) : null}

        {partner.level >= 3 && partner.languages.length > 0 && (
          <Text style={estilos.pista}>
            Habla {partner.languages.map((l) => l.toUpperCase()).join(', ')}
          </Text>
        )}
      </Tarjeta>

      {chat.canAskForPhoto && !partner.photoAvailable && (
        <Tarjeta>
          {chat.alreadyAskedForPhoto ? (
            <>
              <Etiqueta>Ya lo has pedido</Etiqueta>
              <Text style={estilos.pista}>
                La foto aparecerá si la otra persona también lo pide. No sabe que tú lo has hecho.
              </Text>
            </>
          ) : (
            <>
              <Etiqueta>Nivel 3</Etiqueta>
              <Text style={estilos.pista}>
                A partir de aquí decidís vosotros, no la app. Hace falta que los dos digáis que sí, y
                nadie sabrá si el otro lo pidió.
              </Text>
              <Boton texto="Quiero verte" onPress={onQuieroVerte} ocupado={pidiendo} />
            </>
          )}
        </Tarjeta>
      )}
    </View>
  );
}

const estilos = StyleSheet.create({
  cabecera: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  escalones: { flexDirection: 'row', gap: 5 },
  escalon: {
    width: 18,
    height: 4,
    borderRadius: 999,
    backgroundColor: colors.lineFuerte,
  },
  escalonGanado: { backgroundColor: colors.accentBorde },
  escalonActual: { backgroundColor: colors.accent },

  filaFoto: { flexDirection: 'row', gap: 14, alignItems: 'center' },
  foto: { width: 88, height: 112, borderRadius: 16, backgroundColor: colors.surface2 },
  marcoVacio: {
    width: 56,
    height: 56,
    borderRadius: 999,
    backgroundColor: colors.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  interrogante: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.ink5 },
  nombre: { fontFamily: fonts.sansNegrita, fontSize: 16, color: colors.ink },
  bio: { fontFamily: fonts.sans, fontSize: 13.5, lineHeight: 20, color: colors.ink2 },
  pista: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  rejilla: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
});
