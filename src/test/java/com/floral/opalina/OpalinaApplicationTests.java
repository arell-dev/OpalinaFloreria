package com.floral.opalina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.floral.opalina.dto.ContactoForm;
import com.floral.opalina.dto.PedidoApiForm;
import com.floral.opalina.exception.ConflictoNegocioException;
import com.floral.opalina.domain.Pedido;
import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.domain.enums.MetodoPago;
import com.floral.opalina.domain.enums.ModalidadEntrega;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.ContactoService;
import com.floral.opalina.service.AlmacenamientoImagenProductoService;
import com.floral.opalina.service.PedidoService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "opalina.demo-data.enabled=false")
class OpalinaApplicationTests {
    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("opalina_test")
            .withUsername("opalina")
            .withPassword("opalina_test");

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ClienteService clientes;

    @Autowired
    private ContactoService contactos;

    @Autowired
    private PedidoService pedidos;

    @Autowired
    private AlmacenamientoImagenProductoService almacenamientoImagen;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void migracionCreaLasDieciseisTablasYElContextoArranca() {
        Integer tablas = jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() "
                        + "and table_type = 'BASE TABLE' "
                        + "and table_name <> 'flyway_schema_history'",
                Integer.class);
        assertThat(tablas).isEqualTo(16);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = database() "
                        + "and ((table_name = 'cliente' and column_name = 'cliente_id' and data_type = 'bigint' and extra like '%auto_increment%') "
                        + "or (table_name = 'producto' and column_name = 'producto_id' and data_type = 'bigint' and extra like '%auto_increment%'))",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void apiValidaYRespondeConCodigosCrudEsperados() {
        HttpResponse<String> lista = enviar("GET", "/api/productos", null);
        assertThat(lista.statusCode()).isEqualTo(200);

        String sku = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        String json = productoJson(sku, 1);
        HttpResponse<String> creado = enviar("POST", "/api/productos", json);
        assertThat(creado.statusCode()).isEqualTo(201);
        Long id = extraerId(creado.body());

        HttpResponse<String> detalle = enviar("GET", "/api/productos/" + id, null);
        assertThat(detalle.statusCode()).isEqualTo(200);

        String productoActualizado = productoJson(sku, 4)
                .replace("Ramo de integración", "Ramo actualizado");
        HttpResponse<String> actualizado = enviar("PUT", "/api/productos/" + id, productoActualizado);
        assertThat(actualizado.statusCode()).isEqualTo(200);
        assertThat(actualizado.body()).contains("Ramo actualizado");

        HttpResponse<String> invalido = enviar("POST", "/api/productos", "{}");
        assertThat(invalido.statusCode()).isEqualTo(400);

        HttpResponse<String> duplicado = enviar("POST", "/api/productos", json);
        assertThat(duplicado.statusCode()).isEqualTo(409);

        HttpResponse<String> noEncontrado = enviar("GET", "/api/productos/999999999", null);
        assertThat(noEncontrado.statusCode()).isEqualTo(404);

        HttpResponse<String> eliminado = enviar("DELETE", "/api/productos/" + id, null);
        assertThat(eliminado.statusCode()).isEqualTo(204);
    }

    @Test
    void apiPedidoUsaIdsNumericosYDevuelveCodigoLegible() {
        String prefijo = UUID.randomUUID().toString().substring(0, 8);
        var cliente = clientes.registrar("Cliente API", prefijo + "@opalina.test", "+51 999 888 777", "ClavePrueba2026!");
        Long productoId = crearProducto("API-" + prefijo, 2);

        HttpResponse<String> clienteApi = enviar("GET", "/api/clientes/" + cliente.getId(), null);
        assertThat(clienteApi.statusCode()).isEqualTo(200);
        assertThat(clienteApi.body()).contains("\"id\":" + cliente.getId());

        String solicitudPedido = """
                {
                  "clienteId": %d,
                  "modalidad": "RECOJO",
                  "direccion": "",
                  "referencia": "",
                  "fecha": "%s",
                  "hora": "12:00:00",
                  "metodoPago": "YAPE",
                  "items": [{"productoId": %d, "cantidad": 1}]
                }
                """.formatted(cliente.getId(), LocalDate.now().plusDays(2), productoId);
        HttpResponse<String> pedidoCreado = enviar("POST", "/api/pedidos", solicitudPedido);

        assertThat(pedidoCreado.statusCode()).isEqualTo(201);
        Long pedidoId = extraerId(pedidoCreado.body());
        assertThat(pedidoCreado.body()).containsPattern("\\\"codigo\\\":\\\"OP-\\d{4}-\\d+\\\"");

        HttpResponse<String> pedidoApi = enviar("GET", "/api/pedidos/" + pedidoId, null);
        assertThat(pedidoApi.statusCode()).isEqualTo(200);
        assertThat(pedidoApi.body()).contains("\"id\":" + pedidoId).contains("\"codigo\":\"");
    }

