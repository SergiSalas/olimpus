import { useState } from 'react';
import { Alert, Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, deleteAccount, ownPhotoSource, type Me, type Profile } from '../api';
import { Entrada, Flotar, Pastilla, Rebote } from '../components';
import { COLOR_NIVEL, NIVELES } from '../components/Escalera';
import { useNombreInteres } from '../interests';
import {
  COLOR_HUECO,
  FONDO_HUECO,
  IDIOMAS,
  nombreDeIdioma,
  nombreConversacion,
  nombreDeNivel,
  pistaDe,
  useCatalogoPreguntas,
} from '../preguntas';
import { tipoDeProfundidad } from '../mapeo';
import { colors, fonts } from '../theme';
import { HojaIdioma } from '../components/SelectorIdioma';
import { IDIOMAS_APP, idioma, t } from '../i18n';
import type { Clave } from '../idiomas/es';

// Los valores del servidor (WOMAN, DATING, 1-5) a palabras, en el idioma de la app.
const genero = (g: string) => t(`genero.${g}` as Clave);
const busca = (g: string) => t(`busca.${g}` as Clave);
const intencion = (i: string) => t(`intencion.${i}` as Clave);
const sociabilidad = (n: number) => t(`sociabilidad.${n}` as Clave);

/**
 * Tu perfil: cómo eres para la app, cómo te ve la otra persona en cada nivel, y
 * los ajustes de la cuenta al final.
 */
