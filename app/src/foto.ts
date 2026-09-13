import * as ImageManipulator from 'expo-image-manipulator';
import * as ImagePicker from 'expo-image-picker';

/**
 * Elegir una foto de la galería y dejarla lista para enviar.
 *
 * Se reduce a 1200 píxeles de lado y se vuelve a guardar como JPG. Eso hace dos
 * cosas: baja el peso (de varios megas a unos cientos de kilobytes) y, al
 * reescribir el fichero, **se pierden los datos ocultos** que traen las fotos
 * del móvil, incluidas las coordenadas de dónde se hizo. Nadie debería subir su
 * casa sin saberlo.
 */
export async function elegirFoto(): Promise<string | null> {
  const permiso = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permiso.granted) {
    throw new Error('Sin permiso no podemos abrir tus fotos. Puedes dárnoslo en Ajustes.');
  }

  const eleccion = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsEditing: true,
    aspect: [3, 4],
    quality: 1,
  });

  if (eleccion.canceled || eleccion.assets.length === 0) return null;

  const reducida = await ImageManipulator.manipulateAsync(
    eleccion.assets[0].uri,
    [{ resize: { width: 1200 } }],
    { compress: 0.75, format: ImageManipulator.SaveFormat.JPEG },
  );

  return reducida.uri;
}
