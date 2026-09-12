# Project Olimpus

«Hablar primero, ver después»: app de citas en la que el perfil se descubre
según va funcionando la conversación.
Estado: **paso 1 de 10** — esqueleto que ya se ve funcionando de punta a punta.

Descripción de producto: https://claude.ai/code/artifact/2e92f35d-5b0b-43df-9937-9440a0e5ca1d

## Qué hay

```
backend/   Spring Boot 3.5 · Java 21 · PostgreSQL · Flyway
app/       React Native con Expo (TypeScript), primero para iPhone
docker-compose.yml   la base de datos de desarrollo
```

El backend sigue **arquitectura hexagonal**: las reglas del negocio viven en
clases Java normales que no saben nada de Spring, y alrededor van los
adaptadores (HTTP, base de datos, y más adelante moderación o verificación de
edad, que son piezas intercambiables). Ejemplo completo y pequeño en
`backend/src/main/java/com/sergisalas/olimpus/health/`:

| Pieza | Fichero |
|---|---|
| Dato del dominio | `health/domain/Health.java` |
| Puerto de salida | `health/domain/DatabaseInfo.java` |
| Caso de uso | `health/application/CheckHealth.java` |
| Adaptador de entrada (HTTP) | `health/adapter/in/HealthController.java` |
| Adaptador de salida (PostgreSQL) | `health/adapter/out/JdbcDatabaseInfo.java` |
| Dónde se montan | `config/DomainBeans.java` |

## Cómo se arranca

**1. La base de datos** (necesita Docker Desktop abierto):

```bash
docker compose up -d
```

Queda escuchando en el puerto **5433**, no en el 5432: este ordenador ya tiene
un PostgreSQL propio instalado ocupando el 5432 y no lo tocamos. Si el backend
dijera *«la autentificación password falló»*, es que se está conectando al otro.

Espera a que el contenedor esté `healthy` antes de arrancar el backend
(`docker compose ps`): si arrancas demasiado pronto, Flyway no encuentra la
base de datos y el backend se cierra.

**2. El backend:**

```bash
cd backend
./mvnw spring-boot:run
```

Comprobar: http://localhost:8080/api/health

**3. La app en tu iPhone:**

```bash
cd app
npx expo start
```

Instala **Expo Go** desde la App Store, escanea el código QR con la cámara y
el móvil tiene que estar en el mismo wifi que el ordenador. La app busca el
backend sola: usa la dirección desde la que Expo le sirve el código, en el
puerto 8080. Si hiciera falta forzarla, crea `app/.env` con:

```
EXPO_PUBLIC_BACKEND_URL=http://192.168.1.40:8080
```

> Si el móvil no conecta, casi siempre es el **cortafuegos de Windows**
> bloqueando el puerto 8080. Hay que permitir Java en redes privadas.

## Los tests

```bash
cd backend
./mvnw test
```

Las reglas del negocio se prueban sin Spring, sin base de datos y sin red, así
que tardan milisegundos.

## Decisiones ya tomadas

- Una conversación nueva al día: reparto a las 4:00, repesca a las 14:00,
  cierre a las 22:00. A las 21:30 cada uno responde en privado si quiere seguir.
- Desbloqueo por niveles: N1 cuando han escrito los dos; N2 con 3 turnos de
  cada uno y 1 hora; N3 solo si los dos lo aceptan, tras N2 y 4 horas; N4 tras
  la conexión.
- Reparto: 80 % mejor pareja, 10 % descubrimiento, 10 % azar puro (para poder
  comparar si el algoritmo aporta algo).
- Las horas se guardan siempre en UTC; la hora local se calcula al mostrarla.
- Solo mayores de 18. Verificación de edad y moderación son piezas
  intercambiables: falsas mientras se programa, reales antes de la beta.
  **Nadie real entra en la beta sin las dos funcionando.**

## Lo que falta (pasos 2 a 10)

Entrar con email y código · registro de 10 preguntas con foto · el reparto
diario en Java puro · las tareas de las 4:00 y las 14:00 · el chat en tiempo
real · la escalera de desbloqueo · decisión mutua y conexiones · reportar,
bloquear y avisos · registro de datos para el algoritmo · proveedores reales,
textos legales, cuenta de Apple y servidor en la UE.
