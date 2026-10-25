# Guía de ejecución y pruebas

Esta guía reúne el arranque local y las comprobaciones principales de Opalina. Los pasos manuales de Postman solo se consideran realizados después de enviar las solicitudes.

## Requisitos

- Java 21.
- Docker Desktop activo.
- Postman, si se probará la API de forma manual.

## 1. Iniciar la aplicación

Desde la raíz del proyecto, crea `.env` si aún no existe y levanta MySQL:

~~~powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d mysql
docker compose ps
~~~

Espera a que MySQL figure como `healthy`. En una segunda terminal, ejecuta:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

Flyway aplica las migraciones V1 y V2. Hibernate valida el mapeo JPA al iniciar la aplicación. Abre [http://localhost:8080](http://localhost:8080).

El [README](../README.md#ejecutar-en-local) explica cómo iniciar MySQL con Docker y configurar la conexión.

## 2. Pruebas automatizadas

Ejecuta desde la raíz:

~~~powershell
.\mvnw.cmd test
~~~

La suite usa Testcontainers para iniciar un MySQL 8.4 temporal. Docker debe estar activo; si no está disponible, la clase de integración queda deshabilitada y las pruebas de persistencia no se ejecutan.

La ejecución registrada el 24/10/2026 terminó con 4 pruebas aprobadas. La suite cubre:

- Aplicación de migraciones y arranque del contexto.
- CRUD de productos y respuestas HTTP de validación.
- Persistencia de clientes y consultas de contacto, correo duplicado y hash de contraseña.
- Creación y cancelación de pedidos, descuento de stock y compras concurrentes.

## 3. Pruebas REST con Postman

Crea una variable de colección llamada `baseUrl` con el valor `http://localhost:8080`. Las rutas REST de esta versión no requieren sesión.

### Productos

| Método | Solicitud | Comprobación |
| --- | --- | --- |
| `GET` | `{{baseUrl}}/api/productos` | Responde `200 OK`; admite `q`, `categoria` y `disponible`. |
| `POST` | `{{baseUrl}}/api/productos` | Responde `201 Created`; guarda el `id` para las siguientes solicitudes. |
| `GET` | `{{baseUrl}}/api/productos/{{productoId}}` | Responde `200 OK`; un ID inexistente responde `404`. |
| `PUT` | `{{baseUrl}}/api/productos/{{productoId}}` | Responde `200 OK`; envía el formulario completo. |
| `DELETE` | `{{baseUrl}}/api/productos/{{productoId}}` | Responde `204 No Content` si no tiene pedidos; con historial responde `409 Conflict`. |

Para crear un producto, usa `Content-Type: application/json` y un cuerpo como este:

~~~json
{
  "nombre": "Ramo de prueba",
  "descripcion": "Producto creado desde Postman.",
  "categoria": "Pruebas",
  "precio": 45.50,
  "stock": 5,
  "disponible": true,
  "destacado": false,
  "sku": "POSTMAN-PRUEBA-001",
  "imagen": "/assets/imagenes/productos/12-RamoOpalinaMorado.png"
}
~~~

Usa un SKU nuevo en cada ejecución. Guarda el `id` de la respuesta como `productoId`. El endpoint de clientes solo ofrece consultas; usa el id de un cliente existente como `clienteId`:

- `GET {{baseUrl}}/api/clientes`
- `GET {{baseUrl}}/api/clientes/{{clienteId}}`

### Pedidos

Usa una fecha futura y actualiza la fecha del ejemplo cuando deje de serlo.

Envía un pedido con `POST {{baseUrl}}/api/pedidos`. Usa un cliente registrado, un producto con stock y una fecha futura:

~~~json
{
  "clienteId": "{{clienteId}}",
  "modalidad": "RECOJO",
  "direccion": "",
  "referencia": "",
  "fecha": "2027-01-15",
  "hora": "12:00:00",
  "metodoPago": "YAPE",
  "items": [
    {
      "productoId": "{{productoId}}",
      "cantidad": 1
    }
  ]
}
~~~

La respuesta esperada es `201 Created`. Guarda el id devuelto como `pedidoId`. Después puedes consultar el pedido con `GET {{baseUrl}}/api/pedidos/{{pedidoId}}` y cambiar su estado con `PATCH {{baseUrl}}/api/pedidos/{{pedidoId}}/estado`:

~~~json
{
  "estado": "EN_PROCESO"
}
~~~

Una transición no permitida responde `400 Bad Request`. La compra guarda el pedido, sus líneas, el destinatario y el historial, y descuenta stock dentro de una transacción.

### Contacto

Envía el formulario desde [http://localhost:8080/contacto](http://localhost:8080/contacto). La respuesta de la página confirma el envío. Para comprobar el registro en MySQL:

~~~sql
SELECT consulta_contacto_id, cliente_id, nombre, email, asunto, estado, creado_en
FROM consulta_contacto
ORDER BY consulta_contacto_id DESC
LIMIT 5;
~~~

## 4. Validaciones y respuestas

| Situación | Respuesta esperada |
| --- | --- |
| Solicitud válida que crea un recurso | `201 Created` |
| Lectura o actualización correcta | `200 OK` |
| Eliminación sin historial | `204 No Content` |
| Campo obligatorio o regla de negocio incorrecta | `400 Bad Request` |
| Recurso inexistente | `404 Not Found` |
| SKU o correo duplicado, o producto usado en un pedido | `409 Conflict` |

La API de clientes es de consulta; el registro y la edición del perfil se realizan en la aplicación web.

## 5. Comprobar que los datos persisten

1. Registra o actualiza un producto y envía una consulta de contacto.
2. Detén la aplicación con `Ctrl+C`.
3. Ejecuta `docker compose down` y vuelve a iniciar MySQL con `docker compose up -d mysql`.
4. Inicia Opalina y comprueba los mismos registros.

`docker compose down` conserva el volumen de datos. No uses `docker compose down -v` para esta comprobación, porque elimina la base local.

No marques como realizadas las solicitudes manuales que aún no hayas ejecutado.
