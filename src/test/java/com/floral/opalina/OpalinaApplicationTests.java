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
import com.floral.opalina.service.PedidoService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Test
    void migracionCreaLasDieciseisTablasYElContextoArranca() {
        Integer tablas = jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() "
                        + "and table_type = 'BASE TABLE' "
                        + "and table_name <> 'flyway_schema_history'",
                Integer.class);
        assertThat(tablas).isEqualTo(16);
    }

    @Test
    void apiValidaYRespondeConCodigosCrudEsperados() {
        HttpResponse<String> lista = enviar("GET", "/api/productos", null);
        assertThat(lista.statusCode()).isEqualTo(200);

        String sku = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        String json = productoJson(sku, 1);
        HttpResponse<String> creado = enviar("POST", "/api/productos", json);
        assertThat(creado.statusCode()).isEqualTo(201);
        String id = extraerId(creado.body());

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

        HttpResponse<String> noEncontrado = enviar("GET", "/api/productos/no-existe", null);
        assertThat(noEncontrado.statusCode()).isEqualTo(404);

        HttpResponse<String> eliminado = enviar("DELETE", "/api/productos/" + id, null);
        assertThat(eliminado.statusCode()).isEqualTo(204);
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

        String productoId = crearProducto("SKU-" + prefijo, 3);
        Pedido pedido = pedidos.crearDesdeApi(formulario(cliente.getId(), productoId, 2));
        assertThat(pedido.getId()).matches("OP-\\d{4}-\\d+");
        assertThat(jdbc.queryForObject("select count(*) from pedido where codigo = ?", Integer.class, pedido.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(1);
        String productoGuardado = jdbc.queryForObject(
                "select i.producto_id from item_pedido i "
                        + "join pedido p on p.pedido_id = i.pedido_id "
                        + "where p.codigo = ?",
                String.class,
                pedido.getId());
        assertThat(productoGuardado).isEqualTo(productoId);

        Integer cantidadGuardada = jdbc.queryForObject(
                "select i.cantidad from item_pedido i "
                        + "join pedido p on p.pedido_id = i.pedido_id "
                        + "where p.codigo = ?",
                Integer.class,
                pedido.getId());
        assertThat(cantidadGuardada).isEqualTo(2);

        HttpResponse<String> productoConHistorial = enviar("DELETE", "/api/productos/" + productoId, null);
        assertThat(productoConHistorial.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());

        pedidos.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);
        assertThat(jdbc.queryForObject("select estado from pedido where codigo = ?", String.class, pedido.getId())).isEqualTo("CANCELADO");
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(3);
        assertThatThrownBy(() -> pedidos.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("select stock from producto where producto_id = ?", Integer.class, productoId)).isEqualTo(3);

        var segundoCliente = clientes.registrar(
                "Cliente Concurrente",
                "c" + prefijo + "@opalina.test",
                "+51 999 888 777",
                "ClavePrueba2026!");
        String productoConUnaUnidad = crearProducto("SKU-C" + prefijo, 1);
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

    private String crearProducto(String sku, int stock) {
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

    private PedidoApiForm formulario(String clienteId, String productoId, int cantidad) {
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

    private boolean intentarCompra(String clienteId, String productoId) {
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

    private String extraerId(String body) {
        Matcher matcher = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("La respuesta no incluyó el identificador del producto: " + body);
        }
        return matcher.group(1);
    }
}