    @Test
    void imagenSubidaSePuedeSolicitarDesdeSuUrlPublica() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/7ugAAAAASUVORK5CYII=");
        String url = almacenamientoImagen.guardar(new MockMultipartFile("imagen", "producto.png", "image/png", png));

        try {
            HttpResponse<String> respuesta = enviar("GET", url, null);
            assertThat(respuesta.statusCode()).isEqualTo(200);
            assertThat(respuesta.headers().firstValue("content-type")).hasValueSatisfying(tipo -> assertThat(tipo).startsWith("image/png"));
        } finally {
            almacenamientoImagen.eliminarSiFueSubida(url);
        }
    }

    @Test
    void formularioAdminCreaProductoConUnaImagenSubida() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@opalina.test";
        String clave = "Clave-Prueba-123";
        String sku = "IMG-" + UUID.randomUUID().toString().substring(0, 8);
        String imagenUrl = null;
        jdbc.update("INSERT INTO cliente (nombre, email, creado_en) VALUES (?, ?, CURRENT_TIMESTAMP)", "Admin de prueba", email);
        Long adminId = jdbc.queryForObject("SELECT cliente_id FROM cliente WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO credencial (cliente_id, clave_hash, rol, creado_en) VALUES (?, ?, 'ADMIN', CURRENT_TIMESTAMP)", adminId, passwordEncoder.encode(clave));

        try {
            CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            HttpClient clienteHttp = HttpClient.newBuilder().cookieHandler(cookies).build();
            String formularioLogin = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&clave=" + URLEncoder.encode(clave, StandardCharsets.UTF_8);
            HttpRequest solicitudLogin = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formularioLogin))
                    .build();
            assertThat(clienteHttp.send(solicitudLogin, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);

            HttpRequest solicitudAdmin = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/admin/productos")).GET().build();
            HttpResponse<String> paginaAdmin = clienteHttp.send(solicitudAdmin, HttpResponse.BodyHandlers.ofString());
            assertThat(paginaAdmin.statusCode()).isEqualTo(200);
            assertThat(paginaAdmin.body()).contains("type=\"file\"").doesNotContain("Ruta web de la imagen");

            String boundaryGrande = "----OpalinaUpload" + UUID.randomUUID();
            byte[] imagenGrande = new byte[5 * 1024 * 1024 + 1];
            byte[] firmaPng = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/7ugAAAAASUVORK5CYII=");
            System.arraycopy(firmaPng, 0, imagenGrande, 0, firmaPng.length);
            HttpRequest solicitudGrande = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/admin/productos"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundaryGrande)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(formularioProductoMultipart(boundaryGrande, sku, imagenGrande)))
                    .build();
            HttpResponse<String> respuestaGrande = clienteHttp.send(solicitudGrande, HttpResponse.BodyHandlers.ofString());
            assertThat(respuestaGrande.statusCode()).isEqualTo(200);
            assertThat(respuestaGrande.body()).contains("La imagen no puede superar los 5 MB.");

            String boundary = "----OpalinaUpload" + UUID.randomUUID();
            HttpRequest solicitudProducto = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/admin/productos"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(formularioProductoMultipart(boundary, sku, firmaPng)))
                    .build();
            assertThat(clienteHttp.send(solicitudProducto, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);

            imagenUrl = jdbc.queryForObject("SELECT imagen_url FROM producto WHERE sku = ?", String.class, sku);
            assertThat(imagenUrl).startsWith(AlmacenamientoImagenProductoService.URL_BASE);
            Long productoId = jdbc.queryForObject("SELECT producto_id FROM producto WHERE sku = ?", Long.class, sku);
            HttpRequest solicitudDetalle = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/admin/productos/" + productoId)).GET().build();
            HttpResponse<String> paginaDetalle = clienteHttp.send(solicitudDetalle, HttpResponse.BodyHandlers.ofString());
            assertThat(paginaDetalle.statusCode()).isEqualTo(200);
            assertThat(paginaDetalle.body()).contains("src=\"" + imagenUrl + "\"").contains("Deja el campo vacío para conservar la imagen actual.");

            String boundaryEdicion = "----OpalinaUpdate" + UUID.randomUUID();
            HttpRequest solicitudEdicion = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/admin/productos/" + productoId))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundaryEdicion)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(formularioProductoMultipart(boundaryEdicion, sku, null)))
                    .build();
            assertThat(clienteHttp.send(solicitudEdicion, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
            assertThat(jdbc.queryForObject("SELECT imagen_url FROM producto WHERE sku = ?", String.class, sku)).isEqualTo(imagenUrl);

            HttpRequest solicitudImagen = HttpRequest.newBuilder(URI.create("http://localhost:" + port + imagenUrl)).GET().build();
            HttpResponse<String> respuestaImagen = clienteHttp.send(solicitudImagen, HttpResponse.BodyHandlers.ofString());
            assertThat(respuestaImagen.statusCode()).isEqualTo(200);
            assertThat(respuestaImagen.headers().firstValue("content-type")).hasValueSatisfying(tipo -> assertThat(tipo).startsWith("image/png"));
        } finally {
            almacenamientoImagen.eliminarSiFueSubida(imagenUrl);
            jdbc.update("DELETE FROM producto WHERE sku = ?", sku);
            jdbc.update("DELETE FROM credencial WHERE cliente_id = ?", adminId);
            jdbc.update("DELETE FROM cliente WHERE cliente_id = ?", adminId);
        }
    }

    @Test
    void clienteContactoYRestriccionesQuedanPersistidos() {
        String email = UUID.randomUUID() + "@opalina.test";
        var cliente = clientes.registrar("Cliente de Contacto", email, "+51 999 888 777", "ClavePrueba2026!");

        assertThatThrownBy(() -> clientes.registrar(
                "Cliente Duplicado",
                email,
                "+51 999 888 777",
                "OtraClave2026!"))
                .isInstanceOf(ConflictoNegocioException.class);

        ContactoForm formulario = new ContactoForm();
        formulario.setNombre("Cliente de Contacto");
        formulario.setEmail(email);
        formulario.setTelefono("+51 999 888 777");
        formulario.setAsunto("Consulta de prueba");
        formulario.setMensaje("Quisiera conocer la disponibilidad de los arreglos.");
        contactos.enviar(formulario, cliente.getId());

        assertThat(jdbc.queryForObject(
                "select count(*) from consulta_contacto where cliente_id = ? and email = ?",
                Integer.class,
                cliente.getId(),
                email)).isEqualTo(1);
    }

    @Test
    void checkoutPersistePedidoCancelaUnaVezYProtegeElStockConcurrente() throws Exception {
        String prefijo = UUID.randomUUID().toString().substring(0, 8);
        var cliente = clientes.registrar("Cliente de Prueba", prefijo + "@opalina.test", "+51 999 888 777", "ClavePrueba2026!");
        String claveHash = jdbc.queryForObject(
                "select clave_hash from credencial where cliente_id = ?",
                String.class,
                cliente.getId());
        assertThat(claveHash).startsWith("$2");

        Long productoId = crearProducto("SKU-" + prefijo, 3);
        Pedido pedido = pedidos.crearDesdeApi(formulario(cliente.getId(), productoId, 2));
        assertThat(pedido.getId()).isPositive();
        assertThat(pedido.getCodigo()).matches("OP-\\d{4}-\\d+");
        assertThat(jdbc.queryForObject("select count(*) from pedido where codigo = ?", Integer.class, pedido.getCodigo())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(1);
        Long productoGuardado = jdbc.queryForObject(
                "select i.producto_id from item_pedido i "
                        + "join pedido p on p.pedido_id = i.pedido_id "
                        + "where p.codigo = ?",
                Long.class,
                pedido.getCodigo());
        assertThat(productoGuardado).isEqualTo(productoId);

        Integer cantidadGuardada = jdbc.queryForObject(
                "select i.cantidad from item_pedido i "
                        + "join pedido p on p.pedido_id = i.pedido_id "
                        + "where p.codigo = ?",
                Integer.class,
                pedido.getCodigo());
        assertThat(cantidadGuardada).isEqualTo(2);

        HttpResponse<String> productoConHistorial = enviar("DELETE", "/api/productos/" + productoId, null);
        assertThat(productoConHistorial.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        pedidos.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);
        assertThat(jdbc.queryForObject("select estado from pedido where codigo = ?", String.class, pedido.getCodigo())).isEqualTo("CANCELADO");
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(3);
        assertThatThrownBy(() -> pedidos.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(3);

        var segundoCliente = clientes.registrar(
                "Cliente Concurrente",
                "c" + prefijo + "@opalina.test",
                "+51 999 888 777",
                "ClavePrueba2026!");
        Long productoConUnaUnidad = crearProducto("SKU-C" + prefijo, 1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> compras = List.of(
                    () -> intentarCompra(segundoCliente.getId(), productoConUnaUnidad),
                    () -> intentarCompra(cliente.getId(), productoConUnaUnidad));
            long comprasExitosas = executor.invokeAll(compras).stream().filter(resultado -> {
                try {
                    return resultado.get();
                } catch (Exception error) {
                    return false;
                }
            }).count();
            assertThat(comprasExitosas).isEqualTo(1);
            Integer stockRestante = jdbc.queryForObject(
                    "select stock from producto where producto_id = ?",
                    Integer.class,
                    productoConUnaUnidad);
            assertThat(stockRestante).isZero();
            Integer pedidosGuardados = jdbc.queryForObject(
                    "select count(*) from pedido p "
                            + "join item_pedido i on i.pedido_id = p.pedido_id "
                            + "where i.producto_id = ?",
                    Integer.class,
                    productoConUnaUnidad);
            assertThat(pedidosGuardados).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Long crearProducto(String sku, int stock) {
        try {
            HttpResponse<String> creado = enviar("POST", "/api/productos", productoJson(sku, stock));
            assertThat(creado.statusCode()).isEqualTo(201);
            return extraerId(creado.body());
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private String productoJson(String sku, int stock) {
        return """
                {
                  "nombre": "Ramo de integración",
                  "descripcion": "Producto para prueba JPA",
                  "categoria": "Pruebas",
                  "precio": 25.00,
                  "stock": %d,
                  "disponible": true,
                  "destacado": false,
                  "sku": "%s",
                  "imagen": "/test.jpg"
                }
                """.formatted(stock, sku);
    }

    private PedidoApiForm formulario(Long clienteId, Long productoId, int cantidad) {
        PedidoApiForm form = new PedidoApiForm();
        form.setClienteId(clienteId);
        form.setModalidad(ModalidadEntrega.RECOJO);
        form.setFecha(LocalDate.now().plusDays(1));
        form.setHora(LocalTime.of(12, 0));
        form.setMetodoPago(MetodoPago.YAPE);
        PedidoApiForm.Linea linea = new PedidoApiForm.Linea();
        linea.setProductoId(productoId);
        linea.setCantidad(cantidad);
        form.setItems(List.of(linea));
        return form;
    }

    private boolean intentarCompra(Long clienteId, Long productoId) {
        try {
            pedidos.crearDesdeApi(formulario(clienteId, productoId, 1));
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String body) {
        try {
            HttpRequest.Builder solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta));
            if (body == null) {
                solicitud.method(metodo, HttpRequest.BodyPublishers.noBody());
            } else {
                solicitud.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(body));
            }
            return HttpClient.newHttpClient().send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo invocar la API de prueba", error);
        }
    }

    private void agregarCampoMultipart(ByteArrayOutputStream cuerpo, String boundary, String nombre, String valor) throws IOException {
        cuerpo.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private byte[] formularioProductoMultipart(String boundary, String sku, byte[] imagen) throws IOException {
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        agregarCampoMultipart(cuerpo, boundary, "nombre", "Ramo subido en prueba");
        agregarCampoMultipart(cuerpo, boundary, "sku", sku);
        agregarCampoMultipart(cuerpo, boundary, "categoria", "Pruebas");
        agregarCampoMultipart(cuerpo, boundary, "precio", "20.00");
        agregarCampoMultipart(cuerpo, boundary, "stock", "3");
        agregarCampoMultipart(cuerpo, boundary, "descripcion", "Imagen multipart de integración");
        agregarCampoMultipart(cuerpo, boundary, "disponible", "true");
        if (imagen != null) {
            cuerpo.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"imagen\"; filename=\"ramo.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            cuerpo.write(imagen);
        }
        cuerpo.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return cuerpo.toByteArray();
    }

    private Long extraerId(String body) {
        Matcher matcher = Pattern.compile("\\\"id\\\":(\\d+)").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("La respuesta no incluyó el identificador del producto: " + body);
        }
        return Long.valueOf(matcher.group(1));
    }
}