export function HomeScreen({
  me,
  perfil,
  token,
  onVolver,
  onEditar,
  onSalir,
}: {
  me: Me;
  perfil: Profile;
  token: string;
  onVolver: () => void;
  onEditar: () => void;
  onSalir: () => void;
}) {
  const nombreInteres = useNombreInteres();
  const catalogoPreguntas = useCatalogoPreguntas();
  const nombrePregunta = (id: string) => catalogoPreguntas.find((q) => q.name === id)?.label ?? id;
  const [borrando, setBorrando] = useState(false);
  const [eligiendoIdioma, setEligiendoIdioma] = useState(false);
  const idiomaApp = IDIOMAS_APP.find((i) => i.codigo === idioma());

  /**
   * Se avisa de lo que se pierde ANTES de preguntar, con las palabras claras:
   * "esto no se puede deshacer" cuesta poco de escribir y evita el arrepentimiento
   * de alguien que creía que solo cerraba sesión.
   */
  function confirmarBorrado() {
    Alert.alert(t('perfil.borrarTitulo'), t('perfil.borrarAviso'), [
      { text: t('comun.cancelar'), style: 'cancel' },
      {
        text: t('perfil.borrarSiempre'),
        style: 'destructive',
        onPress: async () => {
          setBorrando(true);
          try {
            await deleteAccount(token);
            onSalir();
          } catch (e) {
            Alert.alert(
              t('perfil.noSeBorro'),
              e instanceof ApiError ? e.message : t('comun.reintentar'),
            );
          } finally {
            setBorrando(false);
          }
        },
      },
    ]);
  }

  const baldosas = [
    { emoji: '🎂', titulo: t('perfil.edad'), valor: t('comun.anos', { edad: perfil.age }) },
    {
      emoji: '🙂',
      titulo: t('perfil.genero'),
      valor: perfil.genderLabel || genero(perfil.gender),
    },
    {
      emoji: '💘',
      titulo: t('perfil.hablarCon'),
      valor: perfil.seeking.length === 4 ? t('perfil.todos') : perfil.seeking.map(busca).join(', '),
    },
    {
      emoji: '📏',
      titulo: t('perfil.edades'),
      valor: t('perfil.rangoEdad', { min: perfil.ageMin, max: perfil.ageMax }),
    },
    {
      emoji: '📍',
      titulo: t('perfil.distancia'),
      valor: t('perfil.hastaKm', { km: perfil.maxDistanceKm }),
    },
    { emoji: '🎯', titulo: t('perfil.buscas'), valor: intencion(perfil.intent) },
    {
      emoji: '💬',
      titulo: t('perfil.conversacion'),
      valor: nombreConversacion(tipoDeProfundidad(perfil.conversationDepth)),
    },
    { emoji: '🫶', titulo: t('perfil.relacionas'), valor: sociabilidad(perfil.sociability) },
  ];

  const banderas = perfil.languages
    .map((l) => IDIOMAS.find((i) => i.code === l.code)?.bandera)
    .filter(Boolean)
    .join(' ');

  return (
    <ScrollView style={estilos.pantalla} contentContainerStyle={{ paddingBottom: 40 }}>
      <Entrada>
        <View style={estilos.cabecera}>
          <View style={estilos.filaCabecera}>
            <Rebote style={estilos.redondo} onPress={onVolver}>
              <Text style={estilos.flecha}>←</Text>
            </Rebote>
            <Rebote style={estilos.editar} onPress={onEditar}>
              <Text style={estilos.editarTexto}>{t('perfil.editar')}</Text>
            </Rebote>
          </View>

          <Flotar distancia={5} giro={2} duracion={3200}>
            <Image source={ownPhotoSource(token)} style={estilos.foto} />
          </Flotar>
          <Text style={estilos.nombre}>
            {perfil.nickname}, {perfil.age}
          </Text>
          <View style={estilos.chipsCabecera}>
            <Chip texto={`📍 ${t('perfil.hastaKm', { km: perfil.maxDistanceKm })}`} />
            <Chip texto={`🎯 ${intencion(perfil.intent)}`} />
            {banderas ? <Chip texto={banderas} /> : null}
          </View>
        </View>
      </Entrada>

      <View style={estilos.cuerpo}>
        <Entrada retraso={120}>
          <ComoTeVen
            perfil={perfil}
            token={token}
            nombreInteres={nombreInteres}
            nombrePregunta={nombrePregunta}
          />
        </Entrada>

        <Seccion titulo={t('perfil.basico')} retraso={200}>
          <View style={estilos.rejilla}>
            {baldosas.map((b, i) => (
              <Entrada key={b.titulo} retraso={240 + i * 50} style={estilos.baldosa}>
                <Text style={estilos.baldosaEmoji}>{b.emoji}</Text>
                <Text style={estilos.baldosaTitulo}>{b.titulo}</Text>
                <Text style={estilos.baldosaValor}>{b.valor}</Text>
              </Entrada>
            ))}
          </View>
          <View style={estilos.idiomas}>
            <Text style={estilos.baldosaTitulo}>{t('perfil.idiomas')}</Text>
            {perfil.languages.map((l) => (
              <View key={l.code} style={estilos.filaIdioma}>
                <Text style={estilos.baldosaValor}>{nombreDeIdioma(l.code)}</Text>
                <Text style={estilos.nivelIdioma}>{nombreDeNivel(l.level)}</Text>
              </View>
            ))}
          </View>
        </Seccion>

        <Seccion titulo={t('perfil.intereses')} retraso={300}>
          <View style={estilos.chips}>
            {perfil.interests.map((interes, i) => (
              <Entrada key={interes} retraso={350 + i * 50}>
                <Pastilla texto={nombreInteres(interes)} elegida tono="suave" />
              </Entrada>
            ))}
          </View>
        </Seccion>

        <Seccion titulo={t('perfil.preguntas')} retraso={380}>
          <View style={{ gap: 10 }}>
            {perfil.prompts.map((p, i) => (
              <View
                key={p.question}
                style={[
                  estilos.tarjetaPregunta,
                  { backgroundColor: FONDO_HUECO[i], borderColor: COLOR_HUECO[i] },
                ]}>
                <Text style={estilos.preguntaTexto}>
                  {pistaDe(p.question).emoji} {nombrePregunta(p.question)}…
                </Text>
                <Text style={estilos.respuesta}>{p.answer}</Text>
              </View>
            ))}
          </View>
        </Seccion>

        {(perfil.occupation || perfil.fromPlace) && (
          <Seccion titulo={t('perfil.masDeTi')} retraso={440}>
            <View style={{ gap: 10 }}>
              {perfil.occupation ? (
                <View style={estilos.filaMas}>
                  <Text style={estilos.baldosaTitulo}>{t('perfil.dedicas')}</Text>
                  <Text style={estilos.baldosaValor}>{perfil.occupation}</Text>
                </View>
              ) : null}
              {perfil.fromPlace ? (
                <View style={estilos.filaMas}>
                  <Text style={estilos.baldosaTitulo}>{t('perfil.deDonde')}</Text>
                  <Text style={estilos.baldosaValor}>{perfil.fromPlace}</Text>
                </View>
              ) : null}
            </View>
          </Seccion>
        )}

        <Seccion titulo={t('perfil.ajustes')} retraso={500}>
          <View style={estilos.ajustes}>
            <FilaAjuste emoji="✏️" texto={t('perfil.cambiarRespuestas')} onPress={onEditar} />
            <View style={estilos.separador} />
            <FilaAjuste
              emoji="🌐"
              texto={t('idiomaApp.titulo')}
              detalle={`${idiomaApp?.bandera} ${idiomaApp?.nombre}`}
              onPress={() => setEligiendoIdioma(true)}
            />
            <View style={estilos.separador} />
            <FilaAjuste
              emoji="🚪"
              texto={t('perfil.cerrarSesion')}
              detalle={me.email}
              onPress={onSalir}
            />
            <View style={estilos.separador} />
            <FilaAjuste
              emoji="🗑️"
              texto={borrando ? t('perfil.borrando') : t('perfil.borrar')}
              peligro
              onPress={confirmarBorrado}
              deshabilitada={borrando}
            />
          </View>
        </Seccion>
      </View>
      <HojaIdioma visible={eligiendoIdioma} onCerrar={() => setEligiendoIdioma(false)} />
    </ScrollView>
  );
}

