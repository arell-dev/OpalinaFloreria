package com.floral.opalina.repository.memory;

import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryPedidoRepository implements PedidoRepository {
    private static final long PRIMER_NUMERO_ORDEN = 100;

    private final List<Pedido> pedidos = new ArrayList<>();
    private final AtomicLong ultimoNumeroAsignado = new AtomicLong(PRIMER_NUMERO_ORDEN - 1);

    @Override
    public synchronized List<Pedido> buscarTodos() {
        return List.copyOf(pedidos);
    }

    @Override
    public synchronized List<Pedido> buscarTodosOrdenados() {
        return ordenar(pedidos.stream());
    }

    @Override
    public synchronized List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return ordenar(pedidos.stream() .filter(pedido -> estado == null || pedido.getEstado() == estado));
    }

    @Override
    public synchronized List<Pedido> buscarParaAdministracion(EstadoPedido estado, String busqueda) {
        String texto = normalizar(busqueda);

        return ordenar(pedidos.stream()
                .filter(pedido -> estado == null || pedido.getEstado() == estado)

                .filter(pedido -> texto.isBlank() || contiene(pedido.getId(), texto) || contiene(pedido.getNombreCliente(), texto) || contiene(pedido.getEmail(), texto)));
    }

    @Override
    public synchronized List<Pedido> buscarPorCliente(String clienteId, EstadoPedido estado) {
        return ordenar(pedidos.stream().filter(pedido -> pedido.getClienteId().equals(clienteId)).filter(pedido -> estado == null || pedido.getEstado() == estado));
    }

    @Override
    public synchronized long contar(EstadoPedido estado) {
        return pedidos.stream().filter(pedido -> estado == null || pedido.getEstado() == estado).count();
    }

    @Override
    public synchronized BigDecimal totalVentas(EstadoPedido estado) {
        return pedidos.stream()
                    .filter(pedido -> estado == null || pedido.getEstado() == estado)
                    .filter(pedido -> pedido.getEstado() != EstadoPedido.CANCELADO)
                    .map(Pedido::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public synchronized Optional<Pedido> buscarPorId(String id) {
        return pedidos.stream().filter(pedido -> pedido.getId().equals(id)).findFirst();
    }

    @Override
    public synchronized String generarId() {
        String numero = String.format(Locale.ROOT, "%04d", ultimoNumeroAsignado.incrementAndGet());
        return "OP-" + Year.now() + "-" + numero;
    }

    @Override
    public synchronized void guardar(Pedido pedido) {
        int indiceExistente = buscarIndice(pedido.getId());
        
        if (indiceExistente >= 0) {
            pedidos.set(indiceExistente, pedido);
        } else {
            pedidos.add(pedido);
            long numeroPorCantidad = PRIMER_NUMERO_ORDEN + pedidos.size() - 1;
            ultimoNumeroAsignado.accumulateAndGet(numeroPorCantidad, Math::max);
        }

        actualizarSecuencia(pedido.getId());
    }

    private List<Pedido> ordenar(Stream<Pedido> pedidosFiltrados) {
        return pedidosFiltrados.sorted(Comparator.comparing(Pedido::getCreadoEn).reversed()).toList();
    }

    private int buscarIndice(String id) {
        for (int indice = 0; indice < pedidos.size(); indice++) {
            if (pedidos.get(indice).getId().equals(id)) {
                return indice;
            }
        }
        return -1;
    }

    private void actualizarSecuencia(String id) {
        String prefijo = "OP-" + Year.now() + "-";
        if (!id.startsWith(prefijo)) {
            return;
        }

        try {
            long numeroGuardado = Long.parseLong(id.substring(prefijo.length()));
            ultimoNumeroAsignado.accumulateAndGet(numeroGuardado, Math::max);
        } catch (NumberFormatException ignored) {
            // Un pedido con otro formato no altera la secuencia numérica.
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contiene(String valor, String texto) {
        return valor.toLowerCase(Locale.ROOT).contains(texto);
    }
}
