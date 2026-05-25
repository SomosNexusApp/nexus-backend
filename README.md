# nexus-backend

API REST y backend principal de **Nexus** — la plataforma de marketplace unificado donde usuarios pueden comprar, vender y descubrir productos, ofertas, vehículos y servicios.

Construido con **Spring Boot 3.5 / Java 17** y desplegado como contenedor Docker.

---

## Tabla de contenidos

1. [Arquitectura general](#arquitectura-general)
2. [Stack tecnológico](#stack-tecnológico)
3. [Estructura del proyecto](#estructura-del-proyecto)
4. [Módulos y responsabilidades](#módulos-y-responsabilidades)
5. [Seguridad](#seguridad)
6. [WebSockets](#websockets)
7. [Integraciones externas](#integraciones-externas)
8. [Tareas programadas](#tareas-programadas)
9. [Variables de entorno](#variables-de-entorno)
10. [Cómo ejecutar en local](#cómo-ejecutar-en-local)
11. [Docker](#docker)
12. [Convenciones de código](#convenciones-de-código)

---

## Arquitectura general

```
nexus-backend/
└── src/main/java/com/nexus/
    ├── controller/     ← Endpoints REST (@RestController)
    ├── service/        ← Lógica de negocio
    ├── repository/     ← Interfaces JPA (Spring Data)
    ├── entity/         ← Entidades JPA (mapeo a base de datos)
    ├── dto/            ← Data Transfer Objects (request/response)
    ├── security/       ← JWT, filtros y configuración de Spring Security
    ├── config/         ← Configuraciones: CORS, WebSocket, caché, Jackson…
    ├── scheduler/      ← Tareas programadas con @Scheduled
    └── soporte/        ← Lógica de soporte al usuario
```

El backend sigue una arquitectura **Controller → Service → Repository** estándar:
- Los **controllers** reciben la petición HTTP, delegan al servicio adecuado y devuelven la respuesta.
- Los **services** contienen toda la lógica de negocio. **Nunca** se llama directamente a un repositorio desde un controller.
- Los **repositories** son interfaces JPA que heredan de `JpaRepository`. Aquí van las queries con `@Query` cuando la query derivada no es suficiente.
- Las **entities** son las clases JPA que mapean directamente la base de datos.
- Los **DTO** desacoplan la capa de persistencia de la API pública.

---

## Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.5.13 | Framework base |
| Spring Security | (incluido) | Autenticación y autorización |
| Spring Data JPA | (incluido) | Acceso a datos |
| Spring WebSocket / STOMP | (incluido) | Chat en tiempo real |
| Spring Cache | (incluido) | Caché en memoria |
| Spring Mail | (incluido) | Envío de correos |
| PostgreSQL | (driver incluido) | Base de datos principal |
| JWT (jjwt) | 0.11.5 | Tokens de autenticación |
| Stripe Java | 24.1.0 | Pasarela de pagos |
| Cloudinary | 1.36.0 | Almacenamiento de imágenes |
| Google API Client | 2.2.0 | Login con Google |
| TOTP (2FA) | 1.7.1 | Autenticación en dos pasos |
| ZXing | 3.5.3 | Generación de códigos QR |
| SpringDoc OpenAPI | 2.6.0 | Documentación Swagger |
| Maven | 3.9 | Build tool |

---

## Estructura del proyecto

```
nexus-backend/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd            ← Maven Wrapper (úsalos, no dependas de Maven global)
└── src/
    └── main/
        ├── java/com/nexus/
        │   ├── NexusApplication.java       ← Punto de entrada
        │   ├── PopulateDB.java             ← Script de seed de datos de desarrollo
        │   ├── controller/
        │   ├── service/
        │   ├── repository/
        │   ├── entity/
        │   ├── dto/
        │   ├── security/
        │   ├── config/
        │   ├── scheduler/
        │   └── soporte/
        └── resources/
            └── application.properties      ← Configuración (no commiteada, ver sección de variables)
```

---

## Módulos y responsabilidades

### Controllers principales

| Controller | Ruta base | Descripción |
|---|---|---|
| `AuthController` | `/auth`, `/api/auth` | Registro, login, 2FA, reset de contraseña, login con Google |
| `ProductoController` | `/producto`, `/api/productos` | CRUD de productos del marketplace |
| `OfertaController` | `/oferta`, `/api/ofertas` | Gestión de ofertas flash y comunidad |
| `VehiculoController` | `/vehiculo`, `/api/vehiculos` | Anuncios de vehículos (coches, motos…) |
| `CompraController` | `/compra`, `/api/compras` | Proceso de compra y estado de pedidos |
| `EnvioController` | `/envio`, `/api/envios` | Gestión de envíos con integración de transportistas |
| `DevolucionController` | `/devolucion`, `/api/devoluciones` | Solicitudes de devolución |
| `ChatController` + `ChatWebSocketController` | `/chat`, `/ws` | Mensajería entre usuarios (REST + WebSocket) |
| `UsuarioController` | `/usuario`, `/api/usuarios` | Perfiles, ajustes de cuenta, bloqueos |
| `NotificacionController` | `/api/notificaciones` | Notificaciones en-app |
| `NewsletterController` | `/newsletter` | Suscripción, confirmación, baja |
| `PatrocinioController` | `/api/patrocinios` | Patrocinios de empresas |
| `AdminPanelController` | `/admin`, `/api/admin` | Panel administrativo (solo `ROLE_ADMIN`) |
| `StripeWebhookController` | `/api/webhooks` | Webhooks de Stripe (pagos) |
| `SitemapController` | `/sitemap.xml` | Generación dinámica de sitemap |

### Services destacados

| Service | Responsabilidad |
|---|---|
| `EmailService` | Envío de todos los correos transaccionales (confirmación, bienvenida, alertas…) usando plantillas HTML |
| `StripeService` | Creación de PaymentIntents, gestión de webhooks, reembolsos |
| `MarketplaceSearchService` | Búsqueda unificada con filtros, ordenación y sinónimos |
| `SynonymService` | Expansión de sinónimos para mejorar la relevancia de búsqueda |
| `ModerationService` | Moderación de texto y contenido generado por usuarios |
| `SoporteAiService` | Respuestas automáticas de soporte usando IA |
| `EnvioService` | Cálculo de precios, tracking y seguimiento de envíos |
| `ContratoService` | Contratos de publicidad entre empresas y Nexus |
| `NotificacionService` | Creación y entrega de notificaciones en-app y push |
| `NewsletterService` | Gestión completa del newsletter (suscripción, campañas, bajas) |
| `TwoFactorService` | Generación y validación de TOTP para 2FA |
| `StorageService` | Subida y gestión de imágenes en Cloudinary |
| `CaptchaService` | Validación de reCAPTCHA v2 |

### Entidades principales

El dominio está centrado en el usuario (`Actor`) y sus interacciones:

```
Actor (usuario base)
 ├── Usuario (comprador/vendedor)
 ├── Admin
 └── Empresa

Listados:
 ├── Producto         → comprado via Compra
 ├── Oferta           → con votos (SparkVoto) y flash deals
 └── Vehiculo

Transacciones:
 ├── Compra           → tiene Envio y puede tener Devolucion
 └── Contrato         → publicidad de empresa

Comunicación:
 ├── Mensaje          → conversaciones privadas
 ├── ChatMensaje      → mensajes en tiempo real (WebSocket)
 ├── Notificacion     → alertas en-app
 └── NotificacionInApp

Moderación:
 ├── Reporte
 ├── Bloqueo
 └── AuditLog

Otros:
 ├── Cupon / CuponUso
 ├── Categoria
 ├── Valoracion / Comentario
 ├── Favorito
 ├── NewsletterSuscripcion
 └── SesionDispositivo
```

> 📄 Hay un diagrama UML completo de entidades en `nexus_entity_uml.md` en la raíz del monorepo.

---

## Seguridad

La seguridad está gestionada en `security/SecurityConfiguration.java`. Puntos clave:

- **Sin sesiones en servidor** — autenticación 100% stateless con **JWT**.
- **CSRF deshabilitado** porque todo el acceso es via token.
- **Filtro JWT** (`JWTAuthenticationFilter`) se ejecuta en cada request antes del filtro de Spring Security estándar. Valida el token del header `Authorization: Bearer <token>`.
- **Dos roles**: `ROLE_USER` y `ROLE_ADMIN`. Las rutas `/admin/**` y `/api/admin/**` solo son accesibles con `ROLE_ADMIN`.
- **CORS** configurado explícitamente para los dominios de producción (`nexus-app.es`, `*.vercel.app`) y localhost para desarrollo.
- **2FA** opcional: el usuario puede activar TOTP. Al hacer login, si tiene 2FA activo, el servidor devuelve un token temporal y espera un segundo paso en `/api/auth/verify-2fa`.
- **WebSocket** también está protegido: `WebSocketAuthInterceptor` valida el JWT en el handshake STOMP.

---

## WebSockets

Se usa **STOMP sobre SockJS** para el chat en tiempo real:

- Endpoint de conexión: `/ws` (con fallback SockJS)
- Prefijo de mensajes al servidor: `/app`
- Topics del broker:
  - `/topic/chat/{conversacionId}` → mensajes de una conversación
  - `/topic/support/{ticketId}` → chat de soporte
  - `/user/{username}/queue/notifications` → notificaciones personales
- El broker en memoria tiene heartbeat cada 25 s para mantener conexiones vivas.

---

## Integraciones externas

| Servicio | Para qué se usa |
|---|---|
| **Stripe** | Pagos, PaymentIntents, webhooks de confirmación, reembolsos |
| **Cloudinary** | Almacenamiento y CDN de imágenes de productos, avatares, etc. |
| **Google OAuth** | Login con cuenta Google (`/api/auth/google`) |
| **Correo SMTP** | Envío de emails transaccionales (configurado via Spring Mail) |
| **Carrier API** | Seguimiento de envíos con transportistas externos |
| **reCAPTCHA** | Protección de formularios de registro y contacto |

---

## Tareas programadas

Ejecutadas con `@Scheduled` en el paquete `scheduler/`:

| Scheduler | Frecuencia | Descripción |
|---|---|---|
| `AnuncioCaducidadScheduler` | Diaria | Marca como expirados los productos/vehículos caducados |
| `EnvioPlazoScheduler` | Periódica | Detecta envíos con plazo incumplido y alerta |
| `EnvioTrackingScheduler` | Periódica | Sincroniza el estado de tracking con transportistas |
| `ProductoPausaScheduler` | Periódica | Reactiva productos que estaban pausados temporalmente |
| `UpvoteRankingScheduler` | Periódica | Recalcula el ranking de ofertas basado en votos y tiempo |

---

## Variables de entorno

El archivo `application.properties` **no está en el repositorio** por seguridad. Necesitas crear uno con las siguientes variables (pide los valores al equipo):

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://HOST:PORT/DBNAME
spring.datasource.username=USUARIO
spring.datasource.password=CONTRASEÑA

# JWT
nexus.jwt.secret=TU_SECRET_JWT_MUY_LARGO
nexus.jwt.expiration=86400000

# Frontend URLs (para CORS)
nexus.frontend.url=http://localhost:4200
nexus.admin.url=http://localhost:4201

# Stripe
stripe.secret.key=sk_test_...
stripe.webhook.secret=whsec_...

# Cloudinary
cloudinary.cloud.name=...
cloudinary.api.key=...
cloudinary.api.secret=...

# Google OAuth
google.client.id=...

# SMTP (correo)
spring.mail.host=smtp.ejemplo.com
spring.mail.port=587
spring.mail.username=...
spring.mail.password=...

# reCAPTCHA
recaptcha.secret.key=...

# 2FA
# (configurado via totp-spring-boot-starter, ver su documentación)
```

---

## Cómo ejecutar en local

### Requisitos previos

- **Java 17** (exactamente esta versión, no superior)
- **Maven** (o usa el Maven Wrapper incluido: `./mvnw`)
- **PostgreSQL** corriendo localmente (o usa un contenedor Docker)
- Archivo `application.properties` configurado (ver sección anterior)

### Pasos

```bash
# 1. Entrar al directorio
cd nexus-backend

# 2. Compilar y ejecutar (usa el wrapper para consistencia)
./mvnw spring-boot:run

# En Windows:
mvnw.cmd spring-boot:run
```

La API arranca en `http://localhost:8080`.

### Swagger UI

Con el servidor arrancado, accede a la documentación interactiva de la API en:

```
http://localhost:8080/swagger-ui.html
```

### Seed de datos

Si necesitas poblar la base de datos con datos de prueba, ejecuta la clase `PopulateDB.java` (tiene el método `main`, se puede lanzar directamente desde el IDE).

---

## Docker

El `Dockerfile` usa un **multi-stage build** para producción:

```bash
# Construir imagen
docker build -t nexus-backend .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/nexus \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  nexus-backend
```

Parámetros JVM de producción (ya configurados en el Dockerfile):
- `-Xmx380m -Xms48m` — heap limitado para instancias pequeñas
- `-XX:+UseSerialGC` — GC más ligero para instancias con 1 CPU
- `-XX:MaxMetaspaceSize=120m` — límite de metaspace

---

## Convenciones de código

- Los comentarios en el código están en **español** (comentarios de negocio) o **inglés** (comentarios técnicos). Sigue la misma convención que el archivo que estés modificando.
- Los nombres de clases, métodos y variables están en **inglés** para las clases técnicas (config, security, utils) y en **español** para las entidades de negocio (Producto, Compra, Usuario…).
- Los **DTOs** de respuesta de admin llevan el prefijo `Admin` (ej: `AdminProducto`, `AdminUsuario`).
- Las **rutas REST** siguen el patrón `/api/{recurso-plural}` para endpoints nuevos. Los endpoints legacy sin prefijo `/api/` se mantienen por compatibilidad.
- Nunca inyectes un repositorio directamente en un controller — pasa siempre por el service.
- Usa `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` para proteger endpoints de admin a nivel de método cuando sea necesario añadir lógica adicional al filtro de `SecurityConfiguration`.