/**
 * Tu perfil visto desde fuera, nivel a nivel. Enseña qué se destapa en cada
 * uno, así la escalera se entiende mirándote a ti mismo.
 */
function ComoTeVen({
  perfil,
  token,
  nombreInteres,
  nombrePregunta,
}: {
  perfil: Profile;
  token: string;
  nombreInteres: (id: string) => string;
  nombrePregunta: (id: string) => string;
}) {
  const [nivel, setNivel] = useState(0);
  const color = COLOR_NIVEL[nivel];

  return (
    <View style={estilos.comoTeVen}>
      <Text style={estilos.seccionTitulo}>{t('comoTeVen.titulo')}</Text>
      <Text style={estilos.pista}>{t('comoTeVen.pista')}</Text>

      <View style={estilos.escalera}>
        <View style={estilos.lineaEscalera} />
        {NIVELES.map((n) => (
          <Rebote
            key={n.n}
            pop={nivel === n.n}
            onPress={() => setNivel(n.n)}
            style={[
              estilos.peldano,
              { backgroundColor: COLOR_NIVEL[n.n] },
              nivel === n.n ? estilos.peldanoActivo : { opacity: 0.35 },
            ]}>
            <Text style={[estilos.peldanoTexto, (n.n === 1 || n.n === 2) && { color: colors.ink }]}>
              {n.n}
            </Text>
          </Rebote>
        ))}
      </View>

      <Entrada key={nivel} style={[estilos.nivelCaja, { borderColor: color }]}>
        <Text style={[estilos.nivelTitulo, { color: nivel === 1 ? '#A87A00' : color }]}>
          {t('comun.nivelTitulo', { nivel, titulo: NIVELES[nivel].titulo })}
        </Text>

        {nivel === 0 && (
          <>
            <Text style={estilos.respuesta}>{t('comoTeVen.nivel0', { edad: perfil.age })}</Text>
            <View style={estilos.chips}>
              {perfil.interests.slice(0, 2).map((i) => (
                <Pastilla key={i} texto={nombreInteres(i)} elegida tono="suave" />
              ))}
            </View>
            <Text style={estilos.pista}>{t('comoTeVen.dosIntereses')}</Text>
          </>
        )}

        {nivel === 1 && (
          <>
            <Text style={estilos.respuesta}>
              {t('comoTeVen.nivel1', { apodo: perfil.nickname })}
            </Text>
            <View style={estilos.chips}>
              {perfil.interests.map((i) => (
                <Pastilla key={i} texto={nombreInteres(i)} elegida tono="suave" />
              ))}
            </View>
          </>
        )}

        {nivel === 2 &&
          perfil.prompts.map((p) => (
            <View key={p.question} style={{ gap: 2 }}>
              <Text style={estilos.negrita}>{nombrePregunta(p.question)}</Text>
              <Text style={estilos.respuesta}>{p.answer}</Text>
            </View>
          ))}

        {nivel === 3 && (
          <View style={estilos.filaNivel3}>
            <Image source={ownPhotoSource(token)} style={estilos.fotoPequena} />
            <View style={{ flexShrink: 1, gap: 3 }}>
              <Text style={estilos.negrita}>{t('comoTeVen.foto')}</Text>
              {[perfil.genderLabel, perfil.occupation, perfil.fromPlace]
                .filter(Boolean)
                .map((dato) => (
                  <Text key={dato} style={estilos.respuesta}>
                    {dato}
                  </Text>
                ))}
              <Text style={estilos.respuesta}>
                {perfil.languages.map((l) => nombreDeIdioma(l.code)).join(', ')}
              </Text>
            </View>
          </View>
        )}

        {nivel === 4 && <Text style={estilos.respuesta}>{t('comoTeVen.nivel4')}</Text>}

        {nivel < 4 && (
          <Text style={estilos.pista}>
            {t('comoTeVen.siguiente', { desc: NIVELES[nivel + 1].desc.toLowerCase() })}
          </Text>
        )}
      </Entrada>
    </View>
  );
}

