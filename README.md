# Opalina Floral Studio

Aplicación web para la gestión de una florería: catálogo de arreglos, cuentas de clientes, pedidos y herramientas de administración.

## Funcionalidades

- Catálogo con búsqueda y filtros por categoría, precio y disponibilidad.
- Registro, inicio de sesión, perfil e historial de pedidos.
- Carrito y checkout con selección de entrega y simulación del medio de pago.
- Administración de productos, pedidos y configuración de la tienda.
- Formulario de contacto con consultas guardadas en la base de datos.
- API REST para productos, clientes y pedidos.
- Reportes administrativos con exportación a PDF.

## Tecnologías

Java 21 · Spring Boot 4.1.2 · Spring MVC · Thymeleaf · Spring Data JPA · Hibernate · MySQL 8.4 · Flyway · Bootstrap · PDFBox.

## Requisitos

- Java 21.
- Docker Desktop con Docker Compose.
- Maven Wrapper incluido en el proyecto.

## Ejecutar en local

Si es la primera vez que ejecutas el proyecto o todavía no configuraste Docker, empieza por la [guía de base de datos local con Docker Desktop](docs/Guia-base-de-datos.md). Explica la instalación, la activación de WSL y la configuración necesaria. Estos son los comandos rápidos para iniciar Opalina una vez preparado el equipo.

Desde la carpeta raíz del proyecto, crea el archivo de configuración local una sola vez:

~~~powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
~~~

Levanta MySQL y espera a que el servicio aparezca como `healthy`:

~~~powershell
docker compose up -d mysql
docker compose ps
~~~

Inicia Opalina en otra terminal:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

Abre [http://localhost:8080](http://localhost:8080). Flyway aplica las migraciones al arrancar y Hibernate valida que las entidades coincidan con el esquema.

El archivo `.env.example` contiene valores para desarrollo local. No reutilices esas contraseñas en un entorno compartido o publicado.

Para detener la aplicación, usa `Ctrl+C`. Para detener MySQL y conservar los datos:

~~~powershell
docker compose down
~~~

El comando `docker compose down -v` también borra el volumen local de MySQL.

### Variables locales

| Variable | Uso |
| --- | --- |
| `OPALINA_DB_URL` | URL JDBC de MySQL; por defecto usa el puerto `3307`. |
| `OPALINA_DB_USERNAME` | Usuario de la aplicación y de Compose. |
| `OPALINA_DB_PASSWORD` | Contraseña de ese usuario. |
| `OPALINA_DB_ROOT_PASSWORD` | Contraseña de `root` en el contenedor. |
| `OPALINA_DB_PORT` | Puerto local publicado por Docker. |
| `OPALINA_DEMO_DATA_ENABLED` | Activa la carga de datos de demostración. |

## Cómo está organizado

El flujo de persistencia es:

~~~text
Controlador MVC o REST
        ↓
Servicio
        ↓
Contrato en repository/
        ↓
Adaptador en persistence/jpa/adapter/
        ↓
Repositorio Spring Data en persistence/jpa/springdata/
        ↓
MySQL
~~~

| Paquete | Responsabilidad |
| --- | --- |
| `controller/` | Recibe solicitudes web y REST. |
| `dto/` | Modela y valida los datos de formularios y cuerpos JSON. |
| `domain/` | Contiene los objetos del negocio, independientes de JPA. |
| `service/` | Aplica reglas de negocio y coordina transacciones. |
| `repository/` | Define los contratos que usan los servicios. |
| `persistence/jpa/entity/` | Mapea las tablas con entidades JPA. |
| `persistence/jpa/springdata/` | Declara interfaces que extienden `JpaRepository`. |
| `persistence/jpa/adapter/` | Implementa los contratos y convierte entre dominio y entidades JPA. |

Los controladores MVC y REST comparten servicios; así las reglas principales se mantienen en una sola capa.

## API REST

| Método | Ruta | Resultado |
| --- | --- | --- |
| `GET` | `/api/productos?q=&categoria=&disponible=` | Lista productos; acepta filtros opcionales. |
| `GET` | `/api/productos/{id}` | Consulta un producto. |
| `POST` | `/api/productos` | Crea un producto; responde `201 Created`. |
| `PUT` | `/api/productos/{id}` | Actualiza un producto. |
| `DELETE` | `/api/productos/{id}` | Elimina un producto; responde `409 Conflict` si tiene pedidos asociados. |
| `GET` | `/api/clientes` | Lista clientes. |
| `GET` | `/api/clientes/{id}` | Consulta un cliente y su resumen de compras. |
| `GET` | `/api/pedidos?estado=PENDIENTE` | Lista pedidos; el filtro por estado es opcional. |
| `GET` | `/api/pedidos/{id}` | Consulta un pedido. |
| `POST` | `/api/pedidos` | Registra un pedido; responde `201 Created`. |
| `PATCH` | `/api/pedidos/{id}/estado` | Cambia el estado de un pedido. |

La API REST aún no exige inicio de sesión en el perfil local. El perfil `railway` la bloquea. El pago del checkout es una simulación: no se conecta con Yape, Plin ni una pasarela bancaria.

## Pruebas y documentación

Ejecuta las pruebas automatizadas con Docker disponible:

~~~powershell
.\mvnw.cmd test
~~~

- [Guía de base de datos y configuración local](docs/Guia-base-de-datos.md)
- [Guía de ejecución, pruebas y API](docs/Guia-de-pruebas.md)
- [Migraciones Flyway](src/main/resources/db/migration/)

## Publicación en Railway

Las publicaciones se construyen desde etiquetas SemVer (`vX.Y.Z`) mediante GitHub Actions. La etiqueta debe apuntar a un commit integrado en `main`, y las pruebas y la construcción de la imagen deben terminar correctamente antes de desplegar. Cada fusión a `main` ejecuta las verificaciones, pero no publica en Railway.

Para habilitar la publicación, configura un entorno de GitHub Actions llamado `railway` que acepte etiquetas `v*`. Añade el secreto `RAILWAY_TOKEN` y las variables `RAILWAY_PROJECT_ID`, `RAILWAY_SERVICE_NAME` y `RAILWAY_ENVIRONMENT_NAME`. Configura el check obligatorio `Integridad` en la regla de protección de `main` y protege las etiquetas de versión frente a actualizaciones y eliminaciones. El workflow registra una GitHub Release cuando Railway informa que el despliegue de esa versión terminó correctamente.

El servicio de aplicación usa el `Dockerfile` y el perfil `railway`. Configura `OPALINA_DB_URL` como `jdbc:mysql://${{MySQL.RAILWAY_PRIVATE_DOMAIN}}:3306/opalina`, junto con `OPALINA_DB_USERNAME`, `OPALINA_DB_PASSWORD`, `DEMO_ACCESS_USERNAME`, `DEMO_ACCESS_PASSWORD` y `OPALINA_DEMO_ADMIN_PASSWORD` como variables privadas del servicio. Configura `MYSQL_DATABASE`, `MYSQL_USER` y `MYSQL_PASSWORD` en el servicio MySQL y monta su volumen persistente en `/var/lib/mysql`. La aplicación expone `/actuator/health/readiness` para comprobar el inicio. El perfil de demostración solicita autenticación HTTP Basic, bloquea `/api/**` y las operaciones de escritura de productos.

La primera publicación aceptada es `v0.0.1`. Una versión nueva debe incrementar exactamente una parte de la última versión desplegada correctamente. Si Railway falla, corrige el despliegue y reintenta la etiqueta fallida. Las GitHub Releases de este workflow registran las versiones cuyo despliegue ya terminó correctamente.
