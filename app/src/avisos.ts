import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { registerPushToken } from './api';

/**
 * Los avisos de Olimpus.
 *
 * Son cinco y ya: reparto de la mañana, mensaje nuevo, repesca, la pregunta de
 * las 21:30 y el resultado. Una app que avisa demasiado se desinstala, y aquí
 * las horas ya las marca el propio producto.
 */

// Con la app abierta no se enseña el aviso del sistema: lo que pasa dentro ya
// se ve en la pantalla, y un banner encima sería ruido.
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: false,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

/**
 * Pide permiso y le dice al servidor dónde encontrar este móvil.
 *
 * Si la persona dice que no, no pasa nada: la app funciona igual, solo que sin
 * avisos. Nunca se vuelve a insistir.
 */
export async function activarAvisos(token: string): Promise<'activados' | 'sin permiso' | 'no aplica'> {
  if (!Device.isDevice) return 'no aplica';

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Olimpus',
      importance: Notifications.AndroidImportance.DEFAULT,
    });
  }

  const actual = await Notifications.getPermissionsAsync();
  const permiso = actual.granted ? actual : await Notifications.requestPermissionsAsync();
  if (!permiso.granted) return 'sin permiso';

  const projectId =
    Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;

  const push = await Notifications.getExpoPushTokenAsync(projectId ? { projectId } : undefined);
  await registerPushToken(token, push.data);
  return 'activados';
}