function Seccion({
  titulo,
  retraso,
  children,
}: {
  titulo: string;
  retraso: number;
  children: React.ReactNode;
}) {
  return (
    <Entrada retraso={retraso} style={{ gap: 10 }}>
      <Text style={estilos.seccionTitulo}>{titulo}</Text>
      {children}
    </Entrada>
  );
}

function Chip({ texto }: { texto: string }) {
  return (
    <View style={estilos.chip}>
      <Text style={estilos.chipTexto}>{texto}</Text>
    </View>
  );
}

function FilaAjuste({
  emoji,
  texto,
  detalle,
  peligro,
  deshabilitada,
  onPress,
}: {
  emoji: string;
  texto: string;
  detalle?: string;
  peligro?: boolean;
  deshabilitada?: boolean;
  onPress: () => void;
}) {
  return (
    <Rebote style={estilos.filaAjuste} onPress={onPress} disabled={deshabilitada}>
      <Text style={estilos.ajusteEmoji}>{emoji}</Text>
      <View style={{ flex: 1, gap: 1 }}>
        <Text style={[estilos.ajusteTexto, peligro && { color: colors.error }]}>{texto}</Text>
        {detalle ? <Text style={estilos.pista}>{detalle}</Text> : null}
      </View>
      <Text style={estilos.ajusteFlecha}>›</Text>
    </Rebote>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: colors.bg },

  cabecera: {
    backgroundColor: colors.uva,
    paddingTop: 54,
    paddingBottom: 26,
    paddingHorizontal: 20,
    borderBottomLeftRadius: 36,
    borderBottomRightRadius: 36,
    borderBottomWidth: 6,
    borderColor: '#6A3FD1',
    alignItems: 'center',
  },
  filaCabecera: {
    alignSelf: 'stretch',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  redondo: {
    width: 40,
    height: 40,
    borderRadius: 999,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  flecha: { fontFamily: fonts.sansNegrita, fontSize: 18, color: '#FFFFFF' },
  editar: {
    backgroundColor: '#FFFFFF',
    borderRadius: 999,
    paddingHorizontal: 16,
    paddingVertical: 9,
    borderBottomWidth: 3,
    borderColor: colors.trazo,
  },
  editarTexto: { fontFamily: fonts.sansNegrita, fontSize: 14, color: colors.uva },
  foto: {
    width: 130,
    height: 130,
    borderRadius: 999,
    borderWidth: 5,
    borderColor: colors.sol,
    backgroundColor: colors.surface2,
  },
  nombre: { fontFamily: fonts.displayFuerte, fontSize: 32, color: '#FFFFFF', marginTop: 12 },
  chipsCabecera: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 7,
    marginTop: 10,
  },
  chip: {
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  chipTexto: { fontFamily: fonts.sansNegrita, fontSize: 13, color: '#FFFFFF' },

  cuerpo: { paddingHorizontal: 20, paddingTop: 22, gap: 26 },
  seccionTitulo: { fontFamily: fonts.display, fontSize: 22, color: colors.ink },
  pista: { fontFamily: fonts.sans, fontSize: 13, lineHeight: 19, color: colors.ink3 },
  respuesta: { fontFamily: fonts.sans, fontSize: 14.5, lineHeight: 21, color: colors.ink2 },
  negrita: { fontFamily: fonts.sansNegrita, fontSize: 14.5, color: colors.ink },

  comoTeVen: {
    backgroundColor: colors.surface,
    borderRadius: 26,
    borderWidth: 1.5,
    borderBottomWidth: 5,
    borderColor: colors.line,
    padding: 18,
    gap: 6,
  },
  escalera: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 12,
    paddingHorizontal: 4,
  },
  lineaEscalera: {
    position: 'absolute',
    left: 20,
    right: 20,
    height: 4,
    borderRadius: 999,
    backgroundColor: colors.surface2,
  },
  peldano: {
    width: 38,
    height: 38,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: '#FFFFFF',
  },
  // Tamaño y no escala: la escala la usa el rebote y la pisaría.
  peldanoActivo: { width: 46, height: 46, borderColor: colors.ink },
  peldanoTexto: { fontFamily: fonts.displayFuerte, fontSize: 16, color: '#FFFFFF' },
  nivelCaja: {
    borderRadius: 18,
    borderWidth: 2,
    borderStyle: 'dashed',
    padding: 14,
    gap: 8,
  },
  nivelTitulo: { fontFamily: fonts.sansNegrita, fontSize: 14 },
  filaNivel3: { flexDirection: 'row', gap: 12, alignItems: 'center' },
  fotoPequena: {
    width: 64,
    height: 80,
    borderRadius: 14,
    borderWidth: 3,
    borderColor: colors.sol,
    backgroundColor: colors.surface2,
  },

  rejilla: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', rowGap: 10 },
  baldosa: {
    width: '48.5%',
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 14,
    gap: 2,
  },
  baldosaEmoji: { fontSize: 22, marginBottom: 2 },
  baldosaTitulo: { fontFamily: fonts.sansMedia, fontSize: 12.5, color: colors.ink3 },
  baldosaValor: { fontFamily: fonts.sansNegrita, fontSize: 15, color: colors.ink },
  idiomas: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 14,
    gap: 8,
  },
  filaIdioma: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  nivelIdioma: { fontFamily: fonts.sansMedia, fontSize: 13, color: colors.uva },

  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },

  tarjetaPregunta: { borderRadius: 20, borderWidth: 2, borderBottomWidth: 4, padding: 14, gap: 6 },
  preguntaTexto: { fontFamily: fonts.display, fontSize: 17, lineHeight: 22, color: colors.ink },

  filaMas: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    padding: 14,
    gap: 2,
  },

  ajustes: {
    backgroundColor: colors.surface,
    borderRadius: 22,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: colors.line,
    overflow: 'hidden',
  },
  filaAjuste: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 15,
  },
  ajusteEmoji: { fontSize: 20 },
  ajusteTexto: { fontFamily: fonts.sansNegrita, fontSize: 15.5, color: colors.ink },
  ajusteFlecha: { fontFamily: fonts.sansNegrita, fontSize: 20, color: colors.ink4 },
  separador: { height: 1.5, backgroundColor: colors.lineSuave, marginLeft: 48 },
});
